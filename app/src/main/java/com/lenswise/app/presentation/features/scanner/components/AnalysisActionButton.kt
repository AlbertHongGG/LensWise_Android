package com.lenswise.app.presentation.features.scanner.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 條件滿足時（各模式最低張數）於快門上方動態浮現的發光分析膠囊按鈕。
 * 採 1:1 白透磨砂玻璃微光設計，柔和融合於取景畫面。
 */
@Composable
fun AnalysisActionButton(
    visible: Boolean,
    mode: ScannerMode,
    isAnalyzing: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (mode) {
        ScannerMode.RECEIPT -> Pair(Icons.AutoMirrored.Outlined.ReceiptLong, mode.actionTitle)
        ScannerMode.PRODUCT -> Pair(Icons.Outlined.Search, mode.actionTitle)
        ScannerMode.COMPARISON -> Pair(Icons.Outlined.Compare, mode.actionTitle)
        ScannerMode.MENU -> Pair(Icons.Outlined.RestaurantMenu, mode.actionTitle)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(26.dp)

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = Color(0xFF3B82F6).copy(alpha = 0.35f),
                    spotColor = Color(0xFF3B82F6).copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = shape
                )
                .clickable(
                    enabled = !isLocked && !isAnalyzing,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 70.dp),
                    onClick = onClick
                )
                .padding(horizontal = 26.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = label,
                    color = Color.White,
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
