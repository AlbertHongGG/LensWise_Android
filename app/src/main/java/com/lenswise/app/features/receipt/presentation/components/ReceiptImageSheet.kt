package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.scanner.viewer.components.ZoomableAsyncImage
import com.lenswise.app.presentation.components.sheet.LensWiseAdjustableDrawer
import com.lenswise.app.presentation.components.sheet.LensWiseSheetHandle
import com.lenswise.app.presentation.theme.OutfitFontFamily
import java.io.File

/**
 * 內嵌式發票/菜單原圖輪播檢視抽屜 (ReceiptImageSheet)。
 * 採用 LensWise 統一可調整抽屜架構 (Type B: LensWiseAdjustableDrawer)：
 * 1. 背景穿透模式 (hasScrim = false)：完全無全螢幕暗黑遮罩，上半部背景收據/菜單內容可完全獨立滑動瀏覽。
 * 2. 滿版全寬展示 (Edge-to-Edge Full Width)：圖片以 ContentScale.FillWidth 填滿寬度。
 * 3. 自由停放機制：首次開啟預設佔 50% 高度 (defaultHeightRatio = 0.50f)，拖曳頂部把手放手即停留在任意位置，絕無多餘檔位吸附干涉。
 * 4. 頂部把手感應區：無 Title，僅頂部 Handle 把手區域監聽高度調整手勢。
 * 5. 唯有向下拖拉至底部緩衝門檻 (< 0.20f) 或按返回鍵才觸發平滑收合關閉。
 */
@Composable
fun ReceiptImageSheet(
    imageFiles: List<File>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    LensWiseAdjustableDrawer(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        defaultHeightRatio = 0.50f,
        maxHeightRatio = 0.95f,
        hasScrim = false, // 啟用穿透模式：上半部背景可獨立滾動
        containerColor = Color.Black,
        headerContent = { dragModifier ->
            // 無 Title，僅頂部 Handle 把手區監聽拖曳
            Box(modifier = dragModifier.fillMaxWidth()) {
                LensWiseSheetHandle(isDark = true)
            }
        },
        content = {
            if (imageFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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

                Box(modifier = Modifier.fillMaxSize()) {
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
                                contentDescription = "照片 $pageIndex",
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
    )
}
