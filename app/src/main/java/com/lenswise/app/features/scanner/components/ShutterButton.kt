package com.lenswise.app.features.scanner.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 專屬雙環快門按鈕元件（按鈕本體動態引擎，零全螢幕干擾）。
 *
 * 核心保證：
 * 1. 徹底淘汰中心黑色轉圈，內圈始終保持純白乾淨質感。
 * 2. 點擊機械微縮（60dp -> 48dp）。
 * 3. 拍攝中外環 76dp 邊框轉換為旋轉白光弧 (Orbital Light Arc)。
 */
@Composable
fun ShutterButton(
    isCapturing: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 內圓尺寸目標計算：平時 60dp，按下 52dp，拍攝中 48dp
    val targetInnerSize = when {
        isCapturing -> 48f
        isPressed -> 52f
        else -> 60f
    }

    val animatedInnerSize by animateFloatAsState(
        targetValue = targetInnerSize,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "innerSize"
    )

    // 拍攝中內圓微呼吸感
    val infiniteTransition = rememberInfiniteTransition(label = "capturingTransition")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // 外環光弧旋轉角度 (0 ~ 360 度)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalRotation"
    )

    Box(
        modifier = modifier
            .size(76.dp)
            .clickable(
                enabled = !isLocked && !isCapturing,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. 外環半透明背景
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
        )

        // 2. 外環邊框：常態為純白靜止 4dp 框；拍攝中為 Canvas 動態旋轉光弧
        if (isCapturing) {
            Canvas(modifier = Modifier.size(76.dp)) {
                val strokeWidthPx = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val arcSize = size.minDimension - strokeWidthPx

                // 背景半透明底軌
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    style = Stroke(width = strokeWidthPx)
                )

                // 旋轉中的 100 度流暢白色光弧
                drawArc(
                    color = Color.White,
                    startAngle = rotationAngle,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(width = 4.dp, color = Color.White, shape = CircleShape)
            )
        }

        // 3. 內圈純白按鈕（無任何雜色菊花轉圈）
        Box(
            modifier = Modifier
                .size(animatedInnerSize.dp)
                .scale(if (isCapturing) breathingScale else 1.0f)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

