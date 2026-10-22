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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 預設把手條 (Handle Pill)。
 */
@Composable
fun LensWiseSheetHandle(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.22f)
                )
        )
    }
}

/**
 * LensWise 統一可調整高度抽屜 (Type B：Adjustable Drawer)。
 *
 * 核心機制：
 * 1. 唯一自動決定高度：僅在首次開啟時以彈簧動畫展開至 [defaultHeightRatio]。
 * 2. 完全自由停放：使用者手動拖拉放手後，停留在當前高度位置，不再進行任何中間檔位吸附干涉。
 * 3. 唯一的關閉動作：唯有向下拖曳到底部緩衝門檻 ([dismissThreshold]，預設 0.20f) 時，才觸發平滑收合關閉。
 * 4. Header 自適應拖曳區：由 [headerContent] 傳遞 [Modifier]，有 Title 時覆蓋整個 Handle+Title，無 Title 僅覆蓋 Handle。
 * 5. Body 完全解耦：[content] 槽位不附帶任何抽屜拖拉監聽，內部清單、Markdown、圖片輪播自由獨立滾動。
 * 6. 背景雙模式 [hasScrim]：
 *    - true: 全螢幕黑色漸變遮罩，修復頂部 32dp 圓角漏光缺陷，點擊遮罩關閉。
 *    - false: 無遮罩層，上半部背景事件完全穿透，背景（如菜單/收據）可獨立滑動瀏覽。
 */
@Composable
fun LensWiseAdjustableDrawer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    defaultHeightRatio: Float = 0.53f,
    maxHeightRatio: Float = 0.90f,
    dismissThreshold: Float = 0.20f,
    hasScrim: Boolean = true,
    containerColor: Color = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
    headerContent: @Composable (dragModifier: Modifier) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentHeightRatio = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    // 首次開啟：平滑彈簧展開至 defaultHeightRatio
    LaunchedEffect(Unit) {
        currentHeightRatio.animateTo(
            targetValue = defaultHeightRatio,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            currentHeightRatio.animateTo(0f, animationSpec = tween(durationMillis = 200))
            onDismissRequest()
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnimation()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val currentSheetHeight = maxHeight * currentHeightRatio.value.coerceIn(0f, 1f)

        // 1. 全螢幕背景遮罩 (僅在 hasScrim = true 啟用)
        if (hasScrim) {
            val scrimAlpha = (0.50f * (currentHeightRatio.value / maxHeightRatio)).coerceIn(0f, 0.50f)
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

        // 2. 抽屜本體
        val dragModifier = Modifier.pointerInput(totalHeightPx) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmountPx ->
                    change.consume()
                    val deltaRatio = -dragAmountPx / totalHeightPx
                    val nextRatio = (currentHeightRatio.value + deltaRatio).coerceIn(0.08f, maxHeightRatio)
                    coroutineScope.launch {
                        currentHeightRatio.snapTo(nextRatio)
                    }
                },
                onDragEnd = {
                    coroutineScope.launch {
                        if (currentHeightRatio.value < dismissThreshold) {
                            // 向下拖拉到底部緩衝區 -> 觸發平滑收合關閉
                            dismissWithAnimation()
                        } else {
                            // 使用者想停哪就停哪，完全不干涉！停在當前位置！
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentSheetHeight)
                .align(Alignment.BottomCenter)
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
                // 攔截抽屜內部的點擊穿透
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Header 槽位 (傳入拖曳感應 Modifier)
                headerContent(dragModifier)

                // Body 槽位 (權重 1f，內部滾動完全解耦)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content()
                }
            }
        }
    }
}
