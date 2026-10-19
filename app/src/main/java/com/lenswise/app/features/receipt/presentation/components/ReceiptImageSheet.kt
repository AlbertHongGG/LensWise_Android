package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.features.scanner.viewer.components.ZoomableAsyncImage
import com.lenswise.app.presentation.theme.OutfitFontFamily
import java.io.File

/**
 * 發票原圖輪播檢視抽屜 (ReceiptImageSheet)。
 * 1:1 對齊 Flutter 的 ReceiptImageCarousel 與 DraggableScrollableSheet：
 * - 沉浸式暗色底襯 (Color.Black) 與圓弧手柄 (RoundedCornerShape 32.dp)。
 * - 支援多圖左右翻頁 (HorizontalPager) 與分頁圓點指示。
 * - 支援雙指捏合縮放 (Pinch-to-zoom) 與細節平移檢視。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptImageSheet(
    imageFiles: List<File>,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Black,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.5.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        if (imageFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
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
                    .fillMaxHeight(0.85f)
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
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
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

                // 多張照片時顯示分頁指示圓點 (Pagination Dots)
                if (imageFiles.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
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
