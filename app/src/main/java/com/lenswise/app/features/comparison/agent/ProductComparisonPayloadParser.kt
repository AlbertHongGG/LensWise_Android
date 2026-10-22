package com.lenswise.app.features.comparison.agent

import com.lenswise.app.domain.model.comparison.ComparisonPoint
import com.lenswise.app.domain.model.comparison.ComparisonProduct
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 單一比對候選商品 DTO，具備原子完整性守護。
 */
class ComparisonProductDto(
    val originalName: String,
    val translatedName: String
) {
    val isComplete: Boolean
        get() = originalName.isNotBlank() && translatedName.isNotBlank()

    fun toDomain() = ComparisonProduct(
        originalName = originalName,
        translatedName = translatedName
    )

    companion object {
        fun fromJson(raw: JsonObject): ComparisonProductDto {
            val original = raw["originalName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val translated = raw["translatedName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            return ComparisonProductDto(original, translated)
        }
    }
}

/**
 * 單一比對維度 DTO，具備原子完整性守護。
 * 只有維度名稱非空、數值陣列非空且內部所有條目皆已完整閉合非空時，才視為完整，
 * 防止左右多欄對比矩陣出現不對稱破版。
 */
class ComparisonPointDto(
    val aspect: String,
    val values: List<String>
) {
    val isComplete: Boolean
        get() = aspect.isNotBlank() && values.isNotEmpty() && values.all { it.isNotBlank() }

    fun toDomain() = ComparisonPoint(
        aspect = aspect,
        values = values
    )

    companion object {
        fun fromJson(raw: JsonObject): ComparisonPointDto {
            val aspect = raw["aspect"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val rawValues = raw["values"]
            val values = mutableListOf<String>()
            if (rawValues is JsonArray) {
                for (v in rawValues) {
                    val str = v.jsonPrimitive.contentOrNull?.trim().orEmpty()
                    values.add(str)
                }
            }
            return ComparisonPointDto(aspect, values)
        }
    }
}

/**
 * 健壯且具型別安全的多商品比對串流酬載解析器 (ProductComparisonPayloadParser)。
 * 1:1 對齊 LensWise 實作：
 * 支援雙重 Schema 架構，
 * 嚴格保證多欄位對比矩陣在串流時絕不因中繼資料殘缺而破版。
 */
object ProductComparisonPayloadParser {

    private fun resolveDataNode(json: JsonObject): JsonObject {
        val data = json["data"]
        return if (data is JsonObject) data else json
    }

    fun parseStreamingSnapshot(json: JsonObject): ProductComparisonRecord {
        return try {
            val dataNode = resolveDataNode(json)

            val title = dataNode["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val summary = dataNode["summary"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            // Products
            val rawProducts = dataNode["products"]
            val completeProducts = mutableListOf<ComparisonProduct>()
            if (rawProducts is JsonArray) {
                for (p in rawProducts) {
                    if (p is JsonObject) {
                        val dto = ComparisonProductDto.fromJson(p)
                        if (dto.isComplete) {
                            completeProducts.add(dto.toDomain())
                        }
                    }
                }
            }

            // Comparison Points
            val rawPoints = dataNode["comparisonPoints"]
            val completePoints = mutableListOf<ComparisonPoint>()
            if (rawPoints is JsonArray) {
                for (pt in rawPoints) {
                    if (pt is JsonObject) {
                        val dto = ComparisonPointDto.fromJson(pt)
                        if (dto.isComplete) {
                            completePoints.add(dto.toDomain())
                        }
                    }
                }
            }

            ProductComparisonRecord(
                title = title.ifEmpty { "商品比對結果" },
                summary = summary,
                products = completeProducts,
                comparisonPoints = completePoints
            )
        } catch (_: Exception) {
            ProductComparisonRecord.empty()
        }
    }
}

