package com.lenswise.app.features.menu.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuScannerPayloadParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseCompleteMenuSnapshot() {
        val raw = """
        {
            "status": "success",
            "data": {
                "storeName": "一蘭拉麵",
                "items": [
                    {
                        "originalName": "天然とんこつラーメン",
                        "translatedName": "天然豚骨拉麵",
                        "description": "特製秘傳辣醬與濃郁豚骨湯頭",
                        "price": 980.0
                    },
                    {
                        "originalName": "替玉",
                        "translatedName": "加麵",
                        "price": 210.0
                    }
                ]
            }
        }
        """.trimIndent()

        val parsed = MenuScannerPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        assertEquals("一蘭拉麵", parsed.storeName)
        assertEquals(2, parsed.items.size)
        assertEquals("天然豚骨拉麵", parsed.items[0].translatedName)
        assertEquals("特製秘傳辣醬與濃郁豚骨湯頭", parsed.items[0].description)
        assertEquals(980.0, parsed.items[0].price, 0.001)
        assertEquals(210.0, parsed.items[1].price, 0.001)
    }

    @Test
    fun testCompletenessGuard_filtersIncompleteTrailingItemDuringStreaming() {
        // While streaming, trailing item might not yet have generated price
        val raw = """
        {
            "data": {
                "storeName": "居酒屋",
                "items": [
                    {
                        "originalName": "焼き鳥",
                        "translatedName": "烤雞肉串",
                        "price": 300.0
                    },
                    {
                        "originalName": "枝豆",
                        "translatedName": "毛豆"
                    }
                ]
            }
        }
        """.trimIndent()

        val parsed = MenuScannerPayloadParser.parseStreamingSnapshot(json.parseToJsonElement(raw).jsonObject)
        // 枝豆尚未生成 price 欄位，必須被暫時扣留，不渲染殘缺卡片！
        assertEquals(1, parsed.items.size)
        assertEquals("烤雞肉串", parsed.items[0].translatedName)
    }
}

