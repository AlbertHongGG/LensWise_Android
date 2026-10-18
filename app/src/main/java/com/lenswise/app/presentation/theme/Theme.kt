package com.lenswise.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LensWiseDarkColorScheme = darkColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.surfaceLight,
    primaryContainer = AppColors.primary.copy(alpha = 0.2f),
    onPrimaryContainer = AppColors.textPrimaryDark,

    secondary = AppColors.secondary,
    onSecondary = AppColors.surfaceDark,

    background = AppColors.backgroundDark,
    onBackground = AppColors.textPrimaryDark,

    surface = AppColors.surfaceDark,
    onSurface = AppColors.textPrimaryDark,
    surfaceVariant = AppColors.backgroundDark,
    onSurfaceVariant = AppColors.textSecondaryDark,

    outline = AppColors.borderDark,
    error = AppColors.danger,
    onError = AppColors.surfaceLight
)

private val LensWiseLightColorScheme = lightColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.surfaceLight,
    primaryContainer = AppColors.primary.copy(alpha = 0.1f),
    onPrimaryContainer = AppColors.primary,

    secondary = AppColors.secondary,
    onSecondary = AppColors.surfaceLight,

    background = AppColors.backgroundLight,
    onBackground = AppColors.textPrimaryLight,

    surface = AppColors.surfaceLight,
    onSurface = AppColors.textPrimaryLight,
    surfaceVariant = AppColors.backgroundLight,
    onSurfaceVariant = AppColors.textSecondaryLight,

    outline = AppColors.borderLight,
    error = AppColors.danger,
    onError = AppColors.surfaceLight
)

@Composable
fun LensWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LensWiseDarkColorScheme else LensWiseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LensWiseTypography,
        content = content
    )
}
