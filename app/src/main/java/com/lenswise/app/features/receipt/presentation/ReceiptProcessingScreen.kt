package com.lenswise.app.features.receipt.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.presentation.components.scanner.ScannerProcessingScaffold
import com.lenswise.app.presentation.components.scanner.ScannerProcessingTextConfig

private val ReceiptProcessingConfig = ScannerProcessingTextConfig(
    connectingTitle = "AI 收據萃取中...",
    connectingSubtitle = "正在為您辨識品項與計算金額\n這可能需要幾秒鐘的時間",
    streamingTitle = "收據辨識完成，載入中...",
    streamingSubtitle = "即將為您即時展開消費明細與稅金",
    errorTitle = "解析失敗",
    defaultErrorMessage = "解析過程發生錯誤",
    backButtonText = "返回相機"
)

/**
 * 收據萃取等待處理畫面 (ReceiptProcessingScreen)。
 */
@Composable
fun ReceiptProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: ReceiptScannerViewModel,
    onTransitionToDetail: (ReceiptScannerRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerProcessingScaffold(
        viewModel = viewModel,
        config = ReceiptProcessingConfig,
        onTransitionToDetail = onTransitionToDetail,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
