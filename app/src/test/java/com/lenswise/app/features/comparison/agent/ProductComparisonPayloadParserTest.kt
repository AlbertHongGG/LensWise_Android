package com.lenswise.app.features.comparison.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductComparisonPayloadParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseCompleteComparisonSnapshot() {
        val raw = """
        {
            "status": "success",
            "data": {
                "title": "日清兵衛 vs 一風堂拉麵",
                "products": [
                    { "originalName": "日清どん兵衛", "translatedName": "日清兵衛豆皮麵" },
                    { "originalName": "一風堂白丸", "translatedName": "一風堂白丸拉麵" }
                ],
                "comparisonPoints": [
                    {
                        "aspect": "麵體工法",
                        "values": ["油炸烏龍麵", "非油炸細直麵"]
                    },
                    {
                        "aspect": "湯頭風味",
                        "values": ["柴魚昆布高湯", "濃郁豚骨湯"]
                    }
                ],
                "summary": "## 核心結論\n兩款截然不同的麵體體驗"
            }
        }
        """.trimIndent()

        val parsed = ProductComparisonPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertEquals("日清兵衛 vs 一風堂拉麵", parsed.title)
        assertEquals(2, parsed.products.size)
        assertEquals(2, parsed.comparisonPoints.size)
        assertEquals("麵體工法", parsed.comparisonPoints[0].aspect)
        assertEquals("油炸烏龍麵", parsed.comparisonPoints[0].values[0])
        assertEquals("非油炸細直麵", parsed.comparisonPoints[0].values[1])
        assertTrue(parsed.summary.contains("核心結論"))
    }

    @Test
    fun testCompletenessGuard_filtersIncompleteTrailingPointDuringStreaming() {
        val raw = """
        {
            "status": "success",
            "data": {
                "title": "規格對決",
                "products": [
                    { "originalName": "P1", "translatedName": "商品1" },
                    { "originalName": "P2", "translatedName": "商品2" },
                    { "originalName": "P3", "translatedName": "" }
                ],
                "comparisonPoints": [
                    {
                        "aspect": "產地",
                        "values": ["日本", "台灣"]
                    },
                    {
                        "aspect": "熱量",
                        "values": ["150kcal", ""]
                    },
                    {
                        "aspect": "",
                        "values": ["10g", "20g"]
                    },
                    {
                        "aspect": "空維度",
                        "values": []
                    }
                ]
            }
        }
        """.trimIndent()

        val parsed = ProductComparisonPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        // P3 譯名為空，必須被過濾
        assertEquals(2, parsed.products.size)
        // 熱量(未填完第二項)、空 aspect、空 values 均判定為不完整，必須被安全過濾以防止矩陣破版！
        assertEquals(1, parsed.comparisonPoints.size)
        assertEquals("產地", parsed.comparisonPoints[0].aspect)
        assertEquals(listOf("日本", "台灣"), parsed.comparisonPoints[0].values)
    }

    @Test
    fun testParseFlatRootJsonWithoutDataWrapper() {
        val raw = """
        {
            "title": "平面結構比對",
            "summary": "總結",
            "products": [
                { "originalName": "P1", "translatedName": "商品1" }
            ],
            "comparisonPoints": [
                { "aspect": "特色", "values": ["高CP值"] }
            ]
        }
        """.trimIndent()

        val parsed = ProductComparisonPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertEquals("平面結構比對", parsed.title)
        assertEquals("總結", parsed.summary)
        assertEquals(1, parsed.products.size)
        assertEquals(1, parsed.comparisonPoints.size)
    }
}

