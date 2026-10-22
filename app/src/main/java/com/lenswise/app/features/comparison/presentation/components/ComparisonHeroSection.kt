package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * 商品比對頂部雙雄對決 Hero 展演看板 (ComparisonHeroSection)。
 * 1:1 絕對對齊 LensWise Flutter 版 comparison_hero_section.dart：
 * - 總高固定 220dp，左右邊距 16dp
 * - 左右商品卡片各佔 50% 寬度，中央縫隙精確 4dp
 * - 左側圓角 [topStart=24, bottomStart=24]，右側圓角 [topEnd=24, bottomEnd=24]
 * - 底部貼合自然漸層遮罩 (雙語品名自動單行截斷，譯名 14sp bold + 原名 11sp white70)
 * - 正中心 52dp 頂級暗黑毛玻璃「VS」徽章 (內建獨立 HazeState 16dp 高斯模糊、1.5dp 半透明白框、padding(end = 2dp) 光學居中補償)。
 */
@Composable
fun ComparisonHeroSection(
    leftImageModel: Any?,
    rightImageModel: Any?,
    leftTranslatedName: String,
    leftOriginalName: String,
    rightTranslatedName: String,
    rightOriginalName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 左側商品卡片 (圓角 topStart=24, bottomStart=24, 陰影 blur=16, y=8)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(220.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            ) {
                AsyncImage(
                    model = leftImageModel,
                    contentDescription = leftTranslatedName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                ProductNameOverlay(
                    translatedName = leftTranslatedName,
                    originalName = leftOriginalName,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 2. 右側商品卡片 (圓角 topEnd=24, bottomEnd=24, 陰影 blur=16, y=8)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(220.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            ) {
                AsyncImage(
                    model = rightImageModel,
                    contentDescription = rightTranslatedName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                ProductNameOverlay(
                    translatedName = rightTranslatedName,
                    originalName = rightOriginalName,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        // 3. 正中心 VS 徽章 (52x52 圓形，深黑微光毛玻璃質感，光學偏置補償 2dp)
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Transparent,
                    spotColor = Color.Black.copy(alpha = 0.30f)
                )
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.50f))
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.30f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // 光學偏置補償：向右內縮 2dp (文字視覺微偏左)，消除 V 與 S 不對稱之視覺偏心
            Box(
                modifier = Modifier.padding(end = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 底部品名遮罩：自然貼合品名字串，無硬性寫死高度。
 */
@Composable
private fun ProductNameOverlay(
    translatedName: String,
    originalName: String,
    modifier: Modifier = Modifier
) {
    if (translatedName.isBlank() && originalName.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = translatedName.ifBlank { originalName },
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (originalName.isNotBlank() && originalName != translatedName) {
                Text(
                    text = originalName,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
