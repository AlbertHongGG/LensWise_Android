package com.lenswise.app.features.receipt.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.scanner.viewer.components.ZoomableAsyncImage
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.launch
import java.io.File

/**
 * 內嵌式發票原圖輪播檢視抽屜 (ReceiptImageSheet)。
 * 1:1 對齊 LensWise Flutter 版 receipt_result_page.dart 之 DraggableScrollableSheet 設計：
 * 1. 無黑幕遮罩 (Zero Modal Scrim)：直接依附於畫面最頂層 Stack，完全不遮蔽上半部收據卡片。
 * 2. 初始高度 50% (initialChildSize: 0.5)：剛開啟時僅佔據畫面下半部，版面不突兀過大，紙卡與相片兩者兼顧。
 * 3. 自由拖曳縮放 (0.50f ~ 0.92f)：向上拖拉手柄平順展開至全螢幕模式，向下拖拉過半或點擊上半部空白處立即收合。
 * 4. 完整保留橫向多圖輪播 (HorizontalPager)、雙指縮放平移與分頁圓點指示。
 */
@Composable
fun ReceiptImageSheet(
    imageFiles: List<File>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 初始高度比例 50%，支援手勢平滑過渡
    val heightRatio = remember { Animatable(0.50f) }
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            heightRatio.animateTo(0f, animationSpec = tween(durationMillis = 220))
            onDismissRequest()
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnimation()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val currentSheetHeight = maxHeight * heightRatio.value

        // 1. 上半部非黑幕點擊穿透區（點擊空白收據卡片處即自然收起抽屜）
        val topEmptySpaceHeight = maxHeight - currentSheetHeight
        if (topEmptySpaceHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topEmptySpaceHeight)
                    .align(Alignment.TopCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        dismissWithAnimation()
                    }
            )
        }

        // 2. 抽屜本體（暗色背景 + 頂部圓角 32dp + 頂部陰影，完全無全螢幕暗黑遮罩）
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
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 拖曳把手區域 (Drag Handle Area)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { deltaPx ->
                                coroutineScope.launch {
                                    val deltaRatio = -deltaPx / totalHeightPx
                                    val newRatio = (heightRatio.value + deltaRatio).coerceIn(0.10f, 0.92f)
                                    heightRatio.snapTo(newRatio)
                                }
                            },
                            onDragStopped = {
                                if (heightRatio.value < 0.35f) {
                                    dismissWithAnimation()
                                } else if (heightRatio.value > 0.70f) {
                                    heightRatio.animateTo(0.92f, spring(stiffness = Spring.StiffnessMediumLow))
                                } else {
                                    heightRatio.animateTo(0.50f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.5.dp))
                    )
                }

                // 內容區塊
                if (imageFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "無收據原始相片",
                                fontFamily = OutfitFontFamily,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { imageFiles.size })

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            val file = imageFiles[pageIndex]
                            var isZoomed by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                ZoomableAsyncImage(
                                    model = file,
                                    contentDescription = "收據照片 $pageIndex",
                                    isSelected = pagerState.currentPage == pageIndex,
                                    onIsZoomedChanged = { isZoomed = it },
                                    onToggleControls = {}
                                )
                            }
                        }

                        // 多張相片指示圓點 (Pagination Dots)
                        if (imageFiles.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(imageFiles.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(8.dp)
                                            .background(
                                                color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.38f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
