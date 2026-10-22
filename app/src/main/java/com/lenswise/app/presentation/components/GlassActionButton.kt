package com.lenswise.app.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.lenswise.app.presentation.theme.GlassStyle

/**
 * 擬毛玻璃懸浮按鈕 (GlassActionButton)。
 * 1:1 復刻 LensWise Flutter 版 glass_action_button.dart：
 * 44x44 圓形、微光半透明背景、即時高斯模糊取樣 (0 雜訊)、0.5dp 精緻光澤切邊與柔和向下微陰影 (無黑邊)。
 *
 * 架構革新：
 * - 徹底移除髒標記補丁 (backdropDirtyTrigger)，改由視口空間架構驅動渲染。
 * - 徹底移除 hazeChild 後多餘的 .background()，解決雙重著色導致模糊被覆蓋的問題。
 * - 支援傳入視口 scrollState，在滑動時精確觸發繪圖相依更新，達到幀級即時毛玻璃動態更新。
 */
@Composable
fun GlassActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    hazeState: HazeState? = null,
    scrollState: ScrollState? = null
) {
    val style = remember(isDark) { GlassStyle.actionButton(isDark) }

    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(CircleShape)
            .then(
                if (scrollState != null) {
                    Modifier.drawWithContent {
                        scrollState.value
                        drawContent()
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = style.blurRadius
                        noiseFactor = style.noiseFactor
                        this.backgroundColor = style.tintColor
                        tints = listOf(HazeTint(style.tintColor))
                    }
                } else {
                    Modifier.background(style.tintColor)
                }
            )
            .border(
                width = style.borderWidth,
                color = style.borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = style.iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
