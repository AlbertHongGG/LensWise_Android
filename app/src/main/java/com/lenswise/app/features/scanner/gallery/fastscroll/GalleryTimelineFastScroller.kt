package com.lenswise.app.features.scanner.gallery.fastscroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.presentation.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 現代化時間軸快速滑動條 (GalleryTimelineFastScroller)。
 *
 * 核心保證：
 * 1. 對標 Google 相簿交互：右側提供 44dp 舒適觸控帶，拖曳時滑塊加粗，左側動態彈出懸浮年月氣泡。
 * 2. 雙向時間軸穿梭：
 *    - 頂部 0.0 -> 最新照片與月份。
 *    - 底部 1.0 -> 歷史最久遠照片，100% 直達全量相簿歷史底部。
 * 3. 觸覺反饋：拖曳跨越月份交界時，觸發細緻的微震動 (TextHandleMove)。
 * 4. 零重組幾何渲染 (Recomposition-Free)：滑塊與氣泡之垂直位移完全在 GPU 渲染階段 ([Modifier.graphicsLayer]) 完成。
 * 5. 自動淡出：停止滑動或放開手指 1,200ms 後，滑動條與氣泡平滑淡出，無干擾感。
 */
@Composable
fun GalleryTimelineFastScroller(
    gridState: LazyGridState,
    items: List<GalleryUiItem>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val timeline = remember(items) { FastScrollTimeline(items) }
    if (!timeline.isValid) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val bubbleEstimatedHeightPx = with(density) { 36.dp.toPx() }

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var activeBucket by remember { mutableStateOf<DateBucket?>(null) }
    val alphaAnim = remember { Animatable(0f) }

    val maxThumbOffsetY = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

    // 當非拖曳狀態且網格正在滑動時，被動同步滑塊位置
    if (!isDragging && trackHeightPx > 0f) {
        val firstVisible = gridState.firstVisibleItemIndex
        val fraction = timeline.getFractionForIndex(firstVisible)
        dragOffsetY = (fraction * maxThumbOffsetY).coerceIn(0f, maxThumbOffsetY)
        activeBucket = timeline.getBucketForIndex(firstVisible)
    }

    // 自動顯現與淡出協程排程
    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (isDragging) {
            alphaAnim.snapTo(1f)
        } else if (gridState.isScrollInProgress) {
            alphaAnim.animateTo(1f, animationSpec = tween(150))
        } else {
            delay(1200L)
            alphaAnim.animateTo(0f, animationSpec = tween(300))
        }
    }

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 6.5.dp else 4.dp,
        animationSpec = tween(150),
        label = "thumbWidth"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onSizeChanged { size -> trackHeightPx = size.height.toFloat() }
    ) {
        // 1. 懸浮年月氣泡 (浮動於滑塊左側)
        FastScrollDateBubble(
            bucket = activeBucket,
            visible = (isDragging || gridState.isScrollInProgress) && alphaAnim.value > 0.05f,
            offsetYProvider = {
                // 將氣泡幾何中心對齊滑塊中心，並進行邊界保護
                val centerThumbY = dragOffsetY + thumbHeightPx / 2f
                (centerThumbY - bubbleEstimatedHeightPx / 2f)
                    .coerceIn(8f, (trackHeightPx - bubbleEstimatedHeightPx - 8f).coerceAtLeast(8f))
            },
            isDark = isDark,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp)
        )

        // 2. 右側手勢專屬截獲區 (44dp 寬度便於手指點按)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(44.dp)
                .fillMaxHeight()
                .pointerInput(timeline, maxThumbOffsetY) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val clampedY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxThumbOffsetY)
                            dragOffsetY = clampedY
                            val fraction = if (maxThumbOffsetY > 0f) clampedY / maxThumbOffsetY else 0f
                            val targetIndex = timeline.getTargetIndex(fraction)
                            val bucket = timeline.getBucketForIndex(targetIndex)
                            if (bucket != activeBucket) {
                                activeBucket = bucket
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val newY = (dragOffsetY + dragAmount).coerceIn(0f, maxThumbOffsetY)
                            dragOffsetY = newY
                            val fraction = if (maxThumbOffsetY > 0f) newY / maxThumbOffsetY else 0f
                            val targetIndex = timeline.getTargetIndex(fraction)
                            val bucket = timeline.getBucketForIndex(targetIndex)
                            if (bucket != null && bucket != activeBucket) {
                                activeBucket = bucket
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            // 3. 視覺滑塊 (膠囊體)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp)
                    .graphicsLayer {
                        translationY = dragOffsetY
                        alpha = alphaAnim.value
                    }
                    .size(width = thumbWidth, height = thumbHeightDp)
                    .background(
                        color = if (isDragging) {
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
    }
}
