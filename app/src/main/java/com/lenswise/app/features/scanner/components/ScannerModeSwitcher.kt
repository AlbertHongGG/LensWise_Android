package com.lenswise.app.features.scanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 底部四大模式膠囊切換列 (收據 / 商品 / 比對 / 菜單)。
 *
 * 核心保證：
 * 1. 空間幾何恆定推導：四大模式槽位等寬 (68.dp)，排版期直接推導座標，杜絕任何非同步測量跳動。
 * 2. 初始就位零延遲：進入 App 或畫面返回時 0ms 直接於目標就位，無任何多餘滑動抖動與閃爍。
 * 3. 點選平滑彈簧過渡：僅在使用者主動點選時啟動果凍微彈滑動 (Spring.StiffnessMediumLow)。
 */
@Composable
fun ScannerModeSwitcher(
    currentMode: ScannerMode,
    onSelectMode: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember { ScannerMode.entries }
    val selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0)

    val tabWidth = 68.dp
    val tabHeight = 36.dp
    val tabSpacing = 4.dp
    val containerPadding = 4.dp

    // 依據選中索引精確計算目標 X 座標，排版期即時已知，初次渲染即精確錨定
    val targetOffset = (tabWidth + tabSpacing) * selectedIndex

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "modePillOffset"
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .padding(containerPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 物理彈簧滑動白底膠囊 (Sliding Pill) - 恆定寬高與圓角，平滑滑動
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(tabWidth)
                .height(tabHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        )

        // 2. Tab 標籤列 (等寬槽位，排版 100% 空間恆定)
        Row(
            horizontalArrangement = Arrangement.spacedBy(tabSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEachIndexed { index, mode ->
                val isSelected = mode == currentMode

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                    animationSpec = tween(durationMillis = 200),
                    label = "modeTextColor_$index"
                )

                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(tabHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectMode(mode) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = textColor,
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

