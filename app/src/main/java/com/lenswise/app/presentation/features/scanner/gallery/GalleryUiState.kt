package com.lenswise.app.presentation.features.scanner.gallery

import android.net.Uri
import com.lenswise.app.core.media.GalleryUiItem

/**
 * 相簿選擇器不可變 UI 狀態 (GalleryUiState)。
 * 純淨狀態機模型，無任何殘留屬性或人為上限設限。
 */
data class GalleryUiState(
    val items: List<GalleryUiItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val selectedUris: List<Uri> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
) {
    val canConfirm: Boolean get() = selectedIds.isNotEmpty()
    val selectionCount: Int get() = selectedIds.size
}
