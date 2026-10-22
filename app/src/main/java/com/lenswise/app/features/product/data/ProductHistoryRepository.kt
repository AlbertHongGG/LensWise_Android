package com.lenswise.app.features.product.data

import android.content.Context
import com.lenswise.app.domain.model.product.ProductLensRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 商品歷史紀錄本地檔案倉儲 (ProductHistoryRepository)。
 * 採用私有目錄 (files/history/products) 進行 JSON 檔案強隔離持久化。
 */
class ProductHistoryRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val productsDir = File(context.filesDir, "history/products").apply {
        if (!exists()) mkdirs()
    }

    private val _productsFlow = MutableStateFlow<List<ProductLensRecord>>(emptyList())
    val productsFlow: StateFlow<List<ProductLensRecord>> = _productsFlow.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _productsFlow.value = loadAll()
    }

    private fun loadAll(): List<ProductLensRecord> {
        val files = productsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString<ProductLensRecord>(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun getAll(): List<ProductLensRecord> = withContext(Dispatchers.IO) {
        loadAll()
    }

    suspend fun getById(id: String): ProductLensRecord? = withContext(Dispatchers.IO) {
        val file = File(productsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<ProductLensRecord>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun save(record: ProductLensRecord) = withContext(Dispatchers.IO) {
        val file = File(productsDir, "${record.id}.json")
        val content = json.encodeToString(record)
        file.writeText(content)
        reload()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val file = File(productsDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
        reload()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        productsDir.listFiles()?.forEach { it.delete() }
        _productsFlow.value = emptyList()
    }
}
