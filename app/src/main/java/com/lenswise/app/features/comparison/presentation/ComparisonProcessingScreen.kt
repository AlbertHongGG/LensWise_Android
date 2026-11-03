package com.lenswise.app.features.comparison.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.presentation.components.scanner.ScannerProcessingScaffold
import com.lenswise.app.presentation.components.scanner.ScannerProcessingTextConfig

private val ComparisonProcessingConfig = ScannerProcessingTextConfig(
    connectingTitle = "AI 多商品比對中...",
    connectingSubtitle = "正在為您深度拆解並比對各商品特徵\n這可能需要幾秒鐘的時間",
    streamingTitle = "商品比對完成，載入中...",
    streamingSubtitle = "即將為您即時展開比對矩陣",
    errorTitle = "比對失敗",
    defaultErrorMessage = "無法完成商品比對分析，請確認網路連線或 API 金鑰設定。",
    backButtonText = "返回重新拍攝"
)

/**
 * 商品比對等待處理畫面 (ComparisonProcessingScreen)。
 */
@Composable
fun ComparisonProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: ProductComparisonViewModel,
    onTransitionToResult: (ProductComparisonRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerProcessingScaffold(
        viewModel = viewModel,
        config = ComparisonProcessingConfig,
        onTransitionToDetail = onTransitionToResult,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
