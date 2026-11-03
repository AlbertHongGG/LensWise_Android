package com.lenswise.app.features.scanner.gallery.fastscroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 時間軸懸浮年月氣泡 (FastScrollDateBubble)。
 *
 * 核心保證：
 * 1. 對標 Google 相簿經典外觀：深色圓角膠囊氣泡，搭配半透明霧面背景與高對比文字。
 * 2. 嚴格可見度控制：僅在狀態為按住拖曳 ([FastScrollerState.isBubbleVisible]) 時顯現，常態滾動完全隱藏。
 * 3. 零重組幾何位移：透過 [Modifier.graphicsLayer] 於 GPU 渲染層更新垂直座標，杜絕任何 Composable 重組穿透。
 * 4. 邊界保護：垂直座標依據容器高度嚴密裁切 (Clamping)，絕不越界遮擋頂部 Header 或底部邊緣。
 * 5. 彈性動力學過渡：出現時帶有微微的彈性縮放 (scaleIn)，消失時迅速淡出 (fadeOut)。
 */
@Composable
fun FastScrollDateBubble(
    state: FastScrollerState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    FastScrollDateBubble(
        bucket = state.activeBucket,
        visible = state.isBubbleVisible,
        offsetYProvider = { state.getBubbleOffsetY() },
        isDark = isDark,
        modifier = modifier
    )
}

@Composable
fun FastScrollDateBubble(
    bucket: DateBucket?,
    visible: Boolean,
    offsetYProvider: () -> Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && bucket != null,
        enter = fadeIn(tween(120)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.85f, animationSpec = tween(180)),
        modifier = modifier.graphicsLayer {
            translationY = offsetYProvider()
        }
    ) {
        if (bucket != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
                    .background(
                        color = if (isDark) Color(0xFF222222).copy(alpha = 0.92f) else Color(0xFF1E1E1E).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = bucket.displayTitle,
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
