package com.lenswise.app.features.product.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.presentation.components.scanner.ScannerProcessingScaffold
import com.lenswise.app.presentation.components.scanner.ScannerProcessingTextConfig

private val ProductProcessingConfig = ScannerProcessingTextConfig(
    connectingTitle = "AI 連線解析中...",
    connectingSubtitle = "正在為您辨識商品包裝特徵\n這可能需要幾秒鐘的時間",
    streamingTitle = "特徵辨識完成，載入中...",
    streamingSubtitle = "即將為您即時展開分析結果",
    errorTitle = "解析失敗",
    defaultErrorMessage = "無法完成商品影像分析，請確認網路連線或 API 金鑰設定。",
    backButtonText = "返回重新拍攝"
)

/**
 * 商品深度分析等待處理畫面 (ProductProcessingScreen)。
 */
@Composable
fun ProductProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: ProductLensViewModel,
    onTransitionToDetail: (ProductLensRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerProcessingScaffold(
        viewModel = viewModel,
        config = ProductProcessingConfig,
        onTransitionToDetail = onTransitionToDetail,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
