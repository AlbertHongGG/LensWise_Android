package com.lenswise.app.features.scanner.viewer.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * 支援雙指捏合縮放 (Pinch-to-zoom)、雙擊放大 (Double-tap zoom) 與單擊沉浸切換的高畫質檢視元件。
 *
 * 手勢互斥保證：
 * 1. 處於 1.0x 預設尺寸時，單指滑動手勢絕不攔截消費，完全交還給 HorizontalPager 左右翻頁。
 * 2. 處於 >1.0x 放大狀態時，單指拖曳平移圖片檢視細節，並消耗事件以防誤觸翻頁。
 * 3. 雙指縮放時平滑計算中心點與邊界限制。
 * 4. 切換頁面時自動重設為 1.0x。
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
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 當此頁切換至非選中頁面時，自動重設比例
    LaunchedEffect(isSelected) {
        if (!isSelected && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
            onIsZoomedChanged(false)
        }
    }

    LaunchedEffect(scale) {
        onIsZoomedChanged(scale > 1.02f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(isSelected) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        if (activePointers.isEmpty()) break

                        if (activePointers.size >= 2) {
                            // 雙指捏合手勢
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            val maxOffsetX = ((containerSize.width * (newScale - 1f)) / 2f).coerceAtLeast(0f)
                            val maxOffsetY = ((containerSize.height * (newScale - 1f)) / 2f).coerceAtLeast(0f)

                            scale = newScale
                            offset = if (newScale <= 1.02f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                            event.changes.forEach { it.consume() }
                        } else if (activePointers.size == 1 && scale > 1.02f) {
                            // 單指且處於放大狀態：平移檢視細節
                            val change = activePointers.first()
                            val pan = change.positionChange()

                            val maxOffsetX = ((containerSize.width * (scale - 1f)) / 2f).coerceAtLeast(0f)
                            val maxOffsetY = ((containerSize.height * (scale - 1f)) / 2f).coerceAtLeast(0f)

                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                            change.consume()
                        }
                    }
                }
            }
            .pointerInput(isSelected) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val targetScale = 2.4f
                                val centerX = containerSize.width / 2f
                                val centerY = containerSize.height / 2f
                                val maxOffsetX = ((containerSize.width * (targetScale - 1f)) / 2f).coerceAtLeast(0f)
                                val maxOffsetY = ((containerSize.height * (targetScale - 1f)) / 2f).coerceAtLeast(0f)

                                val targetOffsetX = (centerX - tapOffset.x) * (targetScale - 1f)
                                val targetOffsetY = (centerY - tapOffset.y) * (targetScale - 1f)

                                scale = targetScale
                                offset = Offset(
                                    x = targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                    y = targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

