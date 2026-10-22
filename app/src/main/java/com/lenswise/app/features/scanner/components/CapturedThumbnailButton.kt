package com.lenswise.app.features.scanner.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 快門右側已捕獲照片縮圖與數量 Badge 按鈕。
 *
 * 核心幾何保證：
 * 1. 數量 Badge 直接以照片圓形主體為座標錨點，嚴格消除 Magic Number 拼湊。
 * 2. Badge 圓心精確壓在照片圓形外框 (Perimeter) 上（剛好壓邊，半嵌半露）。
 * 3. 固定 20dp 正圓，零拉伸變形，白色 1.5dp 精緻外邊框。
 */
@Composable
fun CapturedThumbnailButton(
    imageCount: Int,
    thumbnailBitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = imageCount > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 30.dp),
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 核心縮圖主體與壓邊 Badge 共同錨定容器 (52dp)
                Box(
                    modifier = Modifier.size(52.dp)
                ) {
                    // 1. 照片縮圖圓形 (52dp，白色 2dp 邊框)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(width = 2.dp, color = Color.White, shape = CircleShape)
                    ) {
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap.asImageBitmap(),
                                contentDescription = "照片檢視",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF333333))
                            )
                        }
                    }

                    // 2. 右上角數量 Badge (20dp 正圓，圓心精準壓在 52dp 圓周外邊框上)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppColors.primary)
                            .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (imageCount > 99) "99+" else imageCount.toString(),
                            color = Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}

