package com.lenswise.app.presentation.features.scanner.components

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
 * 核心保證：
 * 1. 圖片與 Badge 結構徹底解耦，父層絕不全局裁切。
 * 2. 右上角 Badge 固定為 20dp 絕對正圓形，100% 完整顯示，零裁切與零長條變形。
 */
@Composable
fun CapturedThumbnailButton(
    imageCount: Int,
    thumbnailBitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = imageCount > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 32.dp),
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 1. 照片縮圖圓形主體 (54dp，獨立裁切)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap.asImageBitmap(),
                            contentDescription = "照片檢視",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(0xFF333333))
                        )
                    }
                }

                // 2. 右上角獨立懸浮正圓形數量 Badge (固定 20dp，絕無長條變形，絕不被裁切)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary)
                        .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$imageCount",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}
