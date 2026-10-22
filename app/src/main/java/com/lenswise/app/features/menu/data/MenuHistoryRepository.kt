package com.lenswise.app.features.menu.data

import android.content.Context
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 菜單掃描歷史紀錄本地檔案倉儲 (MenuHistoryRepository)。
 * 採用私有目錄 (files/history/menus) 進行 JSON 檔案強隔離持久化。
 */
class MenuHistoryRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val menusDir = File(context.filesDir, "history/menus").apply {
        if (!exists()) mkdirs()
    }

    private val _menusFlow = MutableStateFlow<List<MenuScannerRecord>>(emptyList())
    val menusFlow: StateFlow<List<MenuScannerRecord>> = _menusFlow.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _menusFlow.value = loadAll()
    }

    private fun loadAll(): List<MenuScannerRecord> {
        val files = menusDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString<MenuScannerRecord>(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun getAll(): List<MenuScannerRecord> = withContext(Dispatchers.IO) {
        loadAll()
    }

    suspend fun getById(id: String): MenuScannerRecord? = withContext(Dispatchers.IO) {
        val file = File(menusDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<MenuScannerRecord>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun save(record: MenuScannerRecord) = withContext(Dispatchers.IO) {
        val file = File(menusDir, "${record.id}.json")
        val content = json.encodeToString(record)
        file.writeText(content)
        reload()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val file = File(menusDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
        reload()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        menusDir.listFiles()?.forEach { it.delete() }
        _menusFlow.value = emptyList()
    }
}
