package com.lenswise.app.presentation.components.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * LensWise 統一固定高度面板 (Type A：Fixed Modal Sheet)。
 *
 * 核心機制：
 * 1. 固定高度展開：展示於固定高度 [heightRatio] (例如 0.88f) 或由內容自適應 ([isWrapContent] = true)。
 * 2. 抓住跟手位移：下拉 Header 區域時，面板即時隨手指向下平移 [dragOffsetY]。
 * 3. 放手二元決策：
 *    - 若位移超過關閉門檻 [dismissDistanceThresholdDp] (預設 110dp)，平滑向下收合並觸發 [onDismissRequest]。
 *    - 若未超過門檻，以平滑彈簧動畫彈回完全展開的固定位置，絕不會停在中間無效尺寸。
 * 4. Header 自適應拖曳區：由 [headerContent] 傳遞 [Modifier]，有 Title 時覆蓋整個 Handle+Title，無 Title 僅覆蓋 Handle。
 * 5. Body 完全解耦：內部清單、聊天訊息、選項自由滾動，絕對不會造成面板意外關閉。
 * 6. 背景雙模式 [hasScrim]：支援遮罩模式與穿透模式。
 */
@Composable
fun LensWiseFixedSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    heightRatio: Float? = null,
    isWrapContent: Boolean = heightRatio == null,
    hasScrim: Boolean = true,
    dismissDistanceThresholdDp: Dp = 110.dp,
    containerColor: Color = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
    headerContent: @Composable (dragModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { dismissDistanceThresholdDp.toPx() }

    val dragOffsetY = remember { Animatable(0f) }
    // 首次開啟進場動畫偏移量 (由螢幕外滑入)
    val enterAnimOffsetY = remember { Animatable(1600f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enterAnimOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            dragOffsetY.animateTo(
                targetValue = 2000f,
                animationSpec = tween(durationMillis = 200)
            )
            onDismissRequest()
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnimation()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalOffset = enterAnimOffsetY.value + dragOffsetY.value
        val progress = (1f - (dragOffsetY.value / 600f)).coerceIn(0f, 1f)

        // 1. 全螢幕背景遮罩
        if (hasScrim) {
            val scrimAlpha = (0.50f * progress).coerceIn(0f, 0.50f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        dismissWithAnimation()
                    }
            )
        }

        // 2. 拖曳手勢 Modifier (僅綁定於 Header)
        val dragModifier = Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmountPx ->
                    change.consume()
                    val nextOffset = (dragOffsetY.value + dragAmountPx).coerceAtLeast(0f)
                    coroutineScope.launch {
                        dragOffsetY.snapTo(nextOffset)
                    }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        if (dragOffsetY.value > thresholdPx) {
                            // 下拉超過門檻 -> 平滑關閉
                            dismissWithAnimation()
                        } else {
                            // 未達門檻 -> 彈簧平滑彈回原位
                            dragOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                }
            )
        }

        // 3. 面板本體
        val sheetModifier = if (heightRatio != null) {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightRatio)
        } else {
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        }

        Box(
            modifier = sheetModifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, totalOffset.roundToInt()) }
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Header 槽位 (傳入拖曳感應 Modifier)
                headerContent(dragModifier)

                // Body 槽位
                if (heightRatio != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        content()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}
