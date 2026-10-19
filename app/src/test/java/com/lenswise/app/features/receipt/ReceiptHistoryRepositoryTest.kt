package com.lenswise.app.features.receipt

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
}
