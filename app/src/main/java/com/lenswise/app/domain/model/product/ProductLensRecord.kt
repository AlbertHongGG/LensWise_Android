package com.lenswise.app.domain.model.product

import android.os.Parcelable
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 商品深度分析領域核心實體 (ProductLensRecord)。
 * 實作 [IHistoryRecord] 支援統一歷史展示；實作 [Parcelable] 支援 Compose 導航傳遞。
 */
@Serializable
@Parcelize
data class ProductLensRecord(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    val productName: String = "",
    val originalName: String = "",
    val brand: String = "",
    val origin: String = "",
    val summary: String = "",
    val ingredients: List<String> = emptyList(),
    val allergenAlerts: List<String> = emptyList(),
    val dietaryFlags: List<String> = emptyList(),
    val whenRecommended: List<String> = emptyList(),
    val whenNotRecommended: List<String> = emptyList(),
    override val imagePaths: List<String> = emptyList()
) : IHistoryRecord, Parcelable {

    override val displayTitle: String
        get() = productName.ifBlank { originalName.ifBlank { "未知商品" } }

    override val displaySubtitle: String
        get() = brand.ifBlank { "未知品牌" }

    companion object {
        fun empty(): ProductLensRecord = ProductLensRecord()
    }
}
