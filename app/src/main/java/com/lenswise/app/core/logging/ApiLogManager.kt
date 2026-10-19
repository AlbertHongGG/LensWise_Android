package com.lenswise.app.core.logging

import android.content.Context
import android.util.Log
import com.lenswise.app.domain.model.log.AiLogger
import com.lenswise.app.domain.model.log.ApiLogModel
import com.lenswise.app.domain.model.log.ApiLogRecord
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
 * API / AI 互動日誌管理器 (ApiLogManager)。
 * 實作 [AiLogger] 契約介面，提供強型別寫入、記憶體解耦快讀與檔案清空功能。
 */
class ApiLogManager(private val context: Context) : AiLogger {

    companion object {
        private const val TAG = "ApiLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_api_logging_enabled"
        private const val PREFS_NAME = "lenswise_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val logDir = File(context.filesDir, "logs/api").apply {
        if (!exists()) mkdirs()
    }

    private val logJson = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logsFlow = MutableStateFlow<List<ApiLogModel>>(emptyList())
    val logsFlow: StateFlow<List<ApiLogModel>> = _logsFlow.asStateFlow()

    init {
        reload()
    }

    override fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_LOGGING_ENABLED, true)
    }

    override fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_LOGGING_ENABLED, enabled).apply()
    }

    override fun logInteraction(record: ApiLogRecord) {
        if (!isLoggingEnabled()) return

        scope.launch {
            try {
                val now = Date()
                val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
                val hhmmss = SimpleDateFormat("HHmmss", Locale.US).format(now)
                val random = Random.nextInt(100000, 999999)
                val safeAgentName = record.agentName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val fileName = "${yyyyMMdd}_${hhmmss}_${safeAgentName}_${random}.json"
                val file = File(logDir, fileName)

                val recordToWrite = if (record.timestamp.isEmpty()) {
                    val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).format(now)
                    record.copy(timestamp = isoTimestamp)
                } else {
                    record
                }

                val jsonString = logJson.encodeToString(recordToWrite)
                file.writeText(jsonString)
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write API log: ${e.message}", e)
            }
        }
    }

    fun reload() {
        scope.launch {
            val entries = loadAllEntries()
            _logsFlow.value = entries
        }
    }

    private fun loadAllEntries(): List<ApiLogModel> {
        val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                val fileName = file.name
                val parts = fileName.split("_")
                val rawAgentName = if (parts.size >= 4) parts[2] else null
                var agentType = rawAgentName?.let { com.lenswise.app.features.ai.domain.AiAgentType.fromId(it) }
                var title = agentType?.displayName ?: rawAgentName ?: fileName.removeSuffix(".json")
                var dtMillis = file.lastModified()
                var displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dtMillis))
                var durationMs = 0L

                if (parts.size >= 2) {
                    val dateStr = parts[0]
                    val timeStr = parts[1]
                    if (dateStr.length == 8 && timeStr.length >= 6) {
                        val parsed = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse("${dateStr}_${timeStr.take(6)}")
                        if (parsed != null) {
                            dtMillis = parsed.time
                            displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(parsed)
                        }
                    }
                }

                // 輕量提取 Header，僅讀取開頭 2KB 提取 agentName 與 durationMs，絕不加載 Base64 圖片巨量酬載至記憶體
                runCatching {
                    file.bufferedReader().use { reader ->
                        val buffer = CharArray(2048)
                        val readCount = reader.read(buffer)
                        if (readCount > 0) {
                            val snippet = String(buffer, 0, readCount)
                            val agentMatch = """"agentName"\s*:\s*"([^"]+)"""".toRegex().find(snippet)
                            if (agentMatch != null) {
                                val matchedName = agentMatch.groupValues[1]
                                val matchedType = com.lenswise.app.features.ai.domain.AiAgentType.fromId(matchedName)
                                if (agentType == null) {
                                    agentType = matchedType
                                }
                                title = matchedType?.displayName ?: matchedName
                            }
                            val durationMatch = """"durationMs"\s*:\s*(\d+)""".toRegex().find(snippet)
                            if (durationMatch != null) {
                                durationMs = durationMatch.groupValues[1].toLongOrNull() ?: 0L
                            }
                        }
                    }
                }

                ApiLogModel(
                    id = file.name,
                    file = file,
                    title = title,
                    displayTime = displayTime,
                    durationMs = durationMs,
                    timestamp = dtMillis,
                    agentType = agentType,
                    source = agentType?.displayName ?: "其他"
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.file.name }
    }

    suspend fun clearLog(model: ApiLogModel) = clearLog(model.file)

    suspend fun clearLog(id: String) = withContext(Dispatchers.IO) {
        val target = _logsFlow.value.find { it.id == id }
        if (target != null) {
            clearLog(target.file)
        } else {
            val file = File(logDir, id)
            if (file.exists()) {
                clearLog(file)
            }
        }
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

    suspend fun clearLogsByAgentType(agentType: com.lenswise.app.features.ai.domain.AiAgentType?) = withContext(Dispatchers.IO) {
        if (agentType == null) {
            clearAllLogs()
        } else {
            val targets = _logsFlow.value.filter { it.agentType == agentType }
            targets.forEach { target ->
                try {
                    if (target.file.exists()) {
                        target.file.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete ${target.file.name}: ${e.message}")
                }
            }
            val entries = loadAllEntries()
            _logsFlow.value = entries
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
