package com.lenswise.app.presentation.features.scanner.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 高效能自製相簿選擇器底部彈窗 (CustomGalleryBottomSheet)。
 *
 * 核心保證：
 * 1. 1:1 對齊原版 LensWise 介面設計（頂部把手、居中「選擇照片」、右上角 CheckCircle 確認按鈕）。
 * 2. 徹底根除原版 Flutter 下拉載入卡頓：元數據背景一次性讀取 + Coil 視口異步降採樣（60~120 FPS 絲滑滾動）。
 * 3. 支援日期分組、多選勾選遮罩與動態張數上限保護。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGalleryBottomSheet(
    maxItems: Int = 10,
    onDismiss: () -> Unit,
    onConfirmSelection: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var groupedAssets by remember { mutableStateOf<Map<String, List<MediaAsset>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    LaunchedEffect(Unit) {
        val repository = MediaRepository(context)
        groupedAssets = repository.loadAllMediaAssets()
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AppColors.backgroundDark else Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.90f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 頂部拖曳把手
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f))
                )
            }

            // 2. 標題與確認列
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側佔位保持標題絕對居中
                Box(modifier = Modifier.size(48.dp))

                Text(
                    text = "選擇照片",
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.0.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.60f)
                )

                IconButton(
                    enabled = selectedUris.isNotEmpty(),
                    onClick = {
                        onConfirmSelection(selectedUris.toList())
                        onDismiss()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "完成選取",
                        tint = if (selectedUris.isNotEmpty()) AppColors.electricBlue else (if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. 照片網格內容
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = if (isDark) Color.White else AppColors.primary,
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (groupedAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "無可用照片",
                        fontFamily = OutfitFontFamily,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    groupedAssets.forEach { (monthKey, assets) ->
                        // 月份標題 Header
                        item(span = { GridItemSpan(3) }) {
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
                        items(assets, key = { it.id }) { asset ->
                            val isSelected = selectedUris.contains(asset.contentUri)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (isSelected) {
                                            selectedUris.remove(asset.contentUri)
                                        } else {
                                            if (selectedUris.size < maxItems) {
                                                selectedUris.add(asset.contentUri)
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(asset.contentUri)
                                        .size(240, 240)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelected) {
                                    // 勾選半透明遮罩
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
                    }
                }
            }
        }
    }
}
