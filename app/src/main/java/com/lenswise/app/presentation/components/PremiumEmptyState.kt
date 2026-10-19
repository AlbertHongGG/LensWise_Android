package com.lenswise.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 尊榮空狀態插畫元件 (PremiumEmptyState)。
 * 1:1 復刻 LensWise 規格：
 * - 112dp 環形半透明微光 Halo 圓形容器
 * - 56dp 向量圖標（AppColors.primary 85% alpha）
 * - 20sp Bold 標題（OutfitFontFamily）
 * - 15sp 柔和說明文字（22sp 行高、置中對齊）
 * 供歷史紀錄 (HistoryScreen) 與 AI 日誌 (AiLogViewerScreen) 等各大模組標準共用。
 */
@Composable
fun PremiumEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color.White else Color.Black

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(baseColor.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, baseColor.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = baseColor.copy(alpha = 0.90f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontFamily = OutfitFontFamily,
                fontSize = 15.sp,
                color = baseColor.copy(alpha = 0.50f),
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
