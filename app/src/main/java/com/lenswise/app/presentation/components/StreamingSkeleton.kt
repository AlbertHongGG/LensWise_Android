package com.lenswise.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 串流載入微光呼吸骨架屏 (StreamingSkeleton)。
 *
 * 1:1 對齊 Flutter 的 StreamingSkeleton 核心動畫與渲染邏輯：
 * 1. 週期 1200ms 的反覆緩動脈衝 (FastOutSlowInEasing)。
 * 2. 透明度 (alpha) 在 0.25 至 0.65 之間平滑插值。
 * 3. 亮色模式以柔黑 (Color.Black) 為基底乘算 0.08，暗色模式以純白 (Color.White) 為基底乘算 0.15。
 * 4. 預設圓角 12.dp。
 */
@Composable
fun StreamingSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_skeleton")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    val multiplier = if (isDark) 0.15f else 0.08f
    val baseColor = if (isDark) Color.White else Color.Black
    val skeletonColor = baseColor.copy(alpha = alphaAnim * multiplier)

    val sizeModifier = when {
        width != null && height != null -> modifier.width(width).height(height)
        width != null -> modifier.width(width)
        height != null -> modifier.height(height)
        else -> modifier
    }

    Box(
        modifier = sizeModifier
            .clip(shape)
            .background(skeletonColor, shape)
    )
}
