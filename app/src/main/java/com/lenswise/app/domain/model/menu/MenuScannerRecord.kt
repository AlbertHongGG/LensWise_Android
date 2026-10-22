package com.lenswise.app.domain.model.menu

import android.os.Parcelable
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 菜單掃描與翻譯領域核心實體 (MenuScannerRecord)。
 * 實作 [IHistoryRecord] 支援統一歷史展示；實作 [Parcelable] 支援 Compose 導航傳遞。
 */
@Serializable
@Parcelize
data class MenuScannerRecord(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    val storeName: String = "",
    val items: List<MenuScannerItem> = emptyList(),
    override val imagePaths: List<String> = emptyList(),
    val translatedImagePaths: Map<String, String> = emptyMap()
) : IHistoryRecord, Parcelable {

    override val displayTitle: String
        get() = storeName.ifBlank { "未知餐廳菜單" }

    override val displaySubtitle: String
        get() = "${items.size} 個品項"

    companion object {
        fun empty(): MenuScannerRecord = MenuScannerRecord()
    }
}

@Serializable
@Parcelize
data class MenuScannerItem(
    val originalName: String = "",
    val translatedName: String = "",
    val description: String? = null,
    val price: Double = 0.0
) : Parcelable
