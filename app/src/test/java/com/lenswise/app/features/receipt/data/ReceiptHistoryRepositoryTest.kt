package com.lenswise.app.features.receipt.data

import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptHistoryRepositoryTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testReceiptScannerRecordSerializationRoundtrip() {
        val record = ReceiptScannerRecord(
            id = "test-receipt-123",
            timestamp = 1760876100000L,
            storeName = "7-Eleven 台北信義門市",
            totalAmount = 145.0,
            currency = "NT$",
            items = listOf(
                ReceiptItem(
                    originalName = "純喫茶紅茶",
                    translatedName = "Pure Tea Black Tea",
                    price = 25.0
                ),
                ReceiptItem(
                    originalName = "御飯糰",
                    translatedName = "Onigiri",
                    price = 35.0
                )
            )
        )

        val serialized = json.encodeToString(record)
        val deserialized = json.decodeFromString<ReceiptScannerRecord>(serialized)

        assertEquals("test-receipt-123", deserialized.id)
        assertEquals("7-Eleven 台北信義門市", deserialized.storeName)
        assertEquals(145.0, deserialized.totalAmount, 0.001)
        assertEquals(2, deserialized.items.size)
        assertEquals("純喫茶紅茶", deserialized.items[0].originalName)
        assertEquals(25.0, deserialized.items[0].price, 0.001)
        assertEquals("7-Eleven 台北信義門市", deserialized.displayTitle)
        assertTrue(deserialized.displaySubtitle.contains("NT$ 145.00"))
    }

    @Test
    fun testReceiptScannerRecordWithDiscountsAndTaxesRoundtrip() {
        val record = ReceiptScannerRecord(
            id = "daikoku-drug-1",
            storeName = "大國藥妝 札幌南2條店",
            receiptDate = "2014-08-20 17:30",
            currency = "JPY",
            subtotal = 11609.0,
            items = listOf(
                ReceiptItem(originalName = "トマトノアマナットウ160g", translatedName = "蕃茄甘納豆 160g", price = 276.0),
                ReceiptItem(originalName = "ピップ エレキバン130 12P", translatedName = "易利氣 磁力貼130 12粒入", price = 665.0),
                ReceiptItem(originalName = "アリナミンEXプラス 270T", translatedName = "合利他命 EX PLUS 強效錠 270錠", price = 8852.0),
                ReceiptItem(originalName = "パブロンゴールドAビリュウ 44H", translatedName = "大正百保能 感冒微粒A 44包", price = 1816.0)
            ),
            discounts = listOf(
                com.lenswise.app.domain.model.receipt.ReceiptFeeItem("蕃茄甘納豆 5%折扣", 13.0),
                com.lenswise.app.domain.model.receipt.ReceiptFeeItem("易利氣磁力貼 5%折扣", 33.0),
                com.lenswise.app.domain.model.receipt.ReceiptFeeItem("合利他命 5%折扣", 442.0),
                com.lenswise.app.domain.model.receipt.ReceiptFeeItem("大正百保能 5%折扣", 90.0)
            ),
            taxes = listOf(
                com.lenswise.app.domain.model.receipt.ReceiptFeeItem("消費稅 8%", 881.0)
            ),
            totalAmount = 11912.0
        )

        val serialized = json.encodeToString(record)
        val deserialized = json.decodeFromString<ReceiptScannerRecord>(serialized)

        assertEquals("大國藥妝 札幌南2條店", deserialized.storeName)
        assertEquals("2014-08-20 17:30", deserialized.receiptDate)
        assertEquals(4, deserialized.items.size)
        assertEquals(4, deserialized.discounts.size)
        assertEquals("蕃茄甘納豆 5%折扣", deserialized.discounts[0].name)
        assertEquals(13.0, deserialized.discounts[0].amount, 0.001)
        assertEquals(1, deserialized.taxes.size)
        assertEquals("消費稅 8%", deserialized.taxes[0].name)
        assertEquals(881.0, deserialized.taxes[0].amount, 0.001)
        assertEquals(11912.0, deserialized.totalAmount, 0.001)
        assertEquals("JPY 11912.00", deserialized.displaySubtitle)
    }
}
