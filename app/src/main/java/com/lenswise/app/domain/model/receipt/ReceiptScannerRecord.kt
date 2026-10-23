package com.lenswise.app.domain.model.receipt

import android.os.Parcelable
import com.lenswise.app.core.ai.model.ChatMessage
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 收據萃取與分析領域核心實體 (ReceiptScannerRecord)。
 * 實作 [IHistoryRecord] 支援統一歷史展示；實作 [Parcelable] 支援 Compose 導航傳遞。
 */
@Serializable
@Parcelize
data class ReceiptScannerRecord(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    val storeName: String = "",
    val receiptDate: String? = null,
    val currency: String? = null,
    val subtotal: Double = 0.0,
    val items: List<ReceiptItem> = emptyList(),
    val discounts: List<ReceiptFeeItem> = emptyList(),
    val taxes: List<ReceiptFeeItem> = emptyList(),
    val totalAmount: Double = 0.0,
    override val imagePaths: List<String> = emptyList(),
    val chatHistory: List<ChatMessage> = emptyList()
) : IHistoryRecord, Parcelable {

    override val displayTitle: String
        get() = storeName.ifBlank { "未知商家" }

    override val displaySubtitle: String
        get() = if (totalAmount > 0.0) {
            val curr = currency ?: "$"
            "$curr ${"%.2f".format(totalAmount)}"
        } else {
            "無金額資訊"
        }

    /**
     * 依據商品清單、稅金與折扣嚴格計算小計與總額 (Single Source of Truth)：
     * 小計 = 若品項清單非空則以品項累加，否則保留既有小計。
     * 稅金 = 累加所有稅額絕對值 (+)。
     * 折扣 = 累加所有折扣額絕對值並扣除 (-)。
     * 總額 = 小計 + 稅金 - 折扣 (保底 >= 0.0)。
     */
    fun recalculateTotals(): ReceiptScannerRecord {
        val itemsSubtotal = if (items.isNotEmpty()) items.sumOf { it.price } else subtotal
        val taxesSum = taxes.sumOf { kotlin.math.abs(it.amount) }
        val discountsSum = discounts.sumOf { kotlin.math.abs(it.amount) }
        val calculatedTotal = (itemsSubtotal + taxesSum - discountsSum).coerceAtLeast(0.0)
        return copy(
            subtotal = itemsSubtotal,
            totalAmount = calculatedTotal
        )
    }

    companion object {
        fun empty(): ReceiptScannerRecord = ReceiptScannerRecord()
    }
}

@Serializable
@Parcelize
data class ReceiptItem(
    val originalName: String,
    val translatedName: String,
    val price: Double
) : Parcelable

@Serializable
@Parcelize
data class ReceiptFeeItem(
    val name: String,
    val amount: Double
) : Parcelable
