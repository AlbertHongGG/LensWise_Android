package com.lenswise.app.features.scanner.gallery.fastscroll

import android.net.Uri
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FastScrollerStateTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private lateinit var mockGridState: LazyGridState
    private lateinit var mockHaptic: HapticFeedback
    private lateinit var timeline: FastScrollTimeline

    private val b202611 = DateBucket(2026, 11)
    private val b202610 = DateBucket(2026, 10)
    private val b202609 = DateBucket(2026, 9)

    private fun createPhoto(id: Long, bucket: DateBucket): GalleryUiItem.Photo {
        val asset = MediaAsset(
            id = id,
            contentUri = mock(Uri::class.java),
            dateTakenMs = 1000L * id,
            bucket = bucket
        )
        return GalleryUiItem.Photo(asset = asset)
    }

    @Before
    fun setUp() {
        mockGridState = mock(LazyGridState::class.java)
        mockHaptic = mock(HapticFeedback::class.java)

        val items = listOf(
            GalleryUiItem.Header(bucket = b202611, totalCount = 2),
            createPhoto(1L, b202611),
            createPhoto(2L, b202611),
            GalleryUiItem.Header(bucket = b202610, totalCount = 2),
            createPhoto(3L, b202610),
            createPhoto(4L, b202610),
            GalleryUiItem.Header(bucket = b202609, totalCount = 2),
            createPhoto(5L, b202609),
            createPhoto(6L, b202609)
        )
        timeline = FastScrollTimeline(items)
    }

    @Test
    fun `initial state has bubble hidden and dragging false`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )

        assertFalse(state.isDragging)
        assertFalse("Date bubble must be strictly hidden initially", state.isBubbleVisible)
        assertNull("activeBucket must be null initially", state.activeBucket)
        assertEquals(0f, state.dragOffsetY, 0.001f)
    }

    @Test
    fun `normal grid scroll moves thumb but NEVER shows date bubble`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )
        state.updateTrackHeight(1000f)

        // 模擬使用者常態滑動網格至中間項目 (firstVisible = 4 -> 2026年10月)
        state.onGridScrolled(firstVisibleItemIndex = 4)

        assertFalse("isDragging must remain false during normal scrolling", state.isDragging)
        assertFalse("Date bubble must NEVER appear during normal grid scrolling", state.isBubbleVisible)
        assertNull("activeBucket must remain null during normal grid scroll", state.activeBucket)
        assertTrue("Thumb must have moved to indicate position", state.dragOffsetY > 0f)
    }

    @Test
    fun `drag start immediately activates dragging and shows date bubble with correct month`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )
        state.updateTrackHeight(1000f)

        // 按住滑動條頂部 (touchY = 50f)
        state.onDragStart(touchY = 50f)

        assertTrue("isDragging must be true", state.isDragging)
        assertTrue("Date bubble MUST be visible when user grabs the scroller", state.isBubbleVisible)
        assertEquals("Should identify the latest month at top", b202611, state.activeBucket)
    }

    @Test
    fun `drag delta updates month and maintains bubble visibility`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )
        state.updateTrackHeight(1000f)

        state.onDragStart(touchY = 50f)
        // 拖曳向下滑動至中底部 (delta = 850f)
        state.onDragDelta(dragAmountPx = 850f)

        assertTrue(state.isDragging)
        assertTrue(state.isBubbleVisible)
        assertEquals("Should snap to oldest month at bottom", b202609, state.activeBucket)
    }

    @Test
    fun `drag release immediately hides date bubble and resets activeBucket`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )
        state.updateTrackHeight(1000f)

        state.onDragStart(touchY = 500f)
        assertTrue(state.isBubbleVisible)
        assertNotNull(state.activeBucket)

        // 手指放開
        state.onDragRelease()

        assertFalse("isDragging must be false on release", state.isDragging)
        assertFalse("Date bubble MUST be hidden immediately on release", state.isBubbleVisible)
        assertNull("activeBucket must be reset to null", state.activeBucket)
        assertEquals(-1, state.lastSnappedSectionIndex)
    }

    @Test
    fun `bubble offset is clamped safely within container boundaries`() {
        val state = FastScrollerState(
            timeline = timeline,
            gridState = mockGridState,
            coroutineScope = testScope,
            haptic = mockHaptic,
            thumbHeightPx = 48f,
            bubbleHeightPx = 36f
        )
        state.updateTrackHeight(1000f)

        // 頂部邊界測試
        state.onDragStart(touchY = 0f)
        val topOffset = state.getBubbleOffsetY()
        assertTrue("Top bubble offset must not clip past 8dp", topOffset >= 8f)

        // 底部邊界測試
        state.onDragDelta(dragAmountPx = 2000f)
        val bottomOffset = state.getBubbleOffsetY()
        val maxAllowed = 1000f - 36f - 8f
        assertTrue("Bottom bubble offset must not clip container", bottomOffset <= maxAllowed)
    }
}
