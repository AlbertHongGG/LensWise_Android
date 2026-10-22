package com.lenswise.app.domain.model.comparison

import android.os.Parcelable
import com.lenswise.app.core.ai.model.ChatMessage
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 商品比對領域核心實體 (ProductComparisonRecord)。
 * 實作 [IHistoryRecord] 支援統一歷史展示；實作 [Parcelable] 支援 Compose 導航傳遞。
 */
@Serializable
@Parcelize
data class ProductComparisonRecord(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    val title: String = "",
    val summary: String = "",
    val products: List<ComparisonProduct> = emptyList(),
    val comparisonPoints: List<ComparisonPoint> = emptyList(),
    override val imagePaths: List<String> = emptyList(),
    val chatHistory: List<ChatMessage> = emptyList()
) : IHistoryRecord, Parcelable {

    override val displayTitle: String
        get() = title.ifBlank { "商品比對紀錄" }

    override val displaySubtitle: String
        get() = "點擊查看比對結果"

    companion object {
        fun empty(): ProductComparisonRecord = ProductComparisonRecord()
    }
}

@Serializable
@Parcelize
data class ComparisonProduct(
    val originalName: String = "",
    val translatedName: String = ""
) : Parcelable

@Serializable
@Parcelize
data class ComparisonPoint(
    val aspect: String = "",
    val values: List<String> = emptyList()
) : Parcelable
