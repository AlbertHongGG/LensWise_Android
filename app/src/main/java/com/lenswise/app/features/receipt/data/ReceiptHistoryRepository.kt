package com.lenswise.app.features.receipt.data

import android.content.Context
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 收據歷史紀錄本地檔案倉儲 (ReceiptHistoryRepository)。
 * 採用應用私有儲存空間 (files/history/receipts) 進行強隔離持久化，
 * 兼顧零多餘資料庫膨脹與快速讀寫特性。
 */
class ReceiptHistoryRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val receiptsDir = File(context.filesDir, "history/receipts").apply {
        if (!exists()) mkdirs()
    }

    private val _receiptsFlow = MutableStateFlow<List<ReceiptScannerRecord>>(emptyList())
    val receiptsFlow: StateFlow<List<ReceiptScannerRecord>> = _receiptsFlow.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _receiptsFlow.value = loadAll()
    }

    private fun loadAll(): List<ReceiptScannerRecord> {
        val files = receiptsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString<ReceiptScannerRecord>(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun getAll(): List<ReceiptScannerRecord> = withContext(Dispatchers.IO) {
        loadAll()
    }

    suspend fun getById(id: String): ReceiptScannerRecord? = withContext(Dispatchers.IO) {
        val file = File(receiptsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<ReceiptScannerRecord>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun save(record: ReceiptScannerRecord) = withContext(Dispatchers.IO) {
        val file = File(receiptsDir, "${record.id}.json")
        val content = json.encodeToString(record)
        file.writeText(content)
        reload()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val file = File(receiptsDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
        reload()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        receiptsDir.listFiles()?.forEach { it.delete() }
        _receiptsFlow.value = emptyList()
    }
}
