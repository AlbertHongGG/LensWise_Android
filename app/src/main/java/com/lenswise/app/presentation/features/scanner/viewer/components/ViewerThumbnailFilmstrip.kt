package com.lenswise.app.presentation.features.scanner.viewer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 底部照片水平滑動膠捲縮圖列 (Filmstrip Carousel)。
 *
 * 核心特性：
 * 1. 支援水平自由滑動瀏覽所有照片，使用者可飛速切換定位。
 * 2. 選中項以 Electric Blue (0xFF3B82F6) 發光框聚焦放大，非選中項半透明淡化。
 * 3. 點擊任一縮圖瞬間連動切換大圖。
 * 4. 序號角標 (01, 02...) 清晰辨識位置。
 */
@Composable
fun ViewerThumbnailFilmstrip(
    images: List<AppImageEntity>,
    selectedIndex: Int,
    lazyListState: LazyListState,
    onSelectImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.95f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(top = 18.dp, bottom = 14.dp)
    ) {
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = images,
                key = { _, entity -> entity.id }
            ) { index, entity ->
                val isSelected = index == selectedIndex

                val cardWidth by animateDpAsState(
                    targetValue = if (isSelected) 54.dp else 44.dp,
                    animationSpec = spring(stiffness = 450f),
                    label = "thumb_width"
                )
                val cardHeight by animateDpAsState(
                    targetValue = if (isSelected) 72.dp else 60.dp,
                    animationSpec = spring(stiffness = 450f),
                    label = "thumb_height"
                )
                val cardAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.55f,
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_alpha"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) AppColors.electricBlue else Color.White.copy(alpha = 0.20f),
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_border"
                )
                val borderWidth = if (isSelected) 2.dp else 1.dp
                val cornerRadius = if (isSelected) 12.dp else 8.dp
                val shape = RoundedCornerShape(cornerRadius)

                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .shadow(
                            elevation = if (isSelected) 10.dp else 0.dp,
                            shape = shape,
                            ambientColor = AppColors.electricBlue,
                            spotColor = AppColors.electricBlue
                        )
                        .clip(shape)
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = shape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectImage(index) }
                        )
                ) {
                    AsyncImage(
                        model = entity.file,
                        contentDescription = "照片縮圖 ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF1E2130))
                    )

                    // 非選中項目遮罩，強化視覺重心對比
                    if (!isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 1f - cardAlpha))
                        )
                    }

                    // 序號膠囊角標
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                            .background(
                                color = if (isSelected) Color.Black.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = String.format("%02d", index + 1),
                            color = if (isSelected) AppColors.electricBlue else Color.White.copy(alpha = 0.85f),
                            fontFamily = OutfitFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
