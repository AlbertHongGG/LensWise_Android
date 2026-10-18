package com.lenswise.app.presentation.features.scanner

import android.graphics.Bitmap
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.scanner.ScannerMode

/**
 * 主畫面 UI 不可變狀態模型。
 */
data class ScannerUiState(
    val currentMode: ScannerMode = ScannerMode.PRODUCT,
    val capturedImages: List<AppImageEntity> = emptyList(),
    val isCapturing: Boolean = false,
    val isPickingGallery: Boolean = false,
    val isAnalyzing: Boolean = false
) {
    /** 是否已滿足當前模式的最低照片門檻（比對需 >= 2，其餘需 >= 1） */
    val canTriggerAnalysis: Boolean
        get() = capturedImages.size >= currentMode.minShots

    /** 最新的照片預覽縮圖 */
    val latestThumbnail: Bitmap?
        get() = capturedImages.lastOrNull()?.thumbnailBitmap

    /** 當前已捕獲照片總張數 */
    val imageCount: Int
        get() = capturedImages.size

    val isLocked: Boolean
        get() = isCapturing || isPickingGallery || isAnalyzing
}
