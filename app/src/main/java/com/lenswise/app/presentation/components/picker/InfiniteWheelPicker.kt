package com.lenswise.app.presentation.components.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlinx.coroutines.launch

/**
 * 通用無限循環輪盤選取器 (InfiniteWheelPicker)。
 * 1:1 復刻 LensWise 原版 Flutter StringWheelPicker 規範：
 * - 視窗高度 200dp、項目槽位 50dp，中央 50dp 高度 Selection Pill。
 * - 嚴格 1:1 字型自然長寬比 (Outfit 字體族，無透視矩陣壓縮，文字立體正雅)。
 * - 選中項目：24sp ExtraBold (w800)，高對比深色 (淺色模式 0xFF1A1A1A / 深色模式 White)。
 * - 未選中項目：18sp Medium (w500)，38% 半透明次要文字灰。
 * - 首尾無限循環滾動、精確磁吸 (Snap Fling)、越過中線物理觸覺回饋 (TextHandleMove)。
 */
@Composable
fun <T> InfiniteWheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemText: (T) -> String = { it.toString() }
) {
    if (items.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val itemCount = items.size
    val itemHeight = 50.dp
    val viewportHeight = 200.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // 設定大範圍虛擬長度，並將初始滾動位置置於中央且對齊 initialIndex，實現無限首尾循環
    val virtualMultiplier = 10000
    val virtualCount = itemCount * virtualMultiplier
    val baseIndex = (virtualCount / 2) - ((virtualCount / 2) % itemCount)
    val initialScrollIndex = baseIndex + initialIndex.coerceIn(0, itemCount - 1)

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    // 計算目前最接近視窗中心點的項目虛擬索引
    val centeredVirtualIndex by remember {
        derivedStateOf {
            val firstIndex = lazyListState.firstVisibleItemIndex
            val offsetPx = lazyListState.firstVisibleItemScrollOffset
            if (offsetPx > itemHeightPx / 2f) firstIndex + 1 else firstIndex
        }
    }

    // 當滾動切換選取項目時觸發物理觸覺震動並向外通知
    var lastHapticIndex by remember { mutableIntStateOf(initialScrollIndex) }
    LaunchedEffect(centeredVirtualIndex) {
        if (centeredVirtualIndex != lastHapticIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticIndex = centeredVirtualIndex
            val realIndex = (centeredVirtualIndex % itemCount + itemCount) % itemCount
            onItemSelected(realIndex)
        }
    }

    val selectedTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val unselectedTextColor = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(viewportHeight),
        contentAlignment = Alignment.Center
    ) {
        // 1. 中央選取高亮指示槽 (Selection Pill: 50dp, 16dp 圓角, 1:1 對齊 Flutter 原版)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // 2. 無限滾動輪盤清單 (頂底 Padding 各 75dp，使 itemExtent 50dp 完美置中於 200dp 視窗)
        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(top = 75.dp, bottom = 75.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
        ) {
            items(
                count = virtualCount,
                key = { it }
            ) { virtualIndex ->
                val realIndex = (virtualIndex % itemCount + itemCount) % itemCount
                val item = items[realIndex]
                val isSelected = virtualIndex == centeredVirtualIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(virtualIndex)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemText(item),
                        fontFamily = OutfitFontFamily,
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        letterSpacing = 1.0.sp
                    )
                }
            }
        }
    }
}
