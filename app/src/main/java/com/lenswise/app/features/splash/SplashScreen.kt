package com.lenswise.app.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.R
import com.lenswise.app.presentation.theme.SplashTypography
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * LensWise 旗艦級經典啟動開屏動畫。
 * 1:1 精確復刻原版品牌動畫：
 * 1. 背景純白極簡 (#FFFFFF)。
 * 2. 140dp Logo 於 0.0~0.5 區間執行淡入與 CubicBezier 縮放 (0.9 -> 1.0)。
 * 3. 品牌字樣於 0.4~0.9 區間自上方 Cinematic Unfold 展開與淡入。
 * 4. 動畫完成後停留 400ms，觸發 onAnimationFinished 平滑 Crossfade 至相機。
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    // 原版 Flutter Curves.easeOutCubic 與 Curves.easeOut 曲線定義
    val easeOutCubic = remember { CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f) }
    val easeOut = remember { FastOutSlowInEasing }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        delay(400)
        onAnimationFinished()
    }

    val t = progress.value

    // 1. Logo 0.0 ~ 0.4 Fade, 0.0 ~ 0.5 Scale (0.9 -> 1.0)
    val logoFade = when {
        t < 0.0f -> 0f
        t > 0.4f -> 1f
        else -> easeOut.transform((t - 0.0f) / 0.4f)
    }

    val logoScale = when {
        t < 0.0f -> 0.9f
        t > 0.5f -> 1f
        else -> 0.9f + (0.1f * easeOutCubic.transform((t - 0.0f) / 0.5f))
    }

    // 2. Cinematic Unfold 文字展開 0.4 ~ 0.9
    val textUnfold = when {
        t < 0.4f -> 0f
        t > 0.9f -> 1f
        else -> easeOutCubic.transform((t - 0.4f) / 0.5f)
    }

    // 3. 文字淡入 0.5 ~ 0.9
    val textFade = when {
        t < 0.5f -> 0f
        t >= 0.9f -> 1f
        else -> easeOut.transform((t - 0.5f) / 0.4f)
    }

    val slideOffsetPx = (-24f * (1f - textUnfold)).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 核心 Logo (140dp x 140dp)
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "LensWise Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale)
                    .alpha(logoFade)
            )

            // 2. 劇院級 Unfold 展開文字區塊
            Box(
                modifier = Modifier
                    .clipToBounds()
                    .alpha(textFade)
                    .offset { IntOffset(0, slideOffsetPx.roundToPx()) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "LensWise",
                        style = SplashTypography.titleStyle
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "AI VISION ASSISTANT",
                        style = SplashTypography.subtitleStyle
                    )
                }
            }
        }
    }
}

