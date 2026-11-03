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
import androidx.compose.runtime.mutableIntStateOf
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
 * 現代化月份離散時間軸快速滑動條 (GalleryTimelineFastScroller)。
 *
 * 核心保證：
 * 1. 對標 Google 相簿互動模型：快速滑動條以「月份 (DateBucket)」為離散吸附單位。
 *    拖曳時年月氣泡 120 FPS 平滑連續隨手指更新，但網格僅在「跨越不同月份交界」時發起 scrollToItem，
 *    徹底消除中間無意義數百張照片的高頻重組與協程雪崩。
 * 2. 觸控閘門：對外回呼 [onFastScrollDraggingChanged]，讓圖片單元格在高速拖曳期間暫停高解析度圖片請求，
 *    100% 算力留給畫面流暢運轉。
 * 3. 雙向時間軸穿梭：頂部 0.0 -> 最新月份；底部 1.0 -> 歷史最久遠月份。
 * 4. 零重組幾何渲染 (Recomposition-Free)：滑塊與氣泡之垂直位移完全在 GPU 渲染階段 ([Modifier.graphicsLayer]) 完成。
 * 5. 觸覺反饋：僅在吸附切換至新月份時觸發微震動 (TextHandleMove)。
 */
@Composable
fun GalleryTimelineFastScroller(
    gridState: LazyGridState,
    items: List<GalleryUiItem>,
    isDark: Boolean,
    onFastScrollDraggingChanged: ((Boolean) -> Unit)? = null,
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
    var lastSnappedSectionIndex by remember { mutableIntStateOf(-1) }
    val alphaAnim = remember { Animatable(0f) }

    val maxThumbOffsetY = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

    // 當非拖曳狀態且網格正在滑動時，被動同步滑塊位置
    if (!isDragging && trackHeightPx > 0f) {
        val firstVisible = gridState.firstVisibleItemIndex
        val fraction = timeline.getFractionForVisibleItem(firstVisible)
        dragOffsetY = (fraction * maxThumbOffsetY).coerceIn(0f, maxThumbOffsetY)
        activeBucket = timeline.getBucketForVisibleItem(firstVisible)
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
        // 1. 懸浮年月氣泡 (浮動於滑塊左側，隨手指平滑跟隨)
        FastScrollDateBubble(
            bucket = activeBucket,
            visible = (isDragging || gridState.isScrollInProgress) && alphaAnim.value > 0.05f,
            offsetYProvider = {
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
                            onFastScrollDraggingChanged?.invoke(true)
                            val clampedY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxThumbOffsetY)
                            dragOffsetY = clampedY
                            val fraction = if (maxThumbOffsetY > 0f) clampedY / maxThumbOffsetY else 0f
                            val sectionIndex = timeline.getSectionIndex(fraction)
                            val bucket = timeline.getBucketForSection(sectionIndex)
                            activeBucket = bucket
                            if (sectionIndex != lastSnappedSectionIndex) {
                                lastSnappedSectionIndex = sectionIndex
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val targetHeaderIndex = timeline.getHeaderItemIndexForSection(sectionIndex)
                                coroutineScope.launch {
                                    gridState.scrollToItem(targetHeaderIndex)
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val newY = (dragOffsetY + dragAmount).coerceIn(0f, maxThumbOffsetY)
                            dragOffsetY = newY
                            val fraction = if (maxThumbOffsetY > 0f) newY / maxThumbOffsetY else 0f
                            val sectionIndex = timeline.getSectionIndex(fraction)
                            val bucket = timeline.getBucketForSection(sectionIndex)
                            activeBucket = bucket
                            // 🌟 關鍵吸附：僅當進入新月份時才觸發跳轉，中斷無意義中間滾動！
                            if (sectionIndex != lastSnappedSectionIndex) {
                                lastSnappedSectionIndex = sectionIndex
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val targetHeaderIndex = timeline.getHeaderItemIndexForSection(sectionIndex)
                                coroutineScope.launch {
                                    gridState.scrollToItem(targetHeaderIndex)
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            lastSnappedSectionIndex = -1
                            onFastScrollDraggingChanged?.invoke(false)
                        },
                        onDragCancel = {
                            isDragging = false
                            lastSnappedSectionIndex = -1
                            onFastScrollDraggingChanged?.invoke(false)
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
