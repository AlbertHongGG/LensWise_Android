package com.lenswise.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 擬毛玻璃（Frosted Glass）樣式規範數據模型。
 * 1:1 對齊 LensWise Flutter 核心視覺規範 (glass_action_button.dart & glass_container.dart)。
 */
@Immutable
data class GlassStyle(
    val blurRadius: Dp = 12.dp,
    val noiseFactor: Float = 0f,
    val tintColor: Color,
    val borderColor: Color,
    val borderWidth: Dp = 0.5.dp,
    val iconColor: Color = Color.Unspecified
) {
    companion object {
        /**
         * 懸浮功能按鈕 (GlassActionButton) 專用樣式：
         * - 12dp 高斯模糊、0 雜訊
         * - 淺色模式：純白 55% 遮罩、純白 35% 高光微邊框 (0.5dp)、深黑灰圖示 (0xFF111418)
         * - 深色模式：純黑 45% 遮罩、純白 15% 精緻微邊框 (0.5dp)、純白圖示
         */
        fun actionButton(isDark: Boolean): GlassStyle = GlassStyle(
            blurRadius = 12.dp,
            noiseFactor = 0f,
            tintColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.55f),
            borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.35f),
            borderWidth = 0.5.dp,
            iconColor = if (isDark) Color.White else Color(0xFF111418)
        )

        /**
         * 底部比對輪巡席次膠囊 (ComparisonRosterBar) 專用樣式：
         * - 12dp 高斯模糊、0 雜訊
         * - 淺色模式：純白 60% 遮罩、純白高光邊框 (1.5dp)
         * - 深色模式：純黑 40% 遮罩、純白 15% 微光邊框 (1.5dp)
         */
        fun rosterBar(isDark: Boolean): GlassStyle = GlassStyle(
            blurRadius = 12.dp,
            noiseFactor = 0f,
            tintColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.60f),
            borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White,
            borderWidth = 1.5.dp
        )

        /**
         * 主畫面相簿挑選按鈕 (GalleryPickerButton) 專用毛玻璃樣式：
         * 復刻精心設計的亮白擬毛玻璃質感 (Pure White Frosted Glass)：
         * - 12dp 高斯模糊、0 雜訊 (消除顆粒感)
         * - 純白 55% 遮罩透光、純白 35% 高光微邊框 (0.5dp)
         * - 柔和向下微陰影 (spotColor 0.10f，杜絕粗黑黑邊)
         */
        fun galleryButton(): GlassStyle = GlassStyle(
            blurRadius = 12.dp,
            noiseFactor = 0f,
            tintColor = Color.White.copy(alpha = 0.55f),
            borderColor = Color.White.copy(alpha = 0.35f),
            borderWidth = 0.5.dp,
            iconColor = Color.White
        )
    }
}
