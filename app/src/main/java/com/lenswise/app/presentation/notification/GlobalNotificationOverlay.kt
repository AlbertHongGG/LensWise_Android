package com.lenswise.app.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全域通知覆蓋層元件，掛載於最頂層。
 *
 * 核心保證：
 * 1. 支援多通知堆疊佇列（最多 3 筆），杜絕後發通知覆蓋既有通知。
 * 2. 嚴格基於 key(notification.id) 隔離各項目重組節點，解決位置索引槽位覆寫問題。
 * 3. 整合 expandVertically / shrinkVertically 與 easeOutQuart 貝茲曲線，提供絲滑平移過渡。
 */
@Composable
fun GlobalNotificationOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val notifications by NotificationController.instance.notifications.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                notifications.forEach { notification ->
                    key(notification.id) {
                        NotificationToastItem(
                            notification = notification,
                            onDismiss = {
                                NotificationController.instance.remove(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 獨立帶有進出場生命週期過渡的通知單元。
 */
@Composable
private fun NotificationToastItem(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val easeOutQuart = remember { CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val triggerDismiss: () -> Unit = {
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = easeOutQuart)
        ) + fadeIn(
            animationSpec = tween(300, easing = easeOutQuart)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 300, easing = easeOutQuart)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 220, easing = easeOutQuart)
        ) + fadeOut(
            animationSpec = tween(220)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 220, easing = easeOutQuart)
        )
    ) {
        NotificationToast(
            notification = notification,
            onDismiss = triggerDismiss
        )
    }
}
