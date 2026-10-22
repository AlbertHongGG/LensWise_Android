package com.lenswise.app.features.product.agent

import com.lenswise.app.domain.model.product.ProductLensRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 健壯且具型別安全的商品分析串流酬載解析器 (ProductLensPayloadParser)。
 * 1:1 對齊 LensWise 實作：
 * 支援雙重 Schema 架構 (包裹於 data 或位於根節點)，
 * 安全正規化字串陣列 (成分、過敏原、飲食禁忌、推薦/不推薦族群)，
 * 並確保串流中繼快照永遠不會拋出異常。
 */
object ProductLensPayloadParser {

    private fun resolveDataNode(json: JsonObject): JsonObject {
        val data = json["data"]
        return if (data is JsonObject) data else json
    }

    private fun parseStringList(element: JsonElement?): List<String> {
        if (element == null) return emptyList()
        val list = when (element) {
            is JsonArray -> element
            is JsonObject -> return emptyList()
            else -> return listOf(element.jsonPrimitive.contentOrNull?.trim().orEmpty()).filter { it.isNotEmpty() }
        }
        val result = mutableListOf<String>()
        for (item in list) {
            val str = item.jsonPrimitive.contentOrNull?.trim().orEmpty()
            if (str.isNotEmpty()) {
                result.add(str)
            }
        }
        return result
    }

    fun parseStreamingSnapshot(json: JsonObject): ProductLensRecord {
        return try {
            val dataNode = resolveDataNode(json)

            val productName = dataNode["productName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val originalName = dataNode["originalName"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val brand = dataNode["brand"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val origin = dataNode["origin"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val summary = dataNode["summary"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val ingredients = parseStringList(dataNode["ingredients"])
            val allergenAlerts = parseStringList(dataNode["allergens"] ?: dataNode["allergenAlerts"])
            val dietaryFlags = parseStringList(dataNode["dietaryFlags"])
            val whenRecommended = parseStringList(dataNode["recommendedFor"] ?: dataNode["whenRecommended"])
            val whenNotRecommended = parseStringList(dataNode["notRecommendedFor"] ?: dataNode["whenNotRecommended"])

            ProductLensRecord(
                productName = productName,
                originalName = originalName,
                brand = brand,
                origin = origin,
                summary = summary,
                ingredients = ingredients,
                allergenAlerts = allergenAlerts,
                dietaryFlags = dietaryFlags,
                whenRecommended = whenRecommended,
                whenNotRecommended = whenNotRecommended
            )
        } catch (_: Exception) {
            ProductLensRecord.empty()
        }
    }
}

