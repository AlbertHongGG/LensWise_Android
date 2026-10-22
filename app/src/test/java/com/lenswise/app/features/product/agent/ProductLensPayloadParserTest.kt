package com.lenswise.app.features.product.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLensPayloadParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseNestedDataSnapshot() {
        val raw = """
        {
            "status": "success",
            "data": {
                "productName": "日清兵衛豆皮烏龍麵",
                "originalName": "日清のどん兵衛 きつねうどん",
                "brand": "日清",
                "origin": "日本",
                "summary": "經典日式柴魚高湯配上大片飽滿多汁炸豆皮",
                "ingredients": ["小麥粉", "植物油脂", "大豆蛋白"],
                "allergens": ["小麥", "大豆"],
                "dietaryFlags": ["非素食"],
                "recommendedFor": ["喜愛濃郁高湯者"],
                "notRecommendedFor": ["對大豆過敏者"]
            }
        }
        """.trimIndent()

        val parsed = ProductLensPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertEquals("日清兵衛豆皮烏龍麵", parsed.productName)
        assertEquals("日清のどん兵衛 きつねうどん", parsed.originalName)
        assertEquals("日清", parsed.brand)
        assertEquals("日本", parsed.origin)
        assertEquals(3, parsed.ingredients.size)
        assertEquals(2, parsed.allergenAlerts.size)
        assertEquals(1, parsed.dietaryFlags.size)
        assertEquals(1, parsed.whenRecommended.size)
        assertEquals(1, parsed.whenNotRecommended.size)
    }

    @Test
    fun testParseFlatRootSnapshot() {
        val raw = """
        {
            "productName": "可口可樂",
            "originalName": "Coca Cola",
            "brand": "Coca Cola",
            "summary": "碳酸飲料",
            "ingredients": ["碳酸水", "焦糖色素"]
        }
        """.trimIndent()

        val parsed = ProductLensPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertEquals("可口可樂", parsed.productName)
        assertEquals("Coca Cola", parsed.brand)
        assertEquals(2, parsed.ingredients.size)
        assertTrue(parsed.allergenAlerts.isEmpty())
    }

    @Test
    fun testParseEmptyOrMalformedSnapshot_returnsEmptyRecordWithoutCrashing() {
        val raw = "{}"
        val parsed = ProductLensPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertTrue(parsed.productName.isEmpty())
        assertTrue(parsed.ingredients.isEmpty())
    }
}

