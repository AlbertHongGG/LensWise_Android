package com.lenswise.app.features.receipt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.presentation.components.ThinkingOrb
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 收據萃取等待處理畫面 (ReceiptProcessingScreen)。
 *
 * 1:1 對齊 Flutter 原生設計：
 * 1. 預設全面亮色系背景 (AppColors.backgroundLight: #F5F7FA)。
 * 2. ThinkingOrb 亮色高透光暈渲染。
 * 3. 標題文字採用 AppColors.primary (#475569) 與次級優雅灰黑文字。
 * 4. 錯誤處理畫面 (PremiumErrorView) 亮色對齊。
 */
@Composable
fun ReceiptProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: ReceiptScannerViewModel,
    onTransitionToDetail: (ReceiptScannerRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val state by viewModel.uiState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.status == ReceiptScannerStatus.IDLE) {
            viewModel.reset()
            viewModel.analyzeReceipt(images)
        }
    }

    LaunchedEffect(state.status) {
        if ((state.status == ReceiptScannerStatus.STREAMING || state.status == ReceiptScannerStatus.SUCCESS) && !hasNavigated) {
            hasNavigated = true
            onTransitionToDetail(state.receiptRecord)
        }
    }

    val backgroundColor = if (isDark) AppColors.backgroundDark else AppColors.backgroundLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.status == ReceiptScannerStatus.ERROR && state.errorMessage != null) {
            // 1:1 對齊 Flutter PremiumErrorView (亮色高階質感)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 頂部圓形錯誤圖示容器
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.1f) else Color(0xFFD32F2F).copy(alpha = 0.06f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.3f) else Color(0xFFD32F2F).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "解析失敗",
                        tint = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F),
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "解析失敗",
                    style = AppTypography.receiptStoreTitle.copy(
                        color = if (isDark) Color.White else Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.errorMessage ?: "解析過程發生錯誤",
                    color = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight,
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "返回相機",
                        style = AppTypography.tileTitle.copy(
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        } else {
            // 正在分析中 (ThinkingOrb 亮色模式 + Slate 標題 + 墨灰副標題)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ThinkingOrb(
                    size = 100.dp,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (state.status == ReceiptScannerStatus.STREAMING) "收據辨識完成，載入中..." else "AI 收據萃取中...",
                    style = AppTypography.receiptStoreTitle.copy(
                        color = AppColors.primary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (state.status == ReceiptScannerStatus.STREAMING) {
                        "即將為您即時展開消費明細與稅金"
                    } else {
                        "正在為您辨識品項與計算金額\n這可能需要幾秒鐘的時間"
                    },
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.55f),
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
