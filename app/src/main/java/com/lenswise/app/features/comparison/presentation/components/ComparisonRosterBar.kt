package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.GlassStyle

/**
 * 底部商品候選席次切換膠囊 (ComparisonRosterBar)。
 * 1:1 對齊 LensWise Flutter 版 comparison_roster_bar.dart 與圖二：
 * - 40dp 圓角置中懸浮膠囊，整合 Haze 12dp 背景模糊與 1.5dp 白光邊框
 * - 外層 Box 58dp 嚴格無裁切，保留右下角 18dp 白邊角標外凸空間
 * - 內部頭像 54dp 獨立圓形裁切與 3dp 主題選中框
 * - 右下角懸浮 18dp 圓形白邊角標，帶雙向箭頭圖示 (KeyboardDoubleArrowLeft / Right)
 *
 * 架構革新：
 * - 徹底移除髒標記補丁 (backdropDirtyTrigger)，改由視口空間架構驅動渲染。
 * - 徹底移除 hazeChild 後多餘的 .background()，解決雙重著色導致模糊被覆蓋的問題。
 * - 支援傳入視口 scrollState，在滑動時精確觸發繪圖相依更新，達到文字流過時幀級即時毛玻璃動態更新。
 */
@Composable
fun ComparisonRosterBar(
    itemCount: Int,
    leftIndex: Int,
    rightIndex: Int,
    getImageModel: (Int) -> Any?,
    onProductTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    scrollState: ScrollState? = null
) {
    if (itemCount <= 2) return

    val isDark = isSystemInDarkTheme()
    val rosterScrollState = rememberScrollState()
    val style = remember(isDark) { GlassStyle.rosterBar(isDark) }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(40.dp),
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(40.dp))
            .then(
                if (scrollState != null) {
                    Modifier.drawWithContent {
                        scrollState.value
                        drawContent()
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = style.blurRadius
                        noiseFactor = style.noiseFactor
                        this.backgroundColor = style.tintColor
                        tints = listOf(HazeTint(style.tintColor))
                    }
                } else {
                    Modifier.background(style.tintColor)
                }
            )
            .border(
                width = style.borderWidth,
                color = style.borderColor,
                shape = RoundedCornerShape(40.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rosterScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until itemCount) {
                val isSelected = i == leftIndex || i == rightIndex
                val isLeft = i == leftIndex

                // 外層 Box 58dp：嚴格不加 clip，保留右下角角標凸出空間
                Box(
                    modifier = Modifier
                        .padding(end = if (i == itemCount - 1) 0.dp else 12.dp)
                        .size(58.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. 選手大頭照 (54dp 獨立圓形裁切與選中邊框)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) AppColors.primary else (if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)),
                                shape = CircleShape
                            )
                            .clickable {
                                onProductTapped(i)
                            }
                    ) {
                        AsyncImage(
                            model = getImageModel(i),
                            contentDescription = "候選商品 $i",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    // 2. 選中雙向箭頭角標 (18dp，1.5dp 純白外框，不被裁切)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(AppColors.primary)
                                .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLeft) {
                                    Icons.Default.KeyboardDoubleArrowLeft
                                } else {
                                    Icons.Default.KeyboardDoubleArrowRight
                                },
                                contentDescription = if (isLeft) "左側選手" else "右側選手",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
