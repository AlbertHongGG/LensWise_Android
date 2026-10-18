package com.lenswise.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * LensWise 應用程式色彩定義與調色盤。
 */
object AppColors {
    // Backgrounds
    val backgroundDark = Color(0xFF0F111A)  // Deep Void Blue
    val backgroundLight = Color(0xFFF5F7FA) // Soft Grey-White

    val surfaceDark = Color(0xFF1E2130)
    val surfaceLight = Color(0xFFFFFFFF)

    // Accents
    val primary = Color(0xFF475569)         // Muted Slate 600
    val secondary = Color(0xFF94A3B8)       // Slate 400
    val electricBlue = Color(0xFF3B82F6)    // Glowing Accent Blue

    // Text
    val textPrimaryDark = Color(0xFFFFFFFF)
    val textSecondaryDark = Color(0xFFAAB2C8)

    val textPrimaryLight = Color(0xFF1A1D2B)
    val textSecondaryLight = Color(0xFF6E768C)

    // Semantic Accents
    val success = Color(0xFF10B981)         // Emerald
    val danger = Color(0xFFEF4444)          // Red Accent
    val warning = Color(0xFFF59E0B)         // Amber
    val info = Color(0xFF3B82F6)            // Blue
    val borderLight = Color(0x0F000000)     // Black 6% subtle border
    val borderDark = Color(0x1FFFFFFF)      // White 12% subtle border
    val divider = Color(0x0D000000)         // Black 5% indented divider
}
