package com.lenswise.app.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors

/**
 * 頁面頂部配置標題區塊，包含主標題、副標題與背景裝飾 ThinkingOrb。
 * 100% 復刻 LensWise 的 PremiumConfigHeader 排版與字級間距。
 */
@Composable
fun PremiumConfigHeader(
    title: String = "偏好設定",
    subtitle: String = "SYSTEM CONFIGURATION",
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 36.dp, bottom = 20.dp)
    ) {
        // 裝飾用 ThinkingOrb：設定佈局尺寸為 (0, 0)，避免影響父容器高度計算
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, minHeight = 0)
                    )
                    layout(0, 0) {
                        placeable.placeRelative(
                            -placeable.width + 16.dp.roundToPx(),
                            -placeable.height / 2
                        )
                    }
                }
                .alpha(if (isDark) 0.25f else 0.15f)
        ) {
            ThinkingOrb(size = 150.dp, isDark = isDark)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else AppColors.textPrimaryLight,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 36.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle.uppercase(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f),
                        letterSpacing = 1.5.sp,
                        lineHeight = 14.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailing()
            }
        }
    }
}
