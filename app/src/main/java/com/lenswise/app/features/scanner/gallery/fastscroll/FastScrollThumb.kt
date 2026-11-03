package com.lenswise.app.features.scanner.gallery.fastscroll

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lenswise.app.presentation.theme.AppColors

/**
 * 快速滑動條之滑塊膠囊視覺元件 (FastScrollThumb)。
 *
 * 核心保證：
 * 1. 空間指示：常態滑動時作為細版膠囊指示進度，按住拖曳時加粗並切換為主色調。
 * 2. 零重組幾何位移：透過 [Modifier.graphicsLayer] 在 GPU 層更新位移與透明度，杜絕 Measure / Layout 排版突變。
 */
@Composable
fun FastScrollThumb(
    state: FastScrollerState,
    isDark: Boolean,
    thumbHeightDp: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val thumbWidth by animateDpAsState(
        targetValue = if (state.isDragging) 6.5.dp else 4.dp,
        animationSpec = tween(150),
        label = "thumbWidth"
    )

    Box(
        modifier = modifier
            .padding(end = 3.dp)
            .graphicsLayer {
                translationY = state.dragOffsetY
                alpha = state.thumbAlphaAnim.value
            }
            .size(width = thumbWidth, height = thumbHeightDp)
            .background(
                color = if (state.isDragging) {
                    AppColors.primary
                } else if (isDark) {
                    Color.White.copy(alpha = 0.55f)
                } else {
                    Color.Black.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(3.dp)
            )
    )
}
