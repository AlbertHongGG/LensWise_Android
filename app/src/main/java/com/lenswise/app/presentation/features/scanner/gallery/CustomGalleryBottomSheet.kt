package com.lenswise.app.presentation.features.scanner.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.MediaRepository
import com.lenswise.app.presentation.components.IsolatedHandleBottomSheet
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 高效能手勢隔離自製相簿選擇器底部彈窗 (CustomGalleryBottomSheet)。
 *
 * 核心保證：
 * 1. 採用 IsolatedHandleBottomSheet：高度硬性鎖定 88%，永遠置底錨定，物理上不可能衝入狀態列。
 * 2. 徹底消除手勢衝突：僅頂部 Header 具備拖曳關閉能力，照片網格區域手勢 100% 隔離，滾動順暢無阻。
 * 3. 視口分頁預載：批次 48 筆載入，提前 24 筆背景預載下一頁。
 * 4. 點陣圖請求優化：使用 remember(uri) 快取 ImageRequest，強制 360x360 降採樣並關閉 crossfade。
 */
@Composable
fun CustomGalleryBottomSheet(
    maxItems: Int = 10,
    onDismiss: () -> Unit,
    onConfirmSelection: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val repository = remember { MediaRepository(context) }

    var allAssets by remember { mutableStateOf<List<MediaAsset>>(emptyList()) }
    var groupedAssets by remember { mutableStateOf<Map<String, List<MediaAsset>>>(emptyMap()) }
    var groupKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    var currentOffset by remember { mutableIntStateOf(0) }
    val pageSize = 48
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val selectedUris = remember { mutableStateListOf<Uri>() }

    // 載入分頁資料函式
    suspend fun loadPage(isInitial: Boolean) {
        if (isInitial) {
            isLoading = true
            currentOffset = 0
        } else {
            isLoadingMore = true
        }

        val assets = repository.getMediaPaged(offset = currentOffset, limit = pageSize)
        if (assets.isEmpty()) {
            hasMore = false
            isLoading = false
            isLoadingMore = false
            return
        }

        val updatedAll = if (isInitial) assets else allAssets + assets
        val (newGrouped, newKeys) = withContext(Dispatchers.Default) {
            val map = linkedMapOf<String, MutableList<MediaAsset>>()
            val keys = mutableListOf<String>()

            for (asset in updatedAll) {
                val list = map.getOrPut(asset.groupKey) {
                    keys.add(asset.groupKey)
                    mutableListOf()
                }
                list.add(asset)
            }
            Pair(map, keys)
        }

        allAssets = updatedAll
        groupedAssets = newGrouped
        groupKeys = newKeys
        currentOffset += assets.size
        hasMore = assets.size == pageSize
        isLoading = false
        isLoadingMore = false
    }

    // 初次載入
    LaunchedEffect(Unit) {
        loadPage(isInitial = true)
    }

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

                // 2. 標題與確認勾選按鈕 (標題絕對置中)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側佔位 48dp
                    Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = "選擇照片",
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
                    )

                    val canConfirm = selectedUris.isNotEmpty()
                    IconButton(
                        onClick = {
                            if (canConfirm) {
                                onConfirmSelection(selectedUris.toList())
                                onDismiss()
                            }
                        },
                        enabled = canConfirm,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "確認選擇",
                            tint = if (canConfirm) AppColors.electricBlue else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.25f)),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) {
        // 內容區域：手勢 100% 隔離，滾動完全不連動彈窗
        if (isLoading && allAssets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.electricBlue,
                    strokeWidth = 2.5.dp
                )
            }
        } else if (allAssets.isEmpty()) {
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

            // 監聽滾動至末端提早 24 筆預載
            val shouldLoadMore by remember {
                derivedStateOf {
                    val total = gridState.layoutInfo.totalItemsCount
                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    total > 0 && lastVisible >= total - 24
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore && hasMore && !isLoadingMore && !isLoading) {
                    loadPage(isInitial = false)
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
                groupKeys.forEach { monthKey ->
                    val assetsInMonth = groupedAssets[monthKey].orEmpty()

                    // 月份標題 Header
                    item(
                        key = "header_$monthKey",
                        span = { GridItemSpan(3) }
                    ) {
                        Text(
                            text = monthKey,
                            fontFamily = OutfitFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1A1A1A),
                            modifier = Modifier.padding(start = 14.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }

                    // 月份內照片單元格
                    items(
                        items = assetsInMonth,
                        key = { it.id }
                    ) { asset ->
                        val isSelected = selectedUris.contains(asset.contentUri)

                        GalleryPhotoThumbnailItem(
                            asset = asset,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) {
                                    selectedUris.remove(asset.contentUri)
                                } else {
                                    if (selectedUris.size < maxItems) {
                                        selectedUris.add(asset.contentUri)
                                    }
                                }
                            }
                        )
                    }
                }

                if (isLoadingMore) {
                    item(
                        key = "loading_indicator_footer",
                        span = { GridItemSpan(3) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.electricBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 獨立高頻重組隔離的相簿縮圖單元格。
 * 1. 使用 remember(asset.contentUri) 杜絕反覆建構 ImageRequest 物件。
 * 2. 嚴格限定 360x360 視口縮圖與關閉 crossfade 動畫，消滅 GPU Alpha 堵塞與卡頓。
 */
@Composable
private fun GalleryPhotoThumbnailItem(
    asset: MediaAsset,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(asset.contentUri) {
        ImageRequest.Builder(context)
            .data(asset.contentUri)
            .size(360, 360)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            // 勾選半透明遮罩與打勾圖示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(width = 3.dp, color = AppColors.electricBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
