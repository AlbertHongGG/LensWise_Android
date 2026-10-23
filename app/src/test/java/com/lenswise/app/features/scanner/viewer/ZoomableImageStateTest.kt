package com.lenswise.app.features.scanner.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.lenswise.app.features.scanner.viewer.state.ZoomableImageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoomableImageStateTest {

    private lateinit var state: ZoomableImageState

    @Before
    fun setUp() {
        state = ZoomableImageState()
        // 模擬視口: 寬 1080, 高 900
        // 模擬長收據圖: 寬 1000, 高 3000
        state.updateDimensions(
            newViewportSize = Size(1080f, 900f),
            newImageSize = Size(1000f, 3000f)
        )
    }

    @Test
    fun testBaseScaleAndNaturalHeightForTallReceipt() {
        // 滿寬縮放比: 1080 / 1000 = 1.08
        assertEquals(1.08f, state.baseScale, 0.001f)
        // 自然高度: 3000 * 1.08 = 3240
        assertEquals(3240f, state.naturalHeight, 0.001f)
    }

    @Test
    fun testPanBoundsAtDefaultScale() {
        val (xRange, yRange) = state.getPanBounds(1.0f)

        // 滿寬長圖水平不可移動
        assertEquals(0f, xRange.start, 0.001f)
        assertEquals(0f, xRange.endInclusive, 0.001f)

        // 垂直滑動範圍: 從 0 (頂端店名) 到 -(3240 - 900) = -2340 (底端條碼)
        assertEquals(-2340f, yRange.start, 0.001f)
        assertEquals(0f, yRange.endInclusive, 0.001f)
    }

    @Test
    fun testCalculateDrawRectsAtTop() {
        // 初始狀態 (panX = 0, panY = 0)
        val rects = state.calculateDrawRects()
        assertNotNull(rects)

        // 畫布填滿視口 1080x900
        assertEquals(0, rects!!.dstOffset.x)
        assertEquals(0, rects.dstOffset.y)
        assertEquals(1080, rects.dstSize.width)
        assertEquals(900, rects.dstSize.height)

        // 原圖取樣頂端: 寬 1000, 高 900 / 1.08 = 833
        assertEquals(0, rects.srcOffset.x)
        assertEquals(0, rects.srcOffset.y)
        assertEquals(1000, rects.srcSize.width)
        assertEquals(833, rects.srcSize.height)
    }

    @Test
    fun testCalculateDrawRectsAtBottom() {
        // 使用者向上滑動到底部 (panY = -2340)
        state.onPan(Offset(0f, -2500f)) // 超出部分會被 clamp 在 -2340
        assertEquals(-2340f, state.panY, 0.001f)

        val rects = state.calculateDrawRects()
        assertNotNull(rects)

        // 畫布依然填滿視口 1080x900 (絕對靜止零位移)
        assertEquals(0, rects!!.dstOffset.x)
        assertEquals(0, rects.dstOffset.y)
        assertEquals(1080, rects.dstSize.width)
        assertEquals(900, rects.dstSize.height)

        // 原圖取樣底端: srcOffset.y = 2340 / 1.08 = 2167, 到 3000 (高 833)
        assertEquals(0, rects.srcOffset.x)
        assertEquals(2167, rects.srcOffset.y)
        assertEquals(1000, rects.srcSize.width)
        assertEquals(833, rects.srcSize.height)
    }

    @Test
    fun testDynamicDrawerExpandAutoCentersShortImage() {
        // 模擬 3:4 相機照片: 寬 3000, 高 4000
        // 當抽屜展開至全高: 寬 1080, 高 1800
        state.updateDimensions(
            newViewportSize = Size(1080f, 1800f),
            newImageSize = Size(3000f, 4000f)
        )
        // 滿寬高度: 4000 * (1080 / 3000) = 1440
        assertEquals(1440f, state.naturalHeight, 0.001f)

        // 圖片高度 (1440) < 視口高度 (1800)，控制器必須自動垂直居中！
        // panY = (1800 - 1440) / 2 = +180 (對齊理想圖二)
        assertEquals(180f, state.panY, 0.001f)

        val rects = state.calculateDrawRects()
        assertNotNull(rects)
        assertEquals(180, rects!!.dstOffset.y)
        assertEquals(1440, rects.dstSize.height)
    }

    @Test
    fun testDynamicDrawerShrinkAutoResetsTopAlignment() {
        // 1. 先展開為全高 (處於置中 panY = +180)
        state.updateDimensions(
            newViewportSize = Size(1080f, 1800f),
            newImageSize = Size(3000f, 4000f)
        )
        assertEquals(180f, state.panY, 0.001f)

        // 2. 使用者將抽屜收小至低高度 (寬 1080, 高 600)
        state.updateDimensions(
            newViewportSize = Size(1080f, 600f),
            newImageSize = Size(3000f, 4000f)
        )

        // 圖片高度 (1440) > 視口高度 (600)，控制器必須自動消除上方黑洞，歸正至頂部貼齊 (panY = 0)！
        // 徹底消滅圖三紅 I 黑色空洞，對齊圖四！
        assertEquals(0f, state.panY, 0.001f)

        val rects = state.calculateDrawRects()
        assertNotNull(rects)
        assertEquals(0, rects!!.dstOffset.y)
        assertEquals(600, rects.dstSize.height)
    }

    @Test
    fun testHysteresisOnIsZoomed() {
        // 初始狀態
        assertFalse(state.isZoomed)

        // 輕微縮放至 1.03x (小於 1.05x 臨界遲滯點)，不應觸發 isZoomed
        state.onPinch(1.03f, Offset(540f, 450f))
        assertFalse(state.isZoomed)

        // 明顯放大至 1.10x (> 1.05x)，進入縮放狀態
        state.onPinch(1.10f / 1.03f, Offset(540f, 450f))
        assertTrue(state.isZoomed)

        // 稍微縮小至 1.02f (大於 1.01f)，依然維持縮放狀態 (防止震盪閃爍)
        state.onPinch(1.02f / 1.10f, Offset(540f, 450f))
        assertTrue(state.isZoomed)

        // 縮小至 1.00f (<= 1.01f)，完全退出縮放
        state.onPinch(1.00f / 1.02f, Offset(540f, 450f))
        assertFalse(state.isZoomed)
    }

    @Test
    fun testVerticalPanConsumedHorizontalPassThrough() {
        // 垂直滑動長圖: 應消費事件 (return true)
        val verticalConsumed = state.onPan(Offset(0f, -100f))
        assertTrue(verticalConsumed)
        assertEquals(-100f, state.panY, 0.001f)

        // 水平滑動: 1.0x 狀態下不應消費事件 (return false，交給 Pager)
        val horizontalConsumed = state.onPan(Offset(100f, 0f))
        assertFalse(horizontalConsumed)
    }

    @Test
    fun testReset() {
        state.onPinch(2.0f, Offset(540f, 450f))
        state.onPan(Offset(-100f, -100f))
        assertTrue(state.isZoomed)

        state.reset()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0f, state.panX, 0.001f)
        assertEquals(0f, state.panY, 0.001f)
        assertFalse(state.isZoomed)
    }
}
