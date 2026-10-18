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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lenswise.app.presentation.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 手勢隔離型沉浸式底部彈窗容器 (IsolatedHandleBottomSheet)。
 *
 * 架構核心保證：
 * 1. 點擊外部半透明遮罩 (Scrim) 或系統返回鍵 ➔ 觸發平滑退出關閉。
 * 2. 彈窗最大高度強制鎖定為螢幕高度之 [heightRatio] (預設 0.88f)，置底錨定，物理上絕不向上穿透遮蔽狀態列。
 * 3. 只有頂部 [header] 區域具備拖曳手勢感知 (Draggable)，下拉超過閥值順暢關閉。
 * 4. 內部 [content] 區域完全手勢隔離，照片網格/列表上下滾動完全不與彈窗產生手勢衝突，提供極致 120 FPS 絲滑滾動。
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
            launch { scrimAlpha.animateTo(0f, tween(200)) }
            offsetY.animateTo(sheetHeightPx, tween(250, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    // 初次進場動畫
    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(0.45f, tween(250)) }
        offsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    }

    // 支援系統返回鍵
    BackHandler {
        dismissAction()
    }

    Dialog(
        onDismissRequest = dismissAction,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
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

            // 彈窗本體 (位置由 offsetY 驅動)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .offset { IntOffset(0, offsetY.value.roundToInt().coerceAtLeast(0)) }
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
                                        // 下拉超額或具有明顯向下甩動速度：順暢退出並關閉
                                        launch { scrimAlpha.animateTo(0f, tween(180)) }
                                        offsetY.animateTo(sheetHeightPx, tween(220, easing = FastOutSlowInEasing))
                                        onDismiss()
                                    } else {
                                        // 未達關閉門檻：彈回原位
                                        offsetY.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                        )

                    Box(modifier = dragModifier) {
                        header()
                    }

                    // 【完全手勢隔離的內容區塊】：相簿網格上下滑動完全不會連動外層彈窗
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
