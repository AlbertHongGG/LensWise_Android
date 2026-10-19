package com.lenswise.app.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 啟動開屏專屬品牌排版規範 (SplashTypography)。
 *
 * 核心保證：
 * 1. 1:1 精確還原原版 App (圖一) 俐落幹練之筆重與字距結構。
 * 2. 採用系統原生高品質 Sans-Serif 字型，第 0 毫秒即時確定渲染，杜絕字型非同步加載延遲與 Faux Bold 粗鈍感。
 * 3. 物件化集中管理主標題與副標題樣式，具備高度維護性與可擴展性。
 */
object SplashTypography {

    val titleStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold, // w700: 幹練俐落、線條清晰，精確對齊原版視覺
        color = Color(0xFF1D1D1F), // Deep iron grey
        letterSpacing = 2.0.sp
    )

    val subtitleStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium, // w500: 纖細優雅、消除過黑粗鈍感
        color = Color(0xFF86868B), // Subtle secondary grey
        letterSpacing = 6.0.sp // 大字距呼吸感
    )
}
