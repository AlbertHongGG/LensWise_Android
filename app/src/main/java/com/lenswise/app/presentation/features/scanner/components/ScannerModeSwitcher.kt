package com.lenswise.app.presentation.features.scanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.theme.OutfitFontFamily
import kotlin.math.roundToInt

/**
 * 底部四大模式膠囊切換列 (收據 / 商品 / 比對 / 菜單)。
 *
 * 核心保證：
 * 1. 物理彈簧連續滑動膠囊底座 (Sliding Pill Indicator)，一改舊版原地閃現淡入淡出。
 * 2. 結合 onGloballyPositioned 動態精確量測各項目之水平偏移與寬度，支援任何語系與字型尺寸。
 * 3. 採用 Spring.StiffnessMediumLow 與 DampingRatio 0.82f，提供極致絲滑的果凍微彈滑動過渡。
 */
@Composable
fun ScannerModeSwitcher(
    currentMode: ScannerMode,
    onSelectMode: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember { ScannerMode.entries }
    val selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0)
    val density = LocalDensity.current

    // 紀錄各 Tab 在父容器內的座標 (X) 與寬度 (Width)
    val tabPositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    var tabHeightPx by remember { mutableFloatStateOf(0f) }

    val currentPos = tabPositions[selectedIndex]
    val targetX = currentPos?.first ?: 0f
    val targetWidth = currentPos?.second ?: 0f

    val animatedX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillX"
    )

    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillWidth"
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 物理彈簧滑動白底膠囊 (Sliding Pill)
        if (targetWidth > 0f && tabHeightPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedX.roundToInt(), 0) }
                    .width(with(density) { animatedWidth.toDp() })
                    .height(with(density) { tabHeightPx.toDp() })
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            )
        }

        // 2. Tab 標籤列
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        .onGloballyPositioned { coordinates ->
                            val posInParent = coordinates.positionInParent()
                            tabPositions[index] = Pair(posInParent.x, coordinates.size.width.toFloat())
                            if (tabHeightPx == 0f) {
                                tabHeightPx = coordinates.size.height.toFloat()
                            }
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectMode(mode) }
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = textColor,
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
