package com.lenswise.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * LensWise 經典膠囊分頁欄元件 (LensWisePillTabBar)。
 * 1:1 絕對復刻 LensWise Flutter 版 unified_history_page.dart 之 TabBar 規範：
 *
 * 核心保證：
 * 1. 固定 3 席次寬度：tabWidth = (availableWidth - 8dp) / 3，在螢幕上永遠剛好呈現 3 個項目。
 * 2. 懸浮白底滑動指示器 (Sliding Pill Indicator)：使用者主動切換以 spring 動態平滑位移。
 * 3. 零閃爍瞬態快拍 (Instant Snap on Mount)：首次進入或頁面返回重建時，以 snapTo 與 scrollTo 瞬時就位，
 *    杜絕任何 0 到 target 的過渡動畫引起的視覺跳動與閃爍。
 * 4. 自動滾動居中 (Auto-Scroll)：切換至邊界外項目時自動滑順滾動至中央視角，邊界處理極致滑順。
 */
@Composable
fun <T> LensWisePillTabBar(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (index: Int) -> Unit,
    itemTitle: (T) -> String,
    itemIcon: (T) -> ImageVector,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.Black.copy(alpha = 0.04f)
            )
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        // 保持一次剛好顯示 3 個選項 (1:1 對齊 Flutter: (screenWidth - 32) / 3)
        val tabWidth = totalWidth / 3f
        val tabHeight = 38.dp
        val safeSelectedIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

        val tabWidthPx = with(density) { tabWidth.toPx() }
        val viewportWidthPx = with(density) { totalWidth.toPx() }
        val targetScrollPx = ((safeSelectedIndex * tabWidthPx) - (viewportWidthPx - tabWidthPx) / 2f).coerceAtLeast(0f)
        val targetOffsetPx = with(density) { (tabWidth * safeSelectedIndex).toPx() }

        // 首幀同步初值注入 (Synchronous Initial Value Injection)：
        // 初值直接為當前 selectedIndex 計算之 offset 與 scroll，首幀繪製即在精準目標位置，
        // 徹底消除從 0f 瞬態跳動造成的白色方塊閃爍！
        val indicatorAnimatable = remember(safeSelectedIndex, tabWidth) { Animatable(targetOffsetPx) }
        val scrollState = rememberScrollState(initial = targetScrollPx.roundToInt())
        var lastSelectedIndex by remember { mutableIntStateOf(safeSelectedIndex) }

        // 唯有使用者手動點擊切換 Tab 時，才啟動 Spring 平滑動畫與滾動
        LaunchedEffect(safeSelectedIndex, tabWidth) {
            if (lastSelectedIndex != safeSelectedIndex) {
                lastSelectedIndex = safeSelectedIndex
                launch {
                    val maxScroll = scrollState.maxValue.toFloat()
                    val clampedScroll = targetScrollPx.coerceIn(0f, maxScroll)
                    scrollState.animateScrollTo(clampedScroll.roundToInt())
                }
                launch {
                    indicatorAnimatable.animateTo(
                        targetValue = targetOffsetPx,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                    )
                }
            } else {
                indicatorAnimatable.snapTo(targetOffsetPx)
            }
        }

        val indicatorOffset = with(density) { indicatorAnimatable.value.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // 1. 懸浮平滑滑動指示器 (Sliding Pill Indicator)
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(tabHeight)
                        .shadow(
                            elevation = if (isDark) 0.dp else 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.15f)
                            else Color.White
                        )
                )
            }

            // 2. Tab 點擊與內容列
            Row {
                items.forEachIndexed { index, item ->
                    val isSelected = safeSelectedIndex == index
                    val tabTextColor = when {
                        isSelected && isDark -> Color.White
                        isSelected -> AppColors.primary
                        isDark -> Color.White.copy(alpha = 0.54f)
                        else -> Color.Black.copy(alpha = 0.54f)
                    }

                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .height(tabHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onItemSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = itemIcon(item),
                                contentDescription = null,
                                tint = tabTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = itemTitle(item),
                                maxLines = 1,
                                style = if (isSelected) {
                                    AppTypography.tabSelected.copy(color = tabTextColor)
                                } else {
                                    AppTypography.tabUnselected.copy(color = tabTextColor)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
