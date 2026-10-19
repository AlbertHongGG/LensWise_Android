package com.lenswise.app.presentation.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全域單一通知控制器 (Single-Slot Notification Controller)。
 *
 * 核心保證：
 * 1. 單一槽位 (Single-Slot)：同一時間螢幕上至多僅呈現一則通知，杜絕多則堆疊累積。
 * 2. 即時搶佔覆蓋 (Instant Overwrite)：新通知發出時，立即取消舊通知倒數並無縫覆蓋。
 * 3. 線程安全與自動清理：倒數結束自動重設為 null，支援手動立即關閉。
 */
class NotificationController {
    companion object {
        val instance: NotificationController by lazy { NotificationController() }

        fun showSuccess(message: String, durationMs: Long = 3500L) {
            instance.showSuccess(message, durationMs)
        }

        fun showError(message: String, durationMs: Long = 4500L) {
            instance.showError(message, durationMs)
        }

        fun showWarning(message: String, durationMs: Long = 3500L) {
            instance.showWarning(message, durationMs)
        }

        fun showInfo(message: String, durationMs: Long = 3500L) {
            instance.showInfo(message, durationMs)
        }

        fun addListener(listener: NotificationListener) {
            instance.addListener(listener)
        }

        fun dismiss() {
            instance.dismiss()
        }
    }

    fun interface NotificationListener {
        fun onNotificationPosted(type: NotificationType, message: String)
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var dismissJob: Job? = null
    private val listeners = mutableListOf<NotificationListener>()

    fun addListener(listener: NotificationListener) {
        listeners.add(listener)
    }

    private val _currentNotification = MutableStateFlow<AppNotification?>(null)
    val currentNotification: StateFlow<AppNotification?> = _currentNotification.asStateFlow()

    private fun postNotification(notification: AppNotification) {
        dismissJob?.cancel()
        _currentNotification.value = notification

        listeners.forEach {
            runCatching { it.onNotificationPosted(notification.type, notification.message) }
        }

        dismissJob = scope.launch {
            delay(notification.durationMs)
            if (_currentNotification.value?.id == notification.id) {
                _currentNotification.value = null
            }
        }
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentNotification.value = null
    }

    fun showSuccess(message: String, durationMs: Long = 3500L) {
        postNotification(
            AppNotification(
                type = NotificationType.SUCCESS,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showError(message: String, durationMs: Long = 4500L) {
        postNotification(
            AppNotification(
                type = NotificationType.ERROR,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showWarning(message: String, durationMs: Long = 3500L) {
        postNotification(
            AppNotification(
                type = NotificationType.WARNING,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showInfo(message: String, durationMs: Long = 3500L) {
        postNotification(
            AppNotification(
                type = NotificationType.INFO,
                message = message,
                durationMs = durationMs
            )
        )
    }
}
