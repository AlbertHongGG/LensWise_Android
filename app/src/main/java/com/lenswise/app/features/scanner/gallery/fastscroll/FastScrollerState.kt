package com.lenswise.app.features.scanner.gallery.fastscroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lenswise.app.core.media.DateBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 快速滑動條狀態持有者 (FastScrollerState)。
 *
 * 核心保證：
 * 1. 職責分離 (SoC)：
 *    - 滑塊 (Thumb)：空間位置提示，常態網格滑動時顯現，停止時淡出；
 *    - 時間標籤 (Date Tag Bubble)：主動時間軸穿梭工具，嚴格「僅在拖曳 (isDragging == true)」時顯現，
 *      常態滑動時完全隱藏且不執行無效的 DateBucket 查詢與字串測量。
 * 2. 零重組渲染 (Recomposition-Free)：
 *    - 滑塊與氣泡之垂直位移完全在 GPU 渲染階段 ([Modifier.graphicsLayer]) 調用，不觸發 Measure / Layout 排版突變。
 * 3. 離散月份吸附 (Discrete Section Snapping)：
 *    - 拖曳過程氣泡隨手指 120 FPS 平滑連續移動，但網格僅在跨越月份時發起 scrollToItem，杜絕無意義中途滾動雪崩。
 */
@Stable
class FastScrollerState(
    val timeline: FastScrollTimeline,
    private val gridState: LazyGridState,
    private val coroutineScope: CoroutineScope,
    private val haptic: HapticFeedback?,
    val thumbHeightPx: Float,
    val bubbleHeightPx: Float
) {
    /** 軌道可用高度 (px) */
    var trackHeightPx by mutableFloatStateOf(0f)
        private set

    /** 是否正在按住拖曳滑動條 */
    var isDragging by mutableStateOf(false)
        private set

    /** 滑塊在軌道上的垂直像素位移 */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    /** 當前拖曳選中之月份 (常態滑動時維持 null，杜絕無效計算) */
    var activeBucket by mutableStateOf<DateBucket?>(null)
        private set

    /** 上一次吸附觸發之月份索引，避免同月份微小晃動重複震動與滾動 */
    var lastSnappedSectionIndex by mutableIntStateOf(-1)
        private set

    /** 滑塊淡入淡出透明度動畫狀態機 */
    val thumbAlphaAnim: Animatable<Float, AnimationVector1D> = Animatable(0f)

    /** 滑塊最大允許位移量 */
    val maxThumbOffsetY: Float
        get() = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

    /** 時間年月標籤氣泡是否可見：嚴格限定僅在按住拖曳中且有有效月份時可見 */
    val isBubbleVisible: Boolean
        get() = isDragging && activeBucket != null

    /**
     * 更新軌道高度 (通常在 onSizeChanged 觸發)。
     */
    fun updateTrackHeight(heightPx: Float) {
        trackHeightPx = heightPx
    }

    /**
     * 常態相簿網格滾動聯動：僅被動同步滑塊位置，絕不計算 activeBucket。
     */
    fun onGridScrolled(firstVisibleItemIndex: Int) {
        if (!isDragging && trackHeightPx > 0f) {
            val fraction = timeline.getFractionForVisibleItem(firstVisibleItemIndex)
            dragOffsetY = (fraction * maxThumbOffsetY).coerceIn(0f, maxThumbOffsetY)
            // 關鍵防線：常態滑動不賦值 activeBucket，保護 CPU 算力並消除標籤干擾
        }
    }

    /**
     * 處理滑塊透明度淡入淡出排程。
     */
    suspend fun handleThumbVisibilityAnimation(isScrollInProgress: Boolean) {
        if (isDragging) {
            thumbAlphaAnim.snapTo(1f)
        } else if (isScrollInProgress) {
            thumbAlphaAnim.animateTo(1f, animationSpec = tween(150))
        } else {
            delay(1200L)
            thumbAlphaAnim.animateTo(0f, animationSpec = tween(300))
        }
    }

    /**
     * 手指按住滑動條啟動拖曳 (Touch Down / Drag Start)。
     */
    fun onDragStart(touchY: Float) {
        isDragging = true
        coroutineScope.launch { thumbAlphaAnim.snapTo(1f) }

        val clampedY = (touchY - thumbHeightPx / 2f).coerceIn(0f, maxThumbOffsetY)
        dragOffsetY = clampedY

        val fraction = if (maxThumbOffsetY > 0f) clampedY / maxThumbOffsetY else 0f
        val sectionIndex = timeline.getSectionIndex(fraction)
        val bucket = timeline.getBucketForSection(sectionIndex)
        activeBucket = bucket

        if (sectionIndex != lastSnappedSectionIndex) {
            lastSnappedSectionIndex = sectionIndex
            haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val targetHeaderIndex = timeline.getHeaderItemIndexForSection(sectionIndex)
            coroutineScope.launch {
                gridState.scrollToItem(targetHeaderIndex)
            }
        }
    }

    /**
     * 拖曳位移更新 (Drag Delta)。
     */
    fun onDragDelta(dragAmountPx: Float) {
        if (!isDragging) return
        val newY = (dragOffsetY + dragAmountPx).coerceIn(0f, maxThumbOffsetY)
        dragOffsetY = newY

        val fraction = if (maxThumbOffsetY > 0f) newY / maxThumbOffsetY else 0f
        val sectionIndex = timeline.getSectionIndex(fraction)
        val bucket = timeline.getBucketForSection(sectionIndex)
        activeBucket = bucket

        // 離散月份吸附：僅當跨入新月份時才觸發跳轉與震動
        if (sectionIndex != lastSnappedSectionIndex) {
            lastSnappedSectionIndex = sectionIndex
            haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val targetHeaderIndex = timeline.getHeaderItemIndexForSection(sectionIndex)
            coroutineScope.launch {
                gridState.scrollToItem(targetHeaderIndex)
            }
        }
    }

    /**
     * 結束或取消拖曳手勢 (Drag End / Cancel)。
     */
    fun onDragRelease() {
        isDragging = false
        activeBucket = null
        lastSnappedSectionIndex = -1
    }

    /**
     * 計算氣泡 Y 軸座標 (GPU Translation 專用，自帶頂底部邊界保護)。
     */
    fun getBubbleOffsetY(): Float {
        val centerThumbY = dragOffsetY + thumbHeightPx / 2f
        return (centerThumbY - bubbleHeightPx / 2f)
            .coerceIn(8f, (trackHeightPx - bubbleHeightPx - 8f).coerceAtLeast(8f))
    }
}

/**
 * 記憶與工廠構造 [FastScrollerState] 的便捷 Composable。
 */
@Composable
fun rememberFastScrollerState(
    timeline: FastScrollTimeline,
    gridState: LazyGridState,
    thumbHeightDp: Dp = 48.dp,
    bubbleHeightDp: Dp = 36.dp
): FastScrollerState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val bubbleHeightPx = with(density) { bubbleHeightDp.toPx() }

    return remember(timeline, gridState) {
        FastScrollerState(
            timeline = timeline,
            gridState = gridState,
            coroutineScope = coroutineScope,
            haptic = haptic,
            thumbHeightPx = thumbHeightPx,
            bubbleHeightPx = bubbleHeightPx
        )
    }
}
