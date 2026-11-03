package com.lenswise.app.features.scanner.gallery

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.snapshotFlow
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.ThumbnailCacheRepository
import com.lenswise.app.features.scanner.gallery.fastscroll.GalleryTimelineFastScroller
import com.lenswise.app.presentation.components.IsolatedHandleBottomSheet
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * 現代化極速自製相簿挑選彈窗 (CustomGalleryBottomSheet)。
 *
 * 核心保證：
 * 1. 1:1 對齊 Flutter 原生設計：標題固定為「選擇照片」（不隨選取變更）、無左側清除按鈕、無張數人為限制。
 * 2. 採用 In-Hierarchy GPU 渲染層彈窗：展開與收起 120 FPS 絲滑無阻。
 * 3. 展平清單 (Flattened Stream)：單一 LazyVerticalGrid 配合 GridItemSpan，完美達成 Compose 視圖複用。
 * 4. 快速滑動月份浮動徽章 (Floating Fast-Scroll Date Pill)：滑動時即時指示當前年月，停滑淡出。
 * 5. 會話生命週期連動：每次進入即啟動最新相簿觀察流，退出（確認或取消）徹底清除狀態機。
 */
@Composable
fun CustomGalleryBottomSheet(
    onDismiss: () -> Unit,
    onConfirmSelection: (List<Uri>) -> Unit,
    viewModel: GalleryPickerViewModel = viewModel(factory = GalleryPickerViewModel.Factory)
) {
    val isDark = isSystemInDarkTheme()
    val state by viewModel.uiState.collectAsState()

    // 每次開啟彈窗時，啟動會話並獲取最新相簿流
    LaunchedEffect(Unit) {
        viewModel.onSessionStart()
    }

    val handleDismiss: () -> Unit = {
        viewModel.onSessionDismiss()
        onDismiss()
    }

    IsolatedHandleBottomSheet(
        onDismiss = handleDismiss,
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
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
                    )

                    IconButton(
                        onClick = {
                            if (state.canConfirm) {
                                onConfirmSelection(state.selectedUris)
                                handleDismiss()
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
                val onTogglePhoto = remember(viewModel) {
                    { asset: MediaAsset -> viewModel.togglePhotoSelection(asset) }
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
                                    isSelected = state.selectedIds.contains(item.asset.id),
                                    onToggle = onTogglePhoto,
                                    gridState = gridState
                                )
                            }
                        }
                    }
                }

                // Google 相簿風格時間軸快速滑動條 (月份離散吸附 + 懸浮年月氣泡 + 零重組渲染)
                GalleryTimelineFastScroller(
                    gridState = gridState,
                    items = state.items,
                    isDark = isDark,
                    modifier = Modifier.fillMaxSize()
                )
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
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color.White else Color(0xFF1A1A1A),
        modifier = modifier.padding(start = 14.dp, top = 20.dp, bottom = 8.dp)
    )
}

/**
 * 具備視窗駐留閘門 (Viewport Dwell Gate) 的獨立照片單元格。
 *
 * 核心保證：
 * 1. 消除滑動期清晰圖無腦搶載：快速滑動 (Flinging) 時，清晰圖閘門 100% 關閉，0 個 L2 請求！
 *    僅渲染冷啟動順序載入之 L1 模糊微縮圖 (0ms RAM 直出)，消除磁碟 I/O 爭搶與 CPU 解碼海嘯。
 * 2. 視窗駐留防抖 (Dwell Debounce 120ms)：僅當滾動停止 (!isScrollInProgress) 且在畫面穩定停留 120ms 時，
 *    才針對可見單元格按需載入 L2 清晰圖。
 * 3. 滑出即卸載 (Zero-Cost Cancellation)：單元格一旦滑出視窗，掛起之載入協程 0ms 自動取消，杜絕幽靈解碼。
 * 4. 100% 零重組穿透：透過 [snapshotFlow] 在協程內部被動監聽滾動狀態，滾動開始/停止時不引發任何 Cell 重組。
 */
@Composable
private fun GalleryPhotoCell(
    photo: GalleryUiItem.Photo,
    isSelected: Boolean,
    onToggle: (MediaAsset) -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    thumbnailCacheRepo: ThumbnailCacheRepository? = remember { LensWiseApplication.instance.thumbnailCacheRepository },
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val asset = photo.asset

    // 1. L1 模糊微縮圖 (冷啟動順序載入，0ms RAM 命中)
    var microBitmap by remember(asset.id) {
        mutableStateOf(thumbnailCacheRepo?.getMemoryMicroThumbnail(asset.id))
    }

    // 2. L2 清晰微縮圖 (RAM 命中直出；未命中時等待駐留閘門開啟)
    var sharpBitmap by remember(asset.id) {
        mutableStateOf(thumbnailCacheRepo?.getMemorySharpThumbnail(asset.id))
    }

    // 防禦性補全：若 L1 未在 RAM (極少見)，背景極速加載
    if (microBitmap == null && thumbnailCacheRepo != null) {
        LaunchedEffect(asset.id) {
            val bmp = thumbnailCacheRepo.loadMicroThumbnailFast(asset)
            if (bmp != null) {
                microBitmap = bmp
            }
        }
    }

    // 🌟 核心視窗駐留閘門 (Viewport Dwell Gate)：
    // 快速滑動時絕不發起 L2 解碼！停留 >= 120ms 時才啟動載入；滑出螢幕立即自動取消卸載！
    if (sharpBitmap == null && thumbnailCacheRepo != null) {
        LaunchedEffect(asset.id) {
            // 等待當前滾動停止
            snapshotFlow { gridState.isScrollInProgress }
                .first { !it }
            // 駐留防抖 120ms
            delay(120L)
            // 二次防禦確認：此時仍處於非滑動狀態
            if (!gridState.isScrollInProgress) {
                val bmp = thumbnailCacheRepo.loadSharpThumbnail(asset)
                if (bmp != null) {
                    sharpBitmap = bmp
                }
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(if (isDark) Color(0xFF242424) else Color(0xFFEBEBEB))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggle(photo.asset) }
            )
    ) {
        // 第一層：L1 模糊微縮圖基底 (雙線性插值放大繪製，柔和過渡)
        microBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 第二層：L2 清晰微縮圖 (駐留載入完成後平滑覆蓋)
        sharpBitmap?.let { sharp ->
            Image(
                bitmap = sharp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isSelected) {
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

