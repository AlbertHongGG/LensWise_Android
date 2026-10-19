package com.lenswise.app.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lenswise.app.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * 手勢隔離型沉浸式層級內底部彈窗容器 (IsolatedHandleBottomSheet)。
 *
 * 架構核心保證：
 * 1. 徹底拔除 Dialog 子視窗建立開銷：消除 SurfaceFlinger 與相機預覽層的合成撕裂，零 Window IPC 成本。
 * 2. GPU 渲染節點直驅 (graphicsLayer)：位移動畫完全跳過 Compose Measure/Layout 排版，提供純淨 120 FPS 滿幀展開與收起。
 * 3. 點擊外部半透明遮罩 (Scrim) 或系統返回鍵 ➔ 觸發平滑退出關閉。
 * 4. 只有頂部 [header] 區域具備拖曳手勢感知 (Draggable)，下拉超過閥值順暢關閉。
 * 5. 內部 [content] 區域完全手勢隔離，照片網格/列表上下滾動完全不與彈窗產生手勢衝突。
 */
@Composable
fun IsolatedHandleBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    heightRatio: Float = 0.88f,
    containerColor: Color = if (isSystemInDarkTheme()) AppColors.surfaceDark else AppColors.surfaceLight,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val sheetHeightDp = (configuration.screenHeightDp * heightRatio).dp
    val sheetHeightPx = with(density) { sheetHeightDp.toPx() }
    val dismissThresholdPx = with(density) { 90.dp.toPx() }

    // 垂直位移動畫控制器 (0f: 完全展開, sheetHeightPx: 完全隱藏)
    val offsetY = remember { Animatable(sheetHeightPx) }
    // 遮罩透明度 (0f -> 0.45f)
    val scrimAlpha = remember { Animatable(0f) }

    // 平滑關閉動畫流程
    val dismissAction: () -> Unit = {
        coroutineScope.launch {
            launch { scrimAlpha.animateTo(0f, tween(180)) }
            offsetY.animateTo(sheetHeightPx, tween(220, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    // 初次進場動畫
    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(0.45f, tween(220)) }
        offsetY.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
    }

    // 支援系統返回鍵
    BackHandler {
        dismissAction()
    }

    // 層級內全螢幕浮層容器 (零 Window/SurfaceFlinger 建立開銷)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 外部半透明遮罩 Scrim (點擊空白處觸發平滑關閉)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissAction
                )
        )

        // 彈窗本體：使用 graphicsLayer 直接在 GPU RenderNode 變換，零 Layout/Measure 消耗
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeightDp)
                .graphicsLayer {
                    translationY = offsetY.value.coerceAtLeast(0f)
                }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = containerColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // 【唯一感應拖拉手勢的區域】：Header 區塊
                val dragModifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
                                offsetY.snapTo(newOffset)
                            }
                        },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            coroutineScope.launch {
                                if (offsetY.value > dismissThresholdPx || velocity > 800f) {
                                    launch { scrimAlpha.animateTo(0f, tween(160)) }
                                    offsetY.animateTo(sheetHeightPx, tween(200, easing = FastOutSlowInEasing))
                                    onDismiss()
                                } else {
                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    )

                Box(modifier = dragModifier) {
                    header()
                }

                // 內容區域：手勢 100% 獨立隔離
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
