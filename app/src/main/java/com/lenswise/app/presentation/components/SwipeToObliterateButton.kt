package com.lenswise.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 滑動抹除確認按鈕 (SwipeToObliterateButton)。
 * 1:1 復刻 GeminiFlowApp / LensWise 核心抹除交互：
 * - 68dp 膠囊高度、56dp 圓形白滑塊、6dp 安全內距。
 * - 實色鮮紅圓形揭示條（靜止時寬度為 0，絕無粉紅外溢或垂直切邊瑕疵）。
 * - 拖曳時置中文字流暢淡出，95% 閥值切換打勾圖標並觸發觸覺震動。
 */
@Composable
fun SwipeToObliterateButton(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "SLIDE TO WIPE",
    isLoading: Boolean = false,
    activeColor: Color = Color(0xFFEF4444)
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val thumbSize = 56.dp
    val padding = 6.dp
    val trackHeight = thumbSize + (padding * 2) // 68.dp

    val trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val textColor = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f)
    val thumbColor = Color.White

    val animatableOffset = remember { Animatable(0f) }
    var isConfirmed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                shape = CircleShape
            )
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val paddingPx = with(density) { padding.toPx() }
        val maxDragPx = (maxWidthPx - thumbSizePx - (paddingPx * 2)).coerceAtLeast(0f)

        val currentDrag = animatableOffset.value
        val progress = if (maxDragPx > 0f) (currentDrag / maxDragPx).coerceIn(0f, 1f) else 0f

        // 鮮紅背景揭示條 (靜止時寬度為 0，拖曳時緊隨滑塊右緣延伸，絕不產生粉紅溢色或垂直平切面)
        if (currentDrag > 0f) {
            val revealWidthDp = with(density) { (thumbSizePx + (paddingPx * 2) + currentDrag).toDp() }
            Box(
                modifier = Modifier
                    .width(revealWidthDp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }

        // 置中提示文字（隨拖曳流暢漸隱）
        val textAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(textAlpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = textColor
            )
        }

        // 白色滑塊 (Thumb)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (paddingPx + currentDrag).roundToInt(),
                        y = paddingPx.roundToInt()
                    )
                }
                .size(thumbSize)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.15f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.2f)
                )
                .clip(CircleShape)
                .background(thumbColor)
                .pointerInput(isLoading, isConfirmed, maxDragPx) {
                    if (isLoading || isConfirmed || maxDragPx <= 0f) return@pointerInput

                    detectHorizontalDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (animatableOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                animatableOffset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            if (animatableOffset.value >= maxDragPx * 0.95f) {
                                // 達成 95% 抹除門檻：確認執行
                                isConfirmed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    animatableOffset.animateTo(maxDragPx)
                                    onConfirmed()
                                }
                            } else {
                                // 未達門檻：彈簧回彈至起點
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    animatableOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animatableOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = activeColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isConfirmed) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已確認",
                    tint = activeColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "滑動以確認",
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 相容別名，確保既有介面呼叫無縫對齊
 */
@Composable
fun SwipeToConfirmButton(
    title: String = "SLIDE TO WIPE",
    confirmedTitle: String = "RELEASE TO CLEAR",
    activeColor: Color = Color(0xFFEF4444),
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToObliterateButton(
        title = title,
        activeColor = activeColor,
        onConfirmed = onConfirmed,
        modifier = modifier
    )
}
