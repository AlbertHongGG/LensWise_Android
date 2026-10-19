package com.lenswise.app.features.ai.agents.receipt

import com.lenswise.app.domain.model.receipt.ReceiptFeeItem
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 具備原子完整性守門 (Atomic Completeness Guard) 的收據品項 DTO。
 */
class ReceiptItemDto(
    val originalName: String,
    val translatedName: String,
    val price: Double,
    val hasPriceField: Boolean
) {
    /**
     * 只有原文非空、譯名非空、且作為結尾欄位的 price 已經生成的品項，才允許進入畫面渲染，
     * 確保每個項目都是 100% 完整產出，避免出現殘缺空白或尚未翻譯完成的中繼卡片。
     */
    val isComplete: Boolean
        get() = originalName.isNotBlank() && translatedName.isNotBlank() && hasPriceField

    fun toDomain() = ReceiptItem(
        originalName = originalName,
        translatedName = translatedName,
        price = price
    )

    companion object {
        fun fromJson(raw: JsonObject): ReceiptItemDto {
            val originalName = raw["originalName"]?.jsonPrimitive?.content.orEmpty().trim()
            val translatedName = raw["translatedName"]?.jsonPrimitive?.content.orEmpty().trim()
            val hasPrice = raw.containsKey("price")
            val price = parseDouble(raw["price"])
            return ReceiptItemDto(originalName, translatedName, price, hasPrice)
        }
    }
}

/**
 * 費用/折扣/稅金 DTO。
 */
class ReceiptFeeItemDto(
    val name: String,
    val amount: Double,
    val hasAmountField: Boolean
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && hasAmountField

    fun toDomain() = ReceiptFeeItem(name = name, amount = amount)

    companion object {
        fun fromJson(raw: JsonObject): ReceiptFeeItemDto {
            val name = raw["name"]?.jsonPrimitive?.content.orEmpty().trim()
            val hasAmount = raw.containsKey("amount")
            val amount = parseDouble(raw["amount"])
            return ReceiptFeeItemDto(name, amount, hasAmount)
        }
    }
}

private fun parseDouble(element: JsonElement?): Double {
    if (element == null) return 0.0
    val primitive = element.jsonPrimitive
    val doubleVal = primitive.doubleOrNull
    if (doubleVal != null) return doubleVal

    val str = primitive.content.trim()
    val isNegative = str.startsWith("-")
    val cleaned = str.replace(Regex("[^0-9.]"), "")
    val parsed = cleaned.toDoubleOrNull() ?: 0.0
    return if (isNegative) -parsed else parsed
}

/**
 * 強韌型別安全收據串流酬載解析器 (ReceiptScannerPayloadParser)。
 * 支援根節點扁平格式與 data 巢狀雙重 Schema，杜絕串流解析型別中斷。
 */
object ReceiptScannerPayloadParser {

    fun parseStreamingSnapshot(json: JsonObject): ReceiptScannerRecord {
        val dataNode = if (json.containsKey("data") && json["data"] is JsonObject) {
            json["data"]!!.jsonObject
        } else {
            json
        }

        val storeName = dataNode["storeName"]?.jsonPrimitive?.content.orEmpty().trim()
        val rawDate = (dataNode["receiptDateTime"] ?: dataNode["receiptDate"])?.jsonPrimitive?.content?.trim()
        val receiptDate = if (!rawDate.isNullOrEmpty()) rawDate else null
        val rawCurrency = dataNode["currency"]?.jsonPrimitive?.content?.trim()
        val currency = if (!rawCurrency.isNullOrEmpty()) rawCurrency else null

        // Items (僅放行 100% isComplete 的品項)
        val completeItems = mutableListOf<ReceiptItem>()
        val rawItems = dataNode["items"]
        if (rawItems != null && rawItems is JsonArray) {
            for (element in rawItems.jsonArray) {
                if (element is JsonObject) {
                    val dto = ReceiptItemDto.fromJson(element)
                    if (dto.isComplete) {
                        completeItems.add(dto.toDomain())
                    }
                }
            }
        }

        // Discounts
        val completeDiscounts = mutableListOf<ReceiptFeeItem>()
        val rawDiscounts = dataNode["discounts"]
        if (rawDiscounts != null && rawDiscounts is JsonArray) {
            for (element in rawDiscounts.jsonArray) {
                if (element is JsonObject) {
                    val dto = ReceiptFeeItemDto.fromJson(element)
                    if (dto.isComplete) {
                        completeDiscounts.add(dto.toDomain())
                    }
                }
            }
        }

        // Taxes
        val completeTaxes = mutableListOf<ReceiptFeeItem>()
        val rawTaxes = dataNode["taxes"]
        if (rawTaxes != null && rawTaxes is JsonArray) {
            for (element in rawTaxes.jsonArray) {
                if (element is JsonObject) {
                    val dto = ReceiptFeeItemDto.fromJson(element)
                    if (dto.isComplete) {
                        completeTaxes.add(dto.toDomain())
                    }
                }
            }
        }

        val itemsSum = completeItems.sumOf { it.price }
        val discountsSum = completeDiscounts.sumOf { it.amount }
        val taxesSum = completeTaxes.sumOf { it.amount }

        val subtotal = if (dataNode.containsKey("subtotal")) {
            parseDouble(dataNode["subtotal"])
        } else {
            itemsSum
        }

        val totalAmount = if (dataNode.containsKey("totalAmount")) {
            parseDouble(dataNode["totalAmount"])
        } else {
            subtotal + taxesSum + discountsSum
        }

        return ReceiptScannerRecord(
            storeName = storeName,
            receiptDate = receiptDate,
            currency = currency,
            subtotal = subtotal,
            items = completeItems,
            discounts = completeDiscounts,
            taxes = completeTaxes,
            totalAmount = totalAmount,
            imagePaths = emptyList()
        )
    }
}
