package com.lenswise.app.features.receipt.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
 * 2. 滿版全寬展示 (Edge-to-Edge Full Width)：移除兩側內縮，圖片以 ContentScale.FillWidth 填滿寬度。
 * 3. 即時無延遲手勢拖曳：頂部手柄區域支援連續跟手上下拖曳，放手時平滑彈性吸附 (0.92f 全開 / 0.50f 半開 / 下滑收合)。
 * 4. 點擊上半部收據卡片空白處或按系統返回鍵立即滑順收合。
 */
@Composable
fun ReceiptImageSheet(
    imageFiles: List<File>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val animatedHeightRatio = remember { Animatable(0.50f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0.50f) }
    var isDismissing by remember { mutableStateOf(false) }

    val effectiveRatio = if (isDragging) dragRatio else animatedHeightRatio.value

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        coroutineScope.launch {
            animatedHeightRatio.snapTo(dragRatio)
            animatedHeightRatio.animateTo(0f, animationSpec = tween(durationMillis = 200))
            onDismissRequest()
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnimation()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val currentSheetHeight = maxHeight * effectiveRatio.coerceIn(0f, 1f)

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
                // 拖曳把手區域 (Drag Handle Area) - 全寬手勢感應，即時跟手零延遲
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(totalHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragRatio = animatedHeightRatio.value
                                },
                                onVerticalDrag = { change, dragAmountPx ->
                                    change.consume()
                                    val deltaRatio = -dragAmountPx / totalHeightPx
                                    dragRatio = (dragRatio + deltaRatio).coerceIn(0.15f, 0.95f)
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        animatedHeightRatio.snapTo(dragRatio)
                                        isDragging = false
                                        if (dragRatio < 0.35f) {
                                            dismissWithAnimation()
                                        } else if (dragRatio > 0.65f) {
                                            animatedHeightRatio.animateTo(
                                                0.92f,
                                                spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        } else {
                                            animatedHeightRatio.animateTo(
                                                0.50f,
                                                spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            )
                        },
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

                            // 滿版全寬展示：無左右內縮，貼合螢幕左右兩側
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ZoomableAsyncImage(
                                    model = file,
                                    contentDescription = "收據照片 $pageIndex",
                                    contentScale = ContentScale.FillWidth,
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
