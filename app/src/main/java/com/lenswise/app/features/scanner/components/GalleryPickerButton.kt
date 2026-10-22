package com.lenswise.app.features.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 快門左側相簿挑選按鈕。
 * 採 1:1 白透磨砂毛玻璃質感搭配實心圓角相簿圖示。
 */
@Composable
fun GalleryPickerButton(
    isLoading: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = 24.dp
                        noiseFactor = 0.05f
                        backgroundColor = Color.White.copy(alpha = 0.22f)
                    }
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.22f))
                }
            )
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.35f), shape = CircleShape)
            .clickable(
                enabled = !isLocked && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = "相簿挑選",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

