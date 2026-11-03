package com.lenswise.app.domain.model.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptArithmeticTest {

    @Test
    fun `recalculateTotals correctly deducts discounts and adds taxes`() {
        val record = ReceiptScannerRecord(
            storeName = "Test Store",
            subtotal = 0.0,
            items = listOf(
                ReceiptItem(originalName = "Item 1", translatedName = "Item 1", quantity = 1, price = 600.0),
                ReceiptItem(originalName = "Item 2", translatedName = "Item 2", quantity = 1, price = 510.0)
            ),
            discounts = listOf(
                ReceiptFeeItem(name = "Cashback", amount = 22.0)
            ),
            taxes = listOf(
                ReceiptFeeItem(name = "Tax 8%", amount = 21.0),
                ReceiptFeeItem(name = "Tax 10%", amount = 30.0)
            )
        )

        val recalculated = record.recalculateTotals()

        // Subtotal = 600 + 510 = 1110.0
        assertEquals(1110.0, recalculated.subtotal, 0.001)
        // Total = 1110 + 21 + 30 - 22 = 1139.0
        assertEquals(1139.0, recalculated.totalAmount, 0.001)
        assertEquals(2, recalculated.totalItemCount)
    }

    @Test
    fun `ReceiptItem quantity and unitPrice calculation with totalItemCount`() {
        val cake = ReceiptItem(
            originalName = "フレッシュフルーツケーキ",
            translatedName = "鮮奶油水果蛋糕",
            quantity = 2,
            price = 2300.0
        )
        val milk = ReceiptItem(
            originalName = "アイス マロン&ミルク",
            translatedName = "冰栗子牛奶",
            quantity = 1,
            price = 900.0
        )
        val tea = ReceiptItem(
            originalName = "アイス ロイヤルミルクティ",
            translatedName = "冰皇家奶茶",
            quantity = 1,
            price = 980.0
        )

        assertEquals(2, cake.safeQuantity)
        assertEquals(1150.0, cake.unitPrice, 0.001)
        assertEquals(900.0, milk.unitPrice, 0.001)

        val record = ReceiptScannerRecord(
            storeName = "HARBS 大名古屋大樓店",
            items = listOf(cake, milk, tea),
            currency = "JPY"
        ).recalculateTotals()

        // 總件數 = 2 + 1 + 1 = 4
        assertEquals(4, record.totalItemCount)
        // 小計與總額 = 2300 + 900 + 980 = 4180.0
        assertEquals(4180.0, record.subtotal, 0.001)
        assertEquals(4180.0, record.totalAmount, 0.001)
    }

    @Test
    fun `recalculateTotals handles negative discount amounts robustly`() {
        val record = ReceiptScannerRecord(
            storeName = "Test Store",
            subtotal = 1000.0,
            items = emptyList(),
            discounts = listOf(
                ReceiptFeeItem(name = "Coupon", amount = -50.0)
            ),
            taxes = listOf(
                ReceiptFeeItem(name = "Tax", amount = 100.0)
            )
        )

        val recalculated = record.recalculateTotals()

        // Subtotal retains 1000.0 when items are empty
        assertEquals(1000.0, recalculated.subtotal, 0.001)
        // Total = 1000 + 100 - abs(-50) = 1050.0
        assertEquals(1050.0, recalculated.totalAmount, 0.001)
    }

    @Test
    fun `recalculateTotals clamps total to zero if discounts exceed subtotal and tax`() {
        val record = ReceiptScannerRecord(
            storeName = "Promo Store",
            subtotal = 50.0,
            items = emptyList(),
            discounts = listOf(
                ReceiptFeeItem(name = "Big Voucher", amount = 200.0)
            ),
            taxes = emptyList()
        )

        val recalculated = record.recalculateTotals()
        assertEquals(0.0, recalculated.totalAmount, 0.001)
    }
}
