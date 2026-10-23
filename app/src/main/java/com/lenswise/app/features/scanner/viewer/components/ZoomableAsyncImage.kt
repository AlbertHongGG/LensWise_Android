package com.lenswise.app.features.scanner.viewer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

/**
 * 轉接至全新純視口硬體繪圖引擎 (LensWiseZoomableImageViewer)
 */
@Composable
fun ZoomableAsyncImage(
    model: File,
    contentDescription: String?,
    isSelected: Boolean,
    onIsZoomedChanged: (Boolean) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    LensWiseZoomableImageViewer(
        model = model,
        contentDescription = contentDescription,
        isSelected = isSelected,
        onIsZoomedChanged = onIsZoomedChanged,
        onToggleControls = onToggleControls,
        modifier = modifier
    )
}
