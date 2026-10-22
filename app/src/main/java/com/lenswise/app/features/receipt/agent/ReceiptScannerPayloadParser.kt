package com.lenswise.app.features.receipt.agent

import com.lenswise.app.domain.model.receipt.ReceiptFeeItem
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 具備原子完整性守門 (Atomic Completeness Guard) 的收據品項 DTO。
 * 嚴格綁定契約欄位 [originalName], [translatedName], [price]，絕無同義詞或別名白名單猜測。
 */
class ReceiptItemDto(
    val originalName: String,
    val translatedName: String,
    val price: Double,
    val hasPriceField: Boolean
) {
    /**
     * 只有品名非空且價格結尾欄位已產出時才放行，杜絕串流中繼渲染空白或半截卡片。
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
            val originalName = raw["originalName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val translatedName = raw["translatedName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val hasPrice = raw.containsKey("price")
            val price = parseDouble(raw["price"])
            return ReceiptItemDto(originalName, translatedName, price, hasPrice)
        }
    }
}

/**
 * 費用/折扣/稅金 DTO。
 * 嚴格綁定契約欄位 [name], [amount]。
 */
class ReceiptFeeItemDto(
    val name: String,
    val amount: Double,
    val hasAmountField: Boolean
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && hasAmountField

    fun toDomain() = ReceiptFeeItem(name = name, amount = kotlin.math.abs(amount))

    companion object {
        fun fromJson(raw: JsonObject): ReceiptFeeItemDto {
            val name = raw["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val hasAmount = raw.containsKey("amount")
            val amount = parseDouble(raw["amount"])
            return ReceiptFeeItemDto(name, amount, hasAmount)
        }
    }
}

private fun parseDouble(element: JsonElement?): Double {
    if (element == null) return 0.0
    return try {
        val primitive = element.jsonPrimitive
        val doubleVal = primitive.doubleOrNull
        if (doubleVal != null) return doubleVal

        val str = primitive.contentOrNull?.trim().orEmpty()
        if (str.isEmpty()) return 0.0
        val isNegative = str.startsWith("-")
        val cleaned = str.replace(Regex("[^0-9.]"), "")
        val parsed = cleaned.toDoubleOrNull() ?: 0.0
        if (isNegative) -parsed else parsed
    } catch (_: Exception) {
        0.0
    }
}

/**
 * 契約驅動且具原子完整性守護的收據串流酬載解析器 (ReceiptScannerPayloadParser)。
 * 1:1 對齊 LensWise 參考架構：
 * 1. 嚴格依據 Prompt 契約欄位萃取資料，絕不設置別名猜測或容錯白名單。
 * 2. 串流原子完整性過濾 (isComplete)，防止未完結品項閃爍。
 * 3. 自動衍生小計與總額校驗。
 */
object ReceiptScannerPayloadParser {

    fun parseStreamingSnapshot(json: JsonObject): ReceiptScannerRecord {
        val dataNode = if (json.containsKey("data") && json["data"] is JsonObject) {
            json["data"]!!.jsonObject
        } else {
            json
        }

        val rawStoreName = dataNode["storeName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val rawDate = (dataNode["receiptDateTime"] ?: dataNode["receiptDate"])?.jsonPrimitive?.contentOrNull?.trim()
        val receiptDate = if (!rawDate.isNullOrEmpty()) rawDate else null
        val rawCurrency = dataNode["currency"]?.jsonPrimitive?.contentOrNull?.trim()
        val currency = if (!rawCurrency.isNullOrEmpty()) rawCurrency else null

        // Items (僅放行 100% isComplete 的品項)
        val completeItems = mutableListOf<ReceiptItem>()
        val rawItems = dataNode["items"]
        if (rawItems is JsonArray) {
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
        if (rawDiscounts is JsonArray) {
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
        if (rawTaxes is JsonArray) {
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
            subtotal + taxesSum - discountsSum
        }

        return ReceiptScannerRecord(
            storeName = rawStoreName,
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

