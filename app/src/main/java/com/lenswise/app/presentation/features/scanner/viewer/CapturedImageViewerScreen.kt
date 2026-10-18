package com.lenswise.app.presentation.features.scanner.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lenswise.app.presentation.features.scanner.ScannerViewModel
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 已捕獲照片全螢幕輪播檢視與刪除管理畫面。
 *
 * 核心保證：
 * 1. 嚴格零上一頁按鈕：自左邊緣向右滑動平滑返回相機畫面。
 * 2. 頂部索引進度標籤與刪除按鈕。
 * 3. 照片全部刪除完畢時自動彈回相機畫面。
 */
@Composable
fun CapturedImageViewerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val images = uiState.capturedImages

    LaunchedEffect(images.isEmpty()) {
        if (images.isEmpty()) {
            onNavigateBack()
        }
    }

    if (images.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        initialPage = (images.size - 1).coerceAtLeast(0),
        pageCount = { images.size }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 全螢幕照片輪播
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val entity = images.getOrNull(pageIndex)
            if (entity != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = entity.file,
                        contentDescription = "照片大圖 ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2. 頂部狀態列（無返回鍵！僅包含中間頁碼指示與右側刪除按鈕）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側佔位保持標題絕對居中（嚴格遵守無上一頁按鈕要求）
            Box(modifier = Modifier.size(44.dp))

            // 中間張數指示器
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontFamily = OutfitFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // 右側單張刪除按鈕
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 22.dp),
                        onClick = {
                            viewModel.removeImage(pagerState.currentPage)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "刪除照片",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
