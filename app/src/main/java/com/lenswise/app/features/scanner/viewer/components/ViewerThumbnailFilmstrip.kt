package com.lenswise.app.features.scanner.viewer.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lenswise.app.core.image.AppImageEntity

/**
 * 底部照片水平滑動膠捲縮圖列 (Filmstrip Carousel)。
 *
 * 核心保證：
 * 1. 空間恆定原則 (Invariant Layout Footprint)：單元格排版槽位硬性鎖定 52.dp × 68.dp，切換照片時相鄰選項 0% 受擠壓，杜絕整列抖動與閃爍。
 * 2. GPU 硬體矩陣縮放：選中項採用 graphicsLayer 進行 GPU 層級微縮放 (1.0f vs 0.88f)，不引發 Compose 重排與測量震盪。
 * 3. 純白高亮邊框與光暈：選中項賦予 2.5dp 純白聚焦外框與微白光陰影，非選中項半透明淡化。
 * 4. 零黑塊瞬間載入：優先讀取記憶體中已預建之 thumbnailBitmap，零 I/O 延遲。
 */
@Composable
fun ViewerThumbnailFilmstrip(
    images: List<AppImageEntity>,
    selectedIndex: Int,
    lazyListState: LazyListState,
    onSelectImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val slotWidth = 52.dp
    val slotHeight = 68.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.65f),
                        Color.Black.copy(alpha = 0.90f),
                        Color.Black,
                        Color.Black
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = images,
                key = { _, entity -> entity.id }
            ) { index, entity ->
                val isSelected = index == selectedIndex

                // 視覺縮放全部在 GPU 圖層進行，不改變排版槽位尺寸
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.88f,
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_scale_$index"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.20f),
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_border_$index"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 2.5.dp else 1.dp,
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_border_width_$index"
                )
                val overlayAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0.0f else 0.40f,
                    animationSpec = tween(durationMillis = 180),
                    label = "thumb_overlay_$index"
                )

                val shape = RoundedCornerShape(10.dp)

                // 外層容器：排版尺寸恆定不變，相鄰項目永遠不會被擠壓
                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .height(slotHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectImage(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 內層視覺元件：GPU 矩陣縮放與選中外框
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(
                                elevation = if (isSelected) 6.dp else 0.dp,
                                shape = shape,
                                ambientColor = Color.White.copy(alpha = 0.35f),
                                spotColor = Color.White.copy(alpha = 0.35f)
                            )
                            .clip(shape)
                            .border(
                                width = borderWidth,
                                color = borderColor,
                                shape = shape
                            )
                    ) {
                        // 優先取用記憶體預建縮圖，消除黑塊閃爍
                        AsyncImage(
                            model = entity.thumbnailBitmap ?: entity.file,
                            contentDescription = "照片縮圖 ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E2130))
                        )

                        // 非選中項目微透明黑遮罩，強化視覺焦點
                        if (overlayAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = overlayAlpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

