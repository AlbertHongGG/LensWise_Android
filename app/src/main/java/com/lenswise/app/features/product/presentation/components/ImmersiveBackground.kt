package com.lenswise.app.features.product.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lenswise.app.core.image.SessionImageResolver

/**
 * 沉浸式商品頂部背景多圖輪播展演組件 (ImmersiveBackground)。
 * 1:1 復刻 LensWise Flutter 版 immersive_background.dart：
 *
 * 核心保證：
 * 1. 雙軌統一抽象 (SessionImageResolver)：以非同步 File 物件驅動，串流開始第 0 毫秒即刻呈現。
 * 2. 多圖水平輪播 (HorizontalPager)：支援多角度拍攝照片左右滑動瀏覽。
 * 3. 膠囊動態分頁指示器 (Capsule Page Indicator)：選中態 24dp 寬圓角膠囊，非選中態 8dp 圓點，具備陰影與柔白微光。
 */
@Composable
fun ImmersiveBackground(
    resolver: SessionImageResolver,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    if (!resolver.hasImages) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE0E0E0))
        )
        return
    }

    val pageCount = resolver.count
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 水平照片輪播分頁
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val imageFile = resolver.resolveFile(page)
            AsyncImage(
                model = imageFile,
                contentDescription = "商品照片 ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. 膠囊分頁指示器 (多於 1 張照片時浮現，精準避開底部內容卡片交界)
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until pageCount) {
                    val isSelected = pagerState.currentPage == i
                    val animatedWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "pageIndicatorWidth"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = animatedWidth, height = 8.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(4.dp),
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.25f)
                            )
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}
