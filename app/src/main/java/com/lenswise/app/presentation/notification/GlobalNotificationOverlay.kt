package com.lenswise.app.presentation.notification

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全域單一通知覆蓋層元件 (Global Single-Slot Notification Overlay)。
 *
 * 核心保證：
 * 1. 唯一通知呈現：畫面同時至多僅顯示一則通知，徹底廢除舊有多層向下疊加瀑布。
 * 2. 即時平滑覆蓋：當新通知觸發時，透過 AnimatedContent 原地平滑置換內容，零高度跳動。
 * 3. 支援手動關閉與定時自動淡出退場。
 */
@Composable
fun GlobalNotificationOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentNotification by NotificationController.instance.currentNotification.collectAsState()
    val easeOutQuart = remember { CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            AnimatedContent(
                targetState = currentNotification,
                transitionSpec = {
                    if (initialState == null && targetState != null) {
                        // 從無到有：由上方平滑滑入 + 淡入
                        (slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 280, easing = easeOutQuart)
                        ) + fadeIn(animationSpec = tween(280))) togetherWith fadeOut(animationSpec = tween(150))
                    } else if (initialState != null && targetState == null) {
                        // 逾時退場：向上方滑出 + 淡出
                        fadeIn(animationSpec = tween(150)) togetherWith (slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 220, easing = easeOutQuart)
                        ) + fadeOut(animationSpec = tween(220)))
                    } else {
                        // 新通知覆蓋舊通知：舊通知原地快速淡出，新通知滑順淡入替換
                        (slideInVertically(
                            initialOffsetY = { -it / 3 },
                            animationSpec = tween(durationMillis = 240, easing = easeOutQuart)
                        ) + fadeIn(animationSpec = tween(240))) togetherWith (slideOutVertically(
                            targetOffsetY = { -it / 3 },
                            animationSpec = tween(durationMillis = 200, easing = easeOutQuart)
                        ) + fadeOut(animationSpec = tween(200)))
                    }
                },
                contentKey = { it?.id },
                label = "singleNotificationTransition"
            ) { notification ->
                if (notification != null) {
                    NotificationToast(
                        notification = notification,
                        onDismiss = {
                            NotificationController.dismiss()
                        }
                    )
                }
            }
        }
    }
}
