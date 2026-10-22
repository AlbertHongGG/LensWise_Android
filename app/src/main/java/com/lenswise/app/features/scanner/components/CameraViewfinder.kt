package com.lenswise.app.features.scanner.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lenswise.app.core.camera.CameraManager

/**
 * 全螢幕相機視口元件。
 * 支援雙指平滑縮放 (Pinch-to-Zoom)、單擊對焦 (Tap-to-Focus) 與動態聚焦環。
 */
@Composable
fun CameraViewfinder(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var focusTarget by remember { mutableStateOf<Offset?>(null) }
    var currentLinearZoom by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(lifecycleOwner, previewView) {
        cameraManager.bindToLifecycle(lifecycleOwner, previewView)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomFactor, _ ->
                    val step = (zoomFactor - 1.0f) * 0.75f
                    currentLinearZoom = (currentLinearZoom + step).coerceIn(0f, 1f)
                    cameraManager.setLinearZoom(currentLinearZoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    focusTarget = offset
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(offset.x, offset.y)
                    cameraManager.focusOnPoint(point)
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        FocusRingOverlay(
            targetPoint = focusTarget,
            onAnimationEnd = { focusTarget = null },
            modifier = Modifier.fillMaxSize()
        )
    }
}

