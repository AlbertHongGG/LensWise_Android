package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.presentation.theme.AppColors

/**
 * 商品比對產品圖鑑抽屜 (ComparisonContendersDrawer)。
 * 1:1 對齊 LensWise Flutter 版 comparison_contenders_drawer.dart：
 * - 頂部圖鑑 Icon 與「產品圖鑑」標題列
 * - 幾何高度精準對齊：當候選人 > 2 時，初始視窗高度精確完整露出 2 個選項 (140dp 橫幅卡片)，絕不垂直裁切第二張卡片
 * - 點擊任意卡片可觸發選手對決席位替換。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonContendersDrawer(
    record: ProductComparisonRecord,
    leftIndex: Int,
    rightIndex: Int,
    getImageModel: (Int) -> Any?,
    onProductTapped: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val products = record.products

    // 參賽選手大於 2 個時支援兩段式滑動 (初始露出 2 個選項，上滑展開至 85%)；若只有 2 個選手則自然 wrap content
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = products.size <= 2
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = if (products.size > 2) {
            modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        } else {
            modifier.fillMaxWidth()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // 1. 標題列
            Padding(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ViewCarousel,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "產品圖鑑",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF111418),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 橫幅商品卡片清單 (每張高度 140dp，間距 16dp)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
            ) {
                itemsIndexed(products) { index, product ->
                    val isSelected = index == leftIndex || index == rightIndex
                    val isLeft = index == leftIndex

                    ContenderBannerCard(
                        translatedName = product.translatedName,
                        originalName = product.originalName,
                        imageModel = getImageModel(index),
                        isSelected = isSelected,
                        isLeft = isLeft,
                        onClick = { onProductTapped(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContenderBannerCard(
    translatedName: String,
    originalName: String,
    imageModel: Any?,
    isSelected: Boolean,
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) AppColors.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // 背景大圖
        AsyncImage(
            model = imageModel,
            contentDescription = translatedName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 底部暗黑漸層遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.90f)
                        ),
                        startY = 40f
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(
                    text = translatedName,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (originalName.isNotBlank() && originalName != translatedName) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = originalName,
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 選中對抗箭頭徽章 (右上角 12dp)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.30f)
                    )
                    .clip(CircleShape)
                    .background(AppColors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLeft) {
                        Icons.Default.KeyboardDoubleArrowLeft
                    } else {
                        Icons.Default.KeyboardDoubleArrowRight
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Padding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
