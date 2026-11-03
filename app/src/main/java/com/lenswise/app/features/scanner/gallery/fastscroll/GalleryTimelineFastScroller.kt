package com.lenswise.app.features.scanner.gallery.fastscroll

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.lenswise.app.core.media.GalleryUiItem

/**
 * 現代化月份離散時間軸快速滑動條 (GalleryTimelineFastScroller)。
 *
 * 核心保證：
 * 1. 職責分離：由 [FastScrollerState] 統一管理手勢換算、位置計算、吸附跳轉排程與可見度。
 * 2. 標籤嚴格受控：時間軸年月氣泡僅在手指「抓住拖曳」滑桿時顯示，常態瀏覽相片時完全隱藏，
 *    消滅視覺干擾與無意義的 CPU 算力浪費。
 * 3. 離散月份吸附：快速滑動條以「月份 (DateBucket)」為吸附單位，僅在跨月時發起 scrollToItem。
 * 4. 零重組幾何渲染：滑塊與氣泡之垂直位移完全在 GPU 渲染階段 ([Modifier.graphicsLayer]) 完成。
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

    val state = rememberFastScrollerState(
        timeline = timeline,
        gridState = gridState
    )

    // 當非拖曳狀態且網格正在滑動時，被動同步滑塊位置 (絕不計算 activeBucket)
    LaunchedEffect(gridState.firstVisibleItemIndex) {
        state.onGridScrolled(gridState.firstVisibleItemIndex)
    }

    // 滑塊自動顯現與淡出協程排程
    LaunchedEffect(gridState.isScrollInProgress, state.isDragging) {
        state.handleThumbVisibilityAnimation(gridState.isScrollInProgress)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onSizeChanged { size -> state.updateTrackHeight(size.height.toFloat()) }
    ) {
        // 1. 懸浮年月氣泡 (僅按住拖曳時顯現，隨手指平滑跟隨)
        FastScrollDateBubble(
            state = state,
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
                .pointerInput(state) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            state.onDragStart(offset.y)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            state.onDragDelta(dragAmount)
                        },
                        onDragEnd = {
                            state.onDragRelease()
                        },
                        onDragCancel = {
                            state.onDragRelease()
                        }
                    )
                }
        ) {
            // 3. 視覺滑塊 (膠囊體)
            FastScrollThumb(
                state = state,
                isDark = isDark,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
