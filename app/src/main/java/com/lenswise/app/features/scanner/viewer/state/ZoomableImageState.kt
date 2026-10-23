package com.lenswise.app.features.scanner.viewer.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 視口繪製矩形參數封裝 (DrawRects)
 * @property srcOffset 原始點陣圖的取樣起始座標 (像素)
 * @property srcSize 原始點陣圖的取樣寬高 (像素)
 * @property dstOffset 畫布視口內的繪製起始座標 (像素)
 * @property dstSize 畫布視口內的繪製寬高 (像素)
 */
data class DrawRects(
    val srcOffset: IntOffset,
    val srcSize: IntSize,
    val dstOffset: IntOffset,
    val dstSize: IntSize
)

/**
 * 視口圖片檢視狀態控制器 (ZoomableImageState)
 *
 * 核心架構職責：
 * 1. 動態視口幾何自適應 (Dynamic Viewport Adaptation)：
 *    - 當抽屜展開至全高 (圖二) 時，若圖片高度小於視口高度，自動居中顯示 (panY = (vH - totalH) / 2f)。
 *    - 當抽屜收小 (圖四) 時，若圖片高度大於視口高度，自動校準歸正為頂對齊 (panY <= 0f)，徹底消除上方黑洞 (圖三紅 I 缺陷)。
 * 2. 視口直繪與永不掉幀保證 (Zero Frame-Drop Defense)：
 *    - 在有效視口下 100% 產出精確的 Source Rect 與 Dest Rect，絕不回傳 null 造成黑幀頻閃。
 * 3. 縮放遲滯保護 (Hysteresis)：
 *    - 縮放判定採遲滯區間 (>1.05f 進入，<=1.01f 退出)，防止微幅晃動導致外層 Pager 重組閃爍。
 * 4. 單一流暢手勢動力學：
 *    - 支援單指拖曳長圖、雙指捏合縮放 (1.0x ~ 4.0x)、雙擊放大、自然 Fling 慣性減速。
 */
class ZoomableImageState(
    private val minScale: Float = 1.0f,
    private val maxScale: Float = 4.0f
) {
    var scale by mutableFloatStateOf(1.0f)
        private set

    var panX by mutableFloatStateOf(0f)
        private set

    var panY by mutableFloatStateOf(0f)
        private set

    var viewportSize by mutableStateOf(Size.Zero)
        private set

    var imageSize by mutableStateOf(Size.Zero)
        private set

    var isZoomed by mutableStateOf(false)
        private set

    private var animationJob: Job? = null

    /**
     * 滿寬模式下的基本縮放比 (寬度貼齊視口寬度)
     */
    val baseScale: Float
        get() = if (imageSize.width > 0f && viewportSize.width > 0f) {
            viewportSize.width / imageSize.width
        } else {
            1.0f
        }

    /**
     * 滿寬模式下的自然展現高度
     */
    val naturalHeight: Float
        get() = imageSize.height * baseScale

    /**
     * 計算當前 scale 下虛擬圖片在畫布上的整體寬度與高度
     */
    val virtualWidth: Float
        get() = viewportSize.width * scale

    val virtualHeight: Float
        get() = naturalHeight * scale

    /**
     * 動態視口與原圖尺寸同步校準 (核心動態幾何自適應引擎)
     */
    fun updateDimensions(newViewportSize: Size, newImageSize: Size) {
        val sizeChanged = viewportSize != newViewportSize || imageSize != newImageSize
        viewportSize = newViewportSize
        imageSize = newImageSize

        if (newViewportSize.width <= 0f || newViewportSize.height <= 0f ||
            newImageSize.width <= 0f || newImageSize.height <= 0f
        ) {
            return
        }

        if (sizeChanged) {
            recalibrateOffsets(scale)
        }
    }

    /**
     * 依據當前視口高度與縮放比，即時校準 panX 與 panY
     */
    private fun recalibrateOffsets(currentScale: Float) {
        val vW = viewportSize.width
        val vH = viewportSize.height
        val totalW = vW * currentScale
        val totalH = naturalHeight * currentScale

        // 水平校準
        if (totalW <= vW) {
            panX = (vW - totalW) / 2f
        } else {
            panX = panX.coerceIn(-(totalW - vW), 0f)
        }

        // 垂直校準 (核心解決圖一與圖三)
        if (totalH <= vH) {
            // 圖片比視口短 (例如抽屜展開全開，圖二)：自動垂直置中！
            panY = (vH - totalH) / 2f
        } else {
            // 圖片比視口長 (例如抽屜收小，圖四)：
            val minY = -(totalH - vH)
            val maxY = 0f
            if (panY > 0f) {
                // 原先處於置中態，當抽屜高度收小時，自動歸正至頂部貼齊，消除上方黑洞！
                panY = 0f
            } else {
                panY = panY.coerceIn(minY, maxY)
            }
        }
    }

    /**
     * 計算垂直與水平有效 pan 範圍
     */
    fun getPanBounds(currentScale: Float = scale): Pair<ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>> {
        val vW = viewportSize.width
        val vH = viewportSize.height
        if (vW <= 0f || vH <= 0f || imageSize.width <= 0f || imageSize.height <= 0f) {
            return (0f..0f) to (0f..0f)
        }

        val totalW = vW * currentScale
        val totalH = naturalHeight * currentScale

        val minX: Float
        val maxX: Float
        if (totalW > vW) {
            minX = -(totalW - vW)
            maxX = 0f
        } else {
            minX = (vW - totalW) / 2f
            maxX = minX
        }

        val minY: Float
        val maxY: Float
        if (totalH > vH) {
            minY = -(totalH - vH)
            maxY = 0f
        } else {
            minY = (vH - totalH) / 2f
            maxY = minY
        }

        return (minX..maxX) to (minY..maxY)
    }

    /**
     * 關鍵演算法：純視口直繪映射 (具備 100% 防黑幀保證)
     * 將當前視口、原圖尺寸、縮放與位移投影為精準的 Source Rect 與 Destination Rect
     */
    fun calculateDrawRects(): DrawRects? {
        val vW = viewportSize.width
        val vH = viewportSize.height
        val rawW = imageSize.width
        val rawH = imageSize.height

        if (vW <= 0f || vH <= 0f || rawW <= 0f || rawH <= 0f) {
            return null
        }

        val totalW = vW * scale
        val totalH = naturalHeight * scale

        // 畫布視口可視交集
        val dstLeft = max(0f, panX)
        val dstTop = max(0f, panY)
        val dstRight = min(vW, panX + totalW)
        val dstBottom = min(vH, panY + totalH)

        if (dstRight <= dstLeft || dstBottom <= dstTop) {
            // 防禦處理：若因浮點極限誤差出現負尺寸，退回至置中安全直繪，絕不掉幀、絕不偏頂
            val fallbackW = min(vW, totalW)
            val fallbackH = min(vH, totalH)
            val fallbackLeft = if (totalW < vW) (vW - totalW) / 2f else 0f
            val fallbackTop = if (totalH < vH) (vH - totalH) / 2f else 0f
            return DrawRects(
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(rawW.roundToInt(), rawH.roundToInt()),
                dstOffset = IntOffset(fallbackLeft.roundToInt(), fallbackTop.roundToInt()),
                dstSize = IntSize(fallbackW.roundToInt().coerceAtLeast(1), fallbackH.roundToInt().coerceAtLeast(1))
            )
        }

        // 像素比例 k = 虛擬尺寸 / 原始尺寸
        val k = (vW * scale) / rawW

        val srcLeft = ((dstLeft - panX) / k).coerceIn(0f, rawW)
        val srcTop = ((dstTop - panY) / k).coerceIn(0f, rawH)
        val srcRight = ((dstRight - panX) / k).coerceIn(0f, rawW)
        val srcBottom = ((dstBottom - panY) / k).coerceIn(0f, rawH)

        val srcW = (srcRight - srcLeft).roundToInt().coerceAtLeast(1)
        val srcH = (srcBottom - srcTop).roundToInt().coerceAtLeast(1)
        val dstW = (dstRight - dstLeft).roundToInt().coerceAtLeast(1)
        val dstH = (dstBottom - dstTop).roundToInt().coerceAtLeast(1)

        return DrawRects(
            srcOffset = IntOffset(srcLeft.roundToInt(), srcTop.roundToInt()),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
            dstSize = IntSize(dstW, dstH)
        )
    }

    /**
     * 更新縮放狀態與遲滯判定
     */
    private fun updateScaleWithHysteresis(newScale: Float) {
        scale = newScale
        if (!isZoomed && newScale > 1.05f) {
            isZoomed = true
        } else if (isZoomed && newScale <= 1.01f) {
            isZoomed = false
        }
    }

    /**
     * 單指拖曳滑動
     * @return Boolean 是否已消費該事件 (true = 消耗；false = 留給外層 Pager)
     */
    fun onPan(delta: Offset): Boolean {
        animationJob?.cancel()
        val (xRange, yRange) = getPanBounds()

        if (isZoomed) {
            // 縮放狀態：2D 自由漫遊
            panX = (panX + delta.x).coerceIn(xRange)
            panY = (panY + delta.y).coerceIn(yRange)
            return true
        }

        // 1.0x 滿寬狀態
        val canScrollV = yRange.start != yRange.endInclusive
        if (canScrollV && abs(delta.y) > abs(delta.x)) {
            // 垂直捲動長圖本身
            panY = (panY + delta.y).coerceIn(yRange)
            return true
        }

        // 水平滑動不消費，交給外層 HorizontalPager
        return false
    }

    /**
     * 雙指捏合縮放 (Pinch-to-zoom)
     */
    fun onPinch(zoomFactor: Float, focalPoint: Offset) {
        animationJob?.cancel()
        val oldScale = scale
        val targetScale = (scale * zoomFactor).coerceIn(minScale, maxScale)
        if (targetScale == oldScale) return

        // 保持焦點所在的圖片像素位置不變
        val fx = focalPoint.x
        val fy = focalPoint.y

        val newPanX = fx - (fx - panX) * (targetScale / oldScale)
        val newPanY = fy - (fy - panY) * (targetScale / oldScale)

        val (xRange, yRange) = getPanBounds(targetScale)
        updateScaleWithHysteresis(targetScale)
        panX = newPanX.coerceIn(xRange)
        panY = newPanY.coerceIn(yRange)
    }

    /**
     * 雙擊快速縮放 (1.0x <-> 2.4x)
     */
    fun onDoubleTap(tapPoint: Offset, coroutineScope: CoroutineScope) {
        animationJob?.cancel()
        coroutineScope.launch {
            if (isZoomed) {
                // 重設回 1.0x，自適應校正
                val (xRange, yRange) = getPanBounds(1.0f)
                val targetPanY = if (yRange.start == yRange.endInclusive) yRange.start else 0f
                animateTo(targetScale = 1.0f, targetPanX = 0f, targetPanY = targetPanY)
            } else {
                val targetScale = 2.4f
                val (xRange, yRange) = getPanBounds(targetScale)
                val targetPanX = (tapPoint.x - (tapPoint.x - panX) * (targetScale / scale)).coerceIn(xRange)
                val targetPanY = (tapPoint.y - (tapPoint.y - panY) * (targetScale / scale)).coerceIn(yRange)
                animateTo(targetScale, targetPanX, targetPanY)
            }
        }.also { animationJob = it }
    }

    /**
     * 慣性拋擲 (Fling Decay)
     */
    fun onFling(velocity: Velocity, coroutineScope: CoroutineScope) {
        animationJob?.cancel()
        val (xRange, yRange) = getPanBounds()

        animationJob = coroutineScope.launch {
            val decay = exponentialDecay<Float>(frictionMultiplier = 1.2f)

            if (isZoomed) {
                // 2D 慣性
                val animX = Animatable(panX)
                val animY = Animatable(panY)
                if (xRange.start != xRange.endInclusive && abs(velocity.x) > 200f) {
                    launch {
                        animX.animateDecay(velocity.x, decay) {
                            panX = value.coerceIn(xRange)
                        }
                    }
                }
                if (yRange.start != yRange.endInclusive && abs(velocity.y) > 200f) {
                    launch {
                        animY.animateDecay(velocity.y, decay) {
                            panY = value.coerceIn(yRange)
                        }
                    }
                }
            } else {
                // 1.0x 僅垂直慣性捲動長圖
                if (yRange.start != yRange.endInclusive && abs(velocity.y) > 200f) {
                    val animY = Animatable(panY)
                    animY.animateDecay(velocity.y, decay) {
                        panY = value.coerceIn(yRange)
                    }
                }
            }
        }
    }

    /**
     * 平滑動畫過渡至目標縮放與位移
     */
    suspend fun animateTo(targetScale: Float, targetPanX: Float, targetPanY: Float) {
        val startScale = scale
        val startPanX = panX
        val startPanY = panY

        val progress = Animatable(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) {
            val p = value
            val currentS = startScale + (targetScale - startScale) * p
            updateScaleWithHysteresis(currentS)
            panX = startPanX + (targetPanX - startPanX) * p
            panY = startPanY + (targetPanY - startPanY) * p
        }
    }

    /**
     * 重設為初始狀態
     */
    fun reset() {
        animationJob?.cancel()
        updateScaleWithHysteresis(1.0f)
        val vW = viewportSize.width
        val vH = viewportSize.height
        val totalH = naturalHeight
        panX = if (vW >= virtualWidth) (vW - virtualWidth) / 2f else 0f
        panY = if (totalH <= vH) (vH - totalH) / 2f else 0f
    }
}

/**
 * 記憶並維護 ZoomableImageState 的 Composable Helper
 */
@Composable
fun rememberZoomableImageState(
    minScale: Float = 1.0f,
    maxScale: Float = 4.0f
): ZoomableImageState {
    return remember { ZoomableImageState(minScale, maxScale) }
}
