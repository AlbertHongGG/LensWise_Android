package com.lenswise.app.core.logging

import android.content.Context
import android.util.Log
import com.lenswise.app.domain.model.log.NotificationLogEntry
import com.lenswise.app.domain.model.log.NotificationLogRecord
import com.lenswise.app.presentation.notification.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 系統通知日誌管理器 (NotificationLogManager)。
 * 負責將 App 發布之各項通知（成功、錯誤、警告、資訊）非同步持久化至本地 JSON。
 * 全面採用 kotlinx.serialization 強型別模型 [NotificationLogRecord]，消除舊式 JSONObject。
 */
class NotificationLogManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_notification_logging_enabled"
        private const val PREFS_NAME = "lenswise_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val logDir = File(context.filesDir, "logs/notifications").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logsFlow = MutableStateFlow<List<NotificationLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<NotificationLogEntry>> = _logsFlow.asStateFlow()

    init {
        reload()
    }

    fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_LOGGING_ENABLED, true)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_LOGGING_ENABLED, enabled).apply()
    }

    fun logNotification(type: NotificationType, message: String) {
        if (!isLoggingEnabled()) return

        scope.launch {
            try {
                val now = Date()
                val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
                val hhmmss = SimpleDateFormat("HHmmss", Locale.US).format(now)
                val random = Random.nextInt(100000, 999999)
                val fileName = "${yyyyMMdd}_${hhmmss}_Notification_${random}.json"
                val file = File(logDir, fileName)

                val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(now)
                val record = NotificationLogRecord(
                    timestamp = isoTimestamp,
                    type = type.name.lowercase(Locale.US),
                    message = message
                )

                file.writeText(json.encodeToString(record))
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write notification log: ${e.message}", e)
            }
        }
    }

    fun reload() {
        scope.launch {
            val entries = loadAllEntries()
            _logsFlow.value = entries
        }
    }

    private fun loadAllEntries(): List<NotificationLogEntry> {
        val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                val record = json.decodeFromString<NotificationLogRecord>(file.readText())
                val type = when (record.type.lowercase(Locale.US)) {
                    "success" -> NotificationType.SUCCESS
                    "error" -> NotificationType.ERROR
                    "warning" -> NotificationType.WARNING
                    else -> NotificationType.INFO
                }
                val fileModified = file.lastModified()

                var displayTime = record.timestamp
                if (record.timestamp.isNotEmpty()) {
                    try {
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(record.timestamp)
                        if (parsedDate != null) {
                            displayTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(parsedDate)
                        }
                    } catch (_: Exception) {}
                }

                NotificationLogEntry(
                    id = file.name,
                    file = file,
                    type = type,
                    message = record.message,
                    timestampStr = displayTime,
                    timestampMillis = fileModified
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.file.name }
    }

    suspend fun clearLog(file: File) = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
            }
            _logsFlow.value = _logsFlow.value.filter {
                it.file.absolutePath != file.absolutePath && it.id != file.name
            }
            val entries = loadAllEntries()
            _logsFlow.value = entries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log file: ${e.message}", e)
        }
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        try {
            if (logDir.exists()) {
                logDir.listFiles()?.forEach { f ->
                    try {
                        if (f.isDirectory) f.deleteRecursively() else f.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete ${f.name}: ${e.message}")
                    }
                }
            }
            _logsFlow.value = emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all logs: ${e.message}", e)
            _logsFlow.value = emptyList()
        }
    }
}
