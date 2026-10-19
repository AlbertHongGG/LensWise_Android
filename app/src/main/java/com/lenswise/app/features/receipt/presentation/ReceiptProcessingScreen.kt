package com.lenswise.app.features.receipt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 收據萃取等待處理畫面 (ReceiptProcessingScreen)。
 * 遵守 TTFT (Time to First Token) 流轉規範：
 * 進入畫面時展示 [ThinkingOrb] 科技光暈，一旦首個 Token 抵達 (狀態切換至 STREAMING)，
 * 立即無縫觸發 [onTransitionToDetail] 跳轉至詳情頁。
 */
@Composable
fun ReceiptProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: ReceiptScannerViewModel,
    onTransitionToDetail: (ReceiptScannerRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.reset()
        viewModel.analyzeReceipt(images)
    }

    LaunchedEffect(state.status) {
        if ((state.status == ReceiptScannerStatus.STREAMING || state.status == ReceiptScannerStatus.SUCCESS) && !hasNavigated) {
            hasNavigated = true
            onTransitionToDetail(state.receiptRecord)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.backgroundDark)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.status == ReceiptScannerStatus.ERROR && state.errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "解析失敗",
                    color = Color.Red,
                    fontFamily = OutfitFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.errorMessage ?: "解析過程發生錯誤",
                    color = AppColors.textSecondaryDark,
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                ) {
                    Text(text = "返回相機", color = Color.White)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ThinkingOrb(size = 100.dp, isDark = true)
                Spacer(modifier = Modifier.height(32.dp))

                val isStreaming = state.status == ReceiptScannerStatus.STREAMING
                Text(
                    text = if (isStreaming) "收據辨識完成，載入中..." else "AI 收據萃取中...",
                    color = AppColors.primary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isStreaming) {
                        "即將為您即時展開消費明細與稅金"
                    } else {
                        "正在為您辨識品項與計算金額\n這可能需要幾秒鐘的時間"
                    },
                    color = AppColors.textSecondaryDark,
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
