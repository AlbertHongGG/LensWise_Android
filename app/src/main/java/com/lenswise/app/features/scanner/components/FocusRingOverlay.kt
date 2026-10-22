package com.lenswise.app.features.scanner.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FocusRingOverlay(
    targetPoint: Offset?,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (targetPoint == null) return

    val density = LocalDensity.current
    val scaleAnim = remember { Animatable(1.35f) }
    val alphaAnim = remember { Animatable(1.0f) }

    LaunchedEffect(targetPoint) {
        scaleAnim.snapTo(1.35f)
        alphaAnim.snapTo(1.0f)

        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )

        delay(800)

        alphaAnim.animateTo(
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 250)
        )

        onAnimationEnd()
    }

    val boxSize = 64.dp
    val boxSizePx = with(density) { boxSize.toPx() }
    val xOffset = with(density) { (targetPoint.x - boxSizePx / 2f).toDp() }
    val yOffset = with(density) { (targetPoint.y - boxSizePx / 2f).toDp() }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset(x = xOffset, y = yOffset)
                .size(boxSize)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}

