package com.lenswise.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors

/**
 * LensWise 經典膠囊分頁欄元件 (LensWisePillTabBar)。
 * 100% 復刻 LensWise Flutter 版統一膠囊分頁規範：
 * - 圓角 16dp 槽體、4dp 內襯、半透明底槽。
 * - 橫向滑動列（horizontalScroll，間距 4dp）。
 * - 項目：高 38dp、圓角 12dp、圖標 16dp、間距 6dp、標題 14sp。
 * - 選中：淺色模式白底+4dp陰影、深色模式 15% 白透明底；主色或白高亮。
 * - 未選中：透明底、54% 次要文字灰。
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

    Box(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val tabBg = when {
                    isSelected && isDark -> Color.White.copy(alpha = 0.15f)
                    isSelected -> Color.White
                    else -> Color.Transparent
                }
                val tabTextColor = when {
                    isSelected && isDark -> Color.White
                    isSelected -> AppColors.primary
                    isDark -> Color.White.copy(alpha = 0.54f)
                    else -> Color.Black.copy(alpha = 0.54f)
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .shadow(
                            elevation = if (isSelected && !isDark) 4.dp else 0.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable { onItemSelected(index) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = itemIcon(item),
                            contentDescription = null,
                            tint = tabTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = itemTitle(item),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTextColor
                        )
                    }
                }
            }
        }
    }
}
