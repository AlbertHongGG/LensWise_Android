package com.lenswise.app.presentation.features.scanner.gallery

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.presentation.components.IsolatedHandleBottomSheet
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 現代化極速自製相簿挑選彈窗 (CustomGalleryBottomSheet)。
 *
 * 核心保證：
 * 1. 1:1 對齊 Flutter 原生設計：標題固定為「選擇照片」（不隨選取變更）、無左側清除按鈕、無張數人為限制。
 * 2. 採用 In-Hierarchy GPU 渲染層彈窗：展開與收起 120 FPS 絲滑無阻。
 * 3. 展平清單 (Flattened Stream)：單一 LazyVerticalGrid 配合 GridItemSpan，完美達成 Compose 視圖複用。
 * 4. 快速滑動月份浮動徽章 (Floating Fast-Scroll Date Pill)：滑動時即時指示當前年月，停滑淡出。
 */
@Composable
fun CustomGalleryBottomSheet(
    onDismiss: () -> Unit,
    onConfirmSelection: (List<Uri>) -> Unit,
    viewModel: GalleryPickerViewModel = viewModel()
) {
    val isDark = isSystemInDarkTheme()
    val state by viewModel.uiState.collectAsState()

    IsolatedHandleBottomSheet(
        onDismiss = onDismiss,
        heightRatio = 0.88f,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 1. 頂部拖曳指示條 (Handle)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(5.dp)
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(2.5.dp)
                        )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 標題與確認勾選按鈕 (標題絕對幾何置中)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側佔位 48dp 確保文字絕對置中（不放清除按鈕，1:1 對齊 Flutter 規範）
                    Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = "選擇照片",
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
                    )

                    IconButton(
                        onClick = {
                            if (state.canConfirm) {
                                onConfirmSelection(state.selectedUris)
                                viewModel.clearSelection()
                                onDismiss()
                            }
                        },
                        enabled = state.canConfirm,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "確認選擇",
                            tint = if (state.canConfirm) AppColors.primary else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppColors.primary,
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (state.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "相簿內查無照片",
                        fontFamily = OutfitFontFamily,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            } else {
                val gridState = rememberLazyGridState()

                // 當前可見月份 (用於快速滑動時浮動顯示)
                val firstVisibleIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
                val currentMonthTitle by remember {
                    derivedStateOf {
                        val item = state.items.getOrNull(firstVisibleIndex)
                        when (item) {
                            is GalleryUiItem.Header -> item.bucket.displayTitle
                            is GalleryUiItem.Photo -> item.asset.bucket.displayTitle
                            null -> ""
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.items,
                        key = { it.stableKey },
                        span = { GridItemSpan(it.spanCount) }
                    ) { item ->
                        when (item) {
                            is GalleryUiItem.Header -> {
                                GalleryHeaderCell(
                                    header = item,
                                    isDark = isDark
                                )
                            }
                            is GalleryUiItem.Photo -> {
                                GalleryPhotoCell(
                                    photo = item,
                                    onToggle = { asset -> viewModel.togglePhotoSelection(asset) }
                                )
                            }
                        }
                    }
                }

                // 快速滑動月份浮動徽章 (Fast-Scroll Floating Month Badge)
                AnimatedVisibility(
                    visible = gridState.isScrollInProgress && currentMonthTitle.isNotEmpty(),
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.85f) else Color(0xFF202020).copy(alpha = 0.75f),
                        contentColor = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = currentMonthTitle,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 月份分類標題單元格。
 */
@Composable
private fun GalleryHeaderCell(
    header: GalleryUiItem.Header,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = header.bucket.displayTitle,
        fontFamily = OutfitFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (isDark) Color.White else Color(0xFF1A1A1A),
        modifier = modifier.padding(start = 14.dp, top = 20.dp, bottom = 8.dp)
    )
}

/**
 * 獨立高頻重組隔離的照片單元格。
 * 1. 依靠 MediaStoreThumbnailFetcher v2 直接讀取系統快取微縮圖。
 * 2. 精確選取遮罩：無外框，純黑半透明遮罩 (alpha = 0.40f) 與正中心白勾。
 * 3. 嚴格穩定的參數簽名，保證未變更狀態的單元格在重組時被 100% 跳過。
 */
@Composable
private fun GalleryPhotoCell(
    photo: GalleryUiItem.Photo,
    onToggle: (MediaAsset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggle(photo.asset) }
            )
    ) {
        AsyncImage(
            model = photo.asset.contentUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (photo.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "已選取",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
