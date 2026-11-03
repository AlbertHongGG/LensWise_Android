package com.lenswise.app.core.image

/**
 * 圖片裁切策略密封介面。
 * 封裝影像在規格化預處理階段的幾何裁切意圖。
 */
sealed interface ImageCropPolicy {

    /**
     * 不裁切，保留原始影像全部內容（用於相簿選圖 GALLERY）。
     */
    data object None : ImageCropPolicy

    /**
     * 居中填滿裁切（對應 PreviewView.ScaleType.FILL_CENTER）。
     * 根據全螢幕視口寬高比 (width / height) 裁切掉超出視野的多餘邊界。
     *
     * @property targetAspectRatio 目標寬高比（例如 1080f / 2400f = 0.45f）
     */
    data class CenterFillAspectRatio(val targetAspectRatio: Float) : ImageCropPolicy
}
