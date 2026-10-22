package com.lenswise.app.core.ai

import com.lenswise.app.core.ai.streaming.JsonStreamPipeline
import com.lenswise.app.features.ai.agents.receipt.ReceiptFeeItemDto
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
        assertFalse("未包含 price 結尾欄位的品項不應視為完整", partialDto.isComplete)

        // 完整品項 (嚴格依據契約 originalName, translatedName, price)
        val completeObj = json.parseToJsonElement(
            """{"originalName": "咖啡", "translatedName": "Coffee", "price": 55.0}"""
        ).jsonObject
        val completeDto = ReceiptItemDto.fromJson(completeObj)
        assertTrue("包含 originalName, translatedName 與 price 的品項應為完整", completeDto.isComplete)
        assertEquals("咖啡", completeDto.originalName)
        assertEquals("Coffee", completeDto.translatedName)
        assertEquals(55.0, completeDto.price, 0.001)
    }

    @Test
    fun testReceiptFeeItemDto_contractCompletenessGuard() {
        // 缺少 amount
        val partialFee = json.parseToJsonElement(
            """{"name": "消費稅 10%"}"""
        ).jsonObject
        val partialFeeDto = ReceiptFeeItemDto.fromJson(partialFee)
        assertFalse("缺少 amount 的費用項不應為完整", partialFeeDto.isComplete)

        // 完整費用
        val completeFee = json.parseToJsonElement(
            """{"name": "消費稅 10%", "amount": 15.0}"""
        ).jsonObject
        val completeFeeDto = ReceiptFeeItemDto.fromJson(completeFee)
        assertTrue("完整費用項應為 isComplete", completeFeeDto.isComplete)
        assertEquals("消費稅 10%", completeFeeDto.name)
        assertEquals(15.0, completeFeeDto.amount, 0.001)
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
        assertEquals("Milk", record.items[0].translatedName)
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
        assertEquals("蘋果", finalSnapshot.data.items[0].translatedName)
        assertEquals(120.0, finalSnapshot.data.items[0].price, 0.001)
    }

    @Test
    fun testJsonStreamSanitizer_stripsConversationalPreambleAndCitations() {
        val dirtyOutput = """
            這兩張圖片 並非同一張收據的連續拍攝（圖一為 7-Eleven[cite: 1]，圖二為大黑藥妝[cite: 2]）。
            以下為解析後的標準 JSON 資料：

            ```json
            {
                "status": "success",
                "data": {
                    "storeName": "7-Eleven (nanaco支付)",
                    "receiptDateTime": "2024-05-12 14:30",
                    "currency": "JPY",
                    "items": [
                        {
                            "originalName": "手巻おにぎり辛子明太子",
                            "translatedName": "手卷飯糰 辛子明太子",
                            "price": 130.0
                        }
                    ],
                    "discounts": [
                        {
                            "name": "會員點數折抵",
                            "amount": -10.0
                        }
                    ],
                    "taxes": [
                        {
                            "name": "消費稅 8%",
                            "amount": 9.6
                        }
                    ],
                    "subtotal": 130.0,
                    "totalAmount": 129.6
                }
            }
            ```
        """.trimIndent()

        val candidate = JsonStreamPipeline.extractJsonSubstring(dirtyOutput)
        assertTrue("必須以 '{' 起始", candidate.startsWith("{"))
        assertFalse("不應包含代碼塊結尾標記", candidate.contains("```"))

        val repaired = JsonStreamPipeline.repairPartialJson(candidate)
        val parsed = json.parseToJsonElement(repaired).jsonObject
        val record = ReceiptScannerPayloadParser.parseStreamingSnapshot(parsed)

        assertEquals("7-Eleven (nanaco支付)", record.storeName)
        assertEquals("2024-05-12 14:30", record.receiptDate)
        assertEquals("JPY", record.currency)
        assertEquals(1, record.items.size)
        assertEquals("手巻おにぎり辛子明太子", record.items[0].originalName)
        assertEquals("手卷飯糰 辛子明太子", record.items[0].translatedName)
        assertEquals(130.0, record.items[0].price, 0.001)
        assertEquals(1, record.discounts.size)
        assertEquals("會員點數折抵", record.discounts[0].name)
        assertEquals(10.0, record.discounts[0].amount, 0.001)
        assertEquals(1, record.taxes.size)
        assertEquals("消費稅 8%", record.taxes[0].name)
        assertEquals(9.6, record.taxes[0].amount, 0.001)
        assertEquals(129.6, record.totalAmount, 0.001)
    }

    @Test
    fun testJsonStreamRepairer_handlesTrailingCommaAndColon() {
        val hangingColon = """{"storeName": "Lawson", "subtotal": """
        val repairedColon = JsonStreamPipeline.repairPartialJson(hangingColon)
        val parsedColon = json.parseToJsonElement(repairedColon).jsonObject
        assertEquals("Lawson", parsedColon["storeName"]?.toString()?.replace("\"", ""))

        val trailingComma = """{"storeName": "Lawson", "items": [{"originalName": "Bread", "translatedName": "麵包", "price": 40.0}, """
        val repairedComma = JsonStreamPipeline.repairPartialJson(trailingComma)
        val parsedComma = json.parseToJsonElement(repairedComma).jsonObject
        val record = ReceiptScannerPayloadParser.parseStreamingSnapshot(parsedComma)
        assertEquals("Lawson", record.storeName)
        assertEquals(1, record.items.size)
        assertEquals("Bread", record.items[0].originalName)
        assertEquals(40.0, record.items[0].price, 0.001)
    }
}
