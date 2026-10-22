package com.lenswise.app.features.scanner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.features.scanner.ScannerUiState

/**
 * 主畫面底部綜合控制區塊（分析按鈕、快門三鍵列、模式切換列）。
 */
@Composable
fun ScannerBottomControls(
    uiState: ScannerUiState,
    onTakeShutter: () -> Unit,
    onPickGallery: () -> Unit,
    onOpenViewer: () -> Unit,
    onTriggerAnalysis: () -> Unit,
    onSelectMode: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 條件觸發之發光分析按鈕 (滿足最低張數時浮現)
        AnalysisActionButton(
            visible = uiState.canTriggerAnalysis,
            mode = uiState.currentMode,
            isAnalyzing = uiState.isAnalyzing,
            isLocked = uiState.isLocked,
            onClick = onTriggerAnalysis
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 快門三鍵控制列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryPickerButton(
                isLoading = uiState.isPickingGallery,
                isLocked = uiState.isLocked,
                onClick = onPickGallery,
                hazeState = hazeState
            )

            ShutterButton(
                isCapturing = uiState.isCapturing,
                isLocked = uiState.isLocked,
                onClick = onTakeShutter
            )

            CapturedThumbnailButton(
                imageCount = uiState.imageCount,
                thumbnailBitmap = uiState.latestThumbnail,
                onClick = onOpenViewer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. 底部四大模式切換膠囊
        ScannerModeSwitcher(
            currentMode = uiState.currentMode,
            onSelectMode = onSelectMode
        )
    }
}

