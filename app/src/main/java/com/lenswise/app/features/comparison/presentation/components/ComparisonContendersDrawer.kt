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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.lenswise.app.presentation.components.sheet.LensWiseAdjustableDrawer
import com.lenswise.app.presentation.components.sheet.LensWiseSheetHandle
import com.lenswise.app.presentation.theme.AppColors

/**
 * 商品比對產品圖鑑抽屜 (ComparisonContendersDrawer)。
 * 採用 LensWise 統一可調整抽屜架構 (Type B: LensWiseAdjustableDrawer)：
 * 1. 初始高度 0.53f：剛好完整露出 2 個選項 (140dp 橫幅卡片)，絕不多露第 3 張也不裁切第 2 張。
 * 2. 大面積拖曳感應區：頂部拉桿與「產品圖鑑」標題列整塊皆可拖曳調整高度。
 * 3. 自由停放機制：使用者拖拉到任意高度放手即停留在該位置，絕無多餘檔位吸附干涉。
 * 4. 滾動完全解耦：下方卡片清單獨立滾動，絕不干擾抽屜高度。
 * 5. 唯有向下拖拉至底部緩衝門檻 (< 0.20f) 才觸發平滑收合關閉。
 */
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

    LensWiseAdjustableDrawer(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        defaultHeightRatio = 0.53f,
        maxHeightRatio = 0.90f,
        hasScrim = true,
        headerContent = { dragModifier ->
            // 將頂部把手與標題整塊作為拖曳區，大面積操作極度順暢
            Column(
                modifier = dragModifier.fillMaxWidth()
            ) {
                LensWiseSheetHandle(isDark = isDark)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        content = {
            // Body 槽位：橫幅商品卡片清單 (每張高度 140dp，間距 16dp)
            // 獨立滾動，與外層抽屜高度手勢解耦
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)
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
    )
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
