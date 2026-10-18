package com.lenswise.app.presentation.features.scanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
 */
@Composable
fun ScannerModeSwitcher(
    currentMode: ScannerMode,
    onSelectMode: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = ScannerMode.entries

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { mode ->
                val isSelected = mode == currentMode

                val targetBgColor = if (isSelected) Color.White else Color.Transparent
                val targetTextColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)

                val bgColor by animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "modeBg"
                )
                val textColor by animateColorAsState(
                    targetValue = targetTextColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "modeText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
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
