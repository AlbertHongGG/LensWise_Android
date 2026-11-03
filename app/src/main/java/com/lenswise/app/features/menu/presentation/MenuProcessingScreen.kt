package com.lenswise.app.features.menu.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.presentation.components.scanner.ScannerProcessingScaffold
import com.lenswise.app.presentation.components.scanner.ScannerProcessingTextConfig

private val MenuProcessingConfig = ScannerProcessingTextConfig(
    connectingTitle = "AI 菜單解析中...",
    connectingSubtitle = "正在為您辨識並翻譯菜單文字\n這可能需要幾秒鐘的時間",
    streamingTitle = "菜單辨識完成，載入中...",
    streamingSubtitle = "即將為您即時展開菜單品項",
    errorTitle = "解析失敗",
    defaultErrorMessage = "無法完成菜單文字辨識與翻譯，請確認網路連線或 API 金鑰設定。",
    backButtonText = "返回重新拍攝"
)

/**
 * 菜單掃描與辨識等待處理畫面 (MenuProcessingScreen)。
 */
@Composable
fun MenuProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: MenuScannerViewModel,
    onTransitionToResult: (MenuScannerRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScannerProcessingScaffold(
        viewModel = viewModel,
        config = MenuProcessingConfig,
        onTransitionToDetail = onTransitionToResult,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
