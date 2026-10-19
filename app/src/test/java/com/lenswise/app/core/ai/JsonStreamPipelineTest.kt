package com.lenswise.app.core.ai

import com.lenswise.app.core.ai.streaming.JsonStreamPipeline
import com.lenswise.app.features.ai.agents.receipt.ReceiptItemDto
import com.lenswise.app.features.ai.agents.receipt.ReceiptScannerPayloadParser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonStreamPipelineTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testExtractJsonSubstring_stripsLeadingMarkdown() {
        val raw = "```json\n{\"storeName\": \"7-Eleven\"}```"
        val extracted = JsonStreamPipeline.extractJsonSubstring(raw)
        assertTrue(extracted.startsWith("{\"storeName\": \"7-Eleven\"}"))
    }

    @Test
    fun testRepairPartialJson_closesUnclosedStringAndBraces() {
        val truncated = "{\"storeName\": \"Familymart\", \"items\": [{\"originalName\": \"Milk"
        val repaired = JsonStreamPipeline.repairPartialJson(truncated)

        // 應成功自動補齊引號與中括號、大括號
        val parsed = json.parseToJsonElement(repaired)
        assertTrue(parsed is JsonObject)
        val obj = parsed.jsonObject
        assertEquals("Familymart", obj["storeName"]?.toString()?.replace("\"", ""))
    }

    @Test
    fun testReceiptItemDto_atomicCompletenessGuard() {
        // 尚未產生 price 結尾欄位的半吊子品項
        val partialObj = json.parseToJsonElement(
            """{"originalName": "咖啡", "translatedName": "Coffee"}"""
        ).jsonObject
        val partialDto = ReceiptItemDto.fromJson(partialObj)
        assertFalse("未包含 price 的品項不應視為完整", partialDto.isComplete)

        // 完整品項
        val completeObj = json.parseToJsonElement(
            """{"originalName": "咖啡", "translatedName": "Coffee", "price": 55.0}"""
        ).jsonObject
        val completeDto = ReceiptItemDto.fromJson(completeObj)
        assertTrue("包含品名、譯名與金額的品項應為完整", completeDto.isComplete)
    }

    @Test
    fun testReceiptScannerPayloadParser_filtersIncompleteItemsDuringStream() {
        val streamJson = json.parseToJsonElement(
            """
            {
                "status": "success",
                "data": {
                    "storeName": "全聯福利中心",
                    "currency": "TWD",
                    "items": [
                        {"originalName": "鮮奶", "translatedName": "Milk", "price": 89.0},
                        {"originalName": "吐司", "translatedName": "Toast"}
                    ],
                    "subtotal": 89.0,
                    "totalAmount": 89.0
                }
            }
            """.trimIndent()
        ).jsonObject

        val record = ReceiptScannerPayloadParser.parseStreamingSnapshot(streamJson)
        assertEquals("全聯福利中心", record.storeName)
        assertEquals("TWD", record.currency)
        // 吐司尚未具備 price，守門機制應過濾它，只放行鮮奶
        assertEquals(1, record.items.size)
        assertEquals("鮮奶", record.items[0].originalName)
        assertEquals(89.0, record.items[0].price, 0.01)
    }

    @Test
    fun testJsonStreamPipeline_bind_emitsSnapshotStream() = runBlocking {
        val chunks = flowOf(
            "{\"storeName\": \"",
            "Costco\", \"items\": [",
            "{\"originalName\": \"Apple\", \"translatedName\": \"蘋果\", \"price\": 120.0}]}"
        )

        val snapshots = JsonStreamPipeline.bind(
            rawStream = chunks,
            mapper = { ReceiptScannerPayloadParser.parseStreamingSnapshot(it) },
            initialData = com.lenswise.app.domain.model.receipt.ReceiptScannerRecord.empty(),
            throttleMs = 0L // 測試環境取消節流
        ).toList()

        assertTrue(snapshots.isNotEmpty())
        val finalSnapshot = snapshots.last()
        assertTrue(finalSnapshot.isComplete)
        assertEquals("Costco", finalSnapshot.data.storeName)
        assertEquals(1, finalSnapshot.data.items.size)
        assertEquals("Apple", finalSnapshot.data.items[0].originalName)
    }
}
