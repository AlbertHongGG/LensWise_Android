package com.lenswise.app.presentation.components.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 通用選擇器抽屜容器 (PickerBottomSheetShell)。
 * 1:1 復刻 LensWise Flutter 原版 PickerBottomSheetShell 規範：
 * - 32dp 頂部大圓角容器，背景色嚴格對齊 AppColors.backgroundLight (0xFFF5F7FA) / backgroundDark (0xFF0F111A)。
 * - 48x5dp 圓角拖曳指示條。
 * - 對稱 Header 佈局（左側 48dp 隱形佔位、中央幾何置中 14sp Bold 大寫追蹤標題、右側 28dp 圓形打勾按鈕）。
 * - 支援上下邊緣硬體加速 Alpha 漸隱著色器 (ShaderMask)，呈現輪盤 3D 滾動淡出淡入質感，中間三行完全清晰。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerBottomSheetShell(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentHeight: Int = 200,
    useShaderMask: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val sheetBackgroundColor = if (isDark) AppColors.backgroundDark else AppColors.backgroundLight
    val handleColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)
    val titleColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val topBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = sheetBackgroundColor,
        dragHandle = null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = topBorderColor,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)
        ) {
            // 頂部拖曳把手 (Drag Handle: 48x5dp, 2.5dp 圓角)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(handleColor)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 對稱 Header 佈局 (左佔位 48dp、中央置中標題、右打勾確認鈕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側佔位，確保標題在視覺與幾何上嚴格置中
                Spacer(modifier = Modifier.width(48.dp))

                // 中央大寫粗體追蹤標題 (採用 Outfit 字體族，13sp Bold，letterSpacing 2.0sp)
                Text(
                    text = title.uppercase(),
                    style = AppTypography.sheetHeader.copy(color = titleColor)
                )

                // 右側確認按鈕 (28dp 圓形勾選圖標)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "確認選取",
                        tint = AppColors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 內容視窗區域 (200dp 高度，上下邊緣 ShaderMask 漸變淡出)
            val shaderModifier = if (useShaderMask) {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.25f to Color.Black,
                                0.75f to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight.dp)
                    .then(shaderModifier),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}
