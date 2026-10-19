package com.lenswise.app.domain.model.receipt

import android.os.Parcelable
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
    override val imagePaths: List<String> = emptyList()
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
