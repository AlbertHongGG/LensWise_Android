package com.lenswise.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirmButton(
    title: String = "SLIDE TO WIPE",
    confirmedTitle: String = "RELEASE TO CLEAR",
    activeColor: Color = Color(0xFFEF4444),
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val trackHeight = 56.dp
    val thumbSize = 48.dp
    val thumbPadding = 4.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
    ) {
        val maxDragDistancePx = with(density) {
            (maxWidth - thumbSize - (thumbPadding * 2)).toPx()
        }

        val offsetX = remember { Animatable(0f) }

        val progress by remember {
            derivedStateOf {
                if (maxDragDistancePx > 0) (offsetX.value / maxDragDistancePx).coerceIn(0f, 1f) else 0f
            }
        }

        val isReachedEnd by remember {
            derivedStateOf { progress >= 0.95f }
        }

        // 背景滑動進度條
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (offsetX.value + thumbSize.toPx() + thumbPadding.toPx()).toDp() })
                .background(activeColor.copy(alpha = 0.25f + progress * 0.75f))
        )

        // 中央提示文字
        Text(
            text = if (isReachedEnd) confirmedTitle else title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.0.sp,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Center)
        )

        // 圓形推鈕 (Thumb)
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbPadding.roundToPx() + offsetX.value.roundToInt(), thumbPadding.roundToPx()) }
                .size(thumbSize)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = if (isReachedEnd) activeColor else Color.Black.copy(alpha = 0.25f)
                )
                .clip(CircleShape)
                .background(if (isReachedEnd) activeColor else Color.White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val target = (offsetX.value + delta).coerceIn(0f, maxDragDistancePx)
                            offsetX.snapTo(target)
                        }
                    },
                    onDragStopped = {
                        if (isReachedEnd) {
                            onConfirmed()
                        }
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = spring())
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReachedEnd) Icons.Default.DeleteForever else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isReachedEnd) Color.White else Color(0xFF1C1C1E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
