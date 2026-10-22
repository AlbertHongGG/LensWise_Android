package com.lenswise.app.features.comparison.data

import android.content.Context
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 商品比對歷史紀錄本地檔案倉儲 (ComparisonHistoryRepository)。
 * 採用私有目錄 (files/history/comparisons) 進行 JSON 檔案強隔離持久化。
 */
class ComparisonHistoryRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val comparisonsDir = File(context.filesDir, "history/comparisons").apply {
        if (!exists()) mkdirs()
    }

    private val _comparisonsFlow = MutableStateFlow<List<ProductComparisonRecord>>(emptyList())
    val comparisonsFlow: StateFlow<List<ProductComparisonRecord>> = _comparisonsFlow.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _comparisonsFlow.value = loadAll()
    }

    private fun loadAll(): List<ProductComparisonRecord> {
        val files = comparisonsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString<ProductComparisonRecord>(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun getAll(): List<ProductComparisonRecord> = withContext(Dispatchers.IO) {
        loadAll()
    }

    suspend fun getById(id: String): ProductComparisonRecord? = withContext(Dispatchers.IO) {
        val file = File(comparisonsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<ProductComparisonRecord>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun save(record: ProductComparisonRecord) = withContext(Dispatchers.IO) {
        val file = File(comparisonsDir, "${record.id}.json")
        val content = json.encodeToString(record)
        file.writeText(content)
        reload()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val file = File(comparisonsDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
        reload()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        comparisonsDir.listFiles()?.forEach { it.delete() }
        _comparisonsFlow.value = emptyList()
    }
}
