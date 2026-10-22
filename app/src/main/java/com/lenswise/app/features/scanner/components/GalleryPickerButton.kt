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
import com.lenswise.app.presentation.theme.GlassStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * 快門左側相簿挑選按鈕。
 * 1:1 復刻 LensWise 經典亮白擬毛玻璃質感 (Pure White Frosted Glass)：
 * 56x56 圓形、純白 55% 透光遮罩、12dp 零雜訊即時高斯模糊取樣、0.5dp 精緻光澤白邊與柔和向下微陰影 (無黑邊)。
 */
@Composable
fun GalleryPickerButton(
    isLoading: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val style = remember { GlassStyle.galleryButton() }

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(CircleShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = style.blurRadius
                        noiseFactor = style.noiseFactor
                        this.backgroundColor = style.tintColor
                        tints = listOf(HazeTint(style.tintColor))
                    }
                } else {
                    Modifier.background(style.tintColor)
                }
            )
            .border(
                width = style.borderWidth,
                color = style.borderColor,
                shape = CircleShape
            )
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
                color = style.iconColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = "相簿挑選",
                tint = style.iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
