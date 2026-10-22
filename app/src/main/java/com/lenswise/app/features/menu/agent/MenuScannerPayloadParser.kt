package com.lenswise.app.features.menu.agent

import com.lenswise.app.domain.model.menu.MenuScannerItem
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 具備原子完整性守門 (Atomic Completeness Guard) 的菜單品項 DTO。
 */
class MenuScannerItemDto(
    val originalName: String,
    val translatedName: String,
    val description: String?,
    val price: Double,
    val hasPriceField: Boolean = true
) {
    /**
     * 判定品項是否已完全生成完畢（最小原子單元）。
     * 只有原文非空、譯名非空、且作為結尾欄位的 price 已經生成的品項，才允許進入畫面渲染，
     * 確保每個項目都是 100% 完整產出，避免出現殘缺空白或尚未翻譯完成的中繼卡片。
     */
    val isComplete: Boolean
        get() = originalName.isNotBlank() && translatedName.isNotBlank() && hasPriceField

    fun toDomain() = MenuScannerItem(
        originalName = originalName,
        translatedName = translatedName,
        description = description,
        price = price
    )

    companion object {
        fun fromJson(raw: JsonObject): MenuScannerItemDto {
            val originalName = raw["originalName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val translatedName = raw["translatedName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val rawDesc = raw["description"]?.jsonPrimitive?.contentOrNull?.trim()
            val description = if (!rawDesc.isNullOrEmpty() && rawDesc != "null") rawDesc else null

            val hasPrice = raw.containsKey("price")
            val price = parsePrice(raw["price"])

            return MenuScannerItemDto(
                originalName = originalName,
                translatedName = translatedName,
                description = description,
                price = price,
                hasPriceField = hasPrice
            )
        }

        private fun parsePrice(element: JsonElement?): Double {
            if (element == null) return 0.0
            return try {
                val primitive = element.jsonPrimitive
                val doubleVal = primitive.doubleOrNull
                if (doubleVal != null) return doubleVal

                val str = primitive.contentOrNull?.trim().orEmpty()
                if (str.isEmpty()) return 0.0
                val cleaned = str.replace(Regex("[^0-9.]"), "")
                cleaned.toDoubleOrNull() ?: 0.0
            } catch (_: Exception) {
                0.0
            }
        }
    }
}

/**
 * 健壯且具型別安全的菜單掃描串流酬載解析器 (MenuScannerPayloadParser)。
 * 1:1 對齊 LensWise 實作：
 * 支援雙重 Schema 架構，
 * 串流期間嚴格過濾末尾未閉合的不完整項目。
 */
object MenuScannerPayloadParser {

    private fun resolveDataNode(json: JsonObject): JsonObject {
        val data = json["data"]
        return if (data is JsonObject) data else json
    }

    fun parseStreamingSnapshot(json: JsonObject): MenuScannerRecord {
        return try {
            val dataNode = resolveDataNode(json)

            val storeName = dataNode["storeName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val rawItems = dataNode["items"]
            val completeItems = mutableListOf<MenuScannerItem>()

            if (rawItems is JsonArray) {
                for (item in rawItems) {
                    if (item is JsonObject) {
                        val dto = MenuScannerItemDto.fromJson(item)
                        if (dto.isComplete) {
                            completeItems.add(dto.toDomain())
                        }
                    }
                }
            }

            MenuScannerRecord(
                storeName = storeName,
                items = completeItems
            )
        } catch (_: Exception) {
            MenuScannerRecord.empty()
        }
    }

    fun parseFinalResponse(json: JsonObject): MenuScannerRecord {
        val status = json["status"]?.jsonPrimitive?.contentOrNull?.trim()
        val message = json["message"]?.jsonPrimitive?.contentOrNull?.trim()

        if (status != null && status != "success") {
            throw Exception(message ?: "菜單辨識解析失敗，請重新拍攝清晰的照片。")
        }

        val dataNode = resolveDataNode(json)
        val storeName = dataNode["storeName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val finalStoreName = storeName.ifBlank { "未知餐廳菜單" }

        val rawItems = dataNode["items"]
        val items = mutableListOf<MenuScannerItem>()

        if (rawItems is JsonArray) {
            for (raw in rawItems) {
                if (raw is JsonObject) {
                    val dto = MenuScannerItemDto.fromJson(raw)
                    if (dto.originalName.isNotBlank() || dto.translatedName.isNotBlank()) {
                        items.add(dto.toDomain())
                    }
                }
            }
        }

        return MenuScannerRecord(
            storeName = finalStoreName,
            items = items
        )
    }
}

