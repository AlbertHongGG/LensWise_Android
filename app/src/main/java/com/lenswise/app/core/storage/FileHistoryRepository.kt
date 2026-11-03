package com.lenswise.app.core.storage

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 泛型本機檔案歷史紀錄倉儲基礎類別 (FileHistoryRepository)。
 * 統一管理各領域歷史紀錄（收據、商品、比對、菜單）的 JSON 序列化、圖片實體儲存、
 * 純淨相對路徑（Relative Path）儲存與刪除連帶清理。
 *
 * 核心保證：
 * 1. 唯一相對路徑：儲存照片時一律記錄相對於 baseDir 的相對路徑 (例如 "history/receipt_images/rec_0.jpg")，
 *    杜絕寫入絕對路徑導致跨手機、跨包名或跨版本還原破圖。
 * 2. 零孤兒檔案：刪除單筆紀錄或 clearAll 時，連帶精確刪除磁碟上對應的所有實體照片。
 * 3. 雙重建構子：支援 Context 與純 File baseDir，支援 100% 純 JVM 單元測試。
 */
abstract class FileHistoryRepository<T : IHistoryRecord>(
    val baseDir: File,
    val subDirName: String,
    val imagesSubDirName: String,
    private val serializer: KSerializer<T>
) {
    constructor(
        context: Context,
        subDirName: String,
        imagesSubDirName: String,
        serializer: KSerializer<T>
    ) : this(context.filesDir, subDirName, imagesSubDirName, serializer)

    protected val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val recordsDir: File = File(baseDir, "history/$subDirName").apply {
        if (!exists()) mkdirs()
    }

    val imagesDir: File = File(baseDir, "history/$imagesSubDirName").apply {
        if (!exists()) mkdirs()
    }

    private val _recordsFlow = MutableStateFlow<List<T>>(emptyList())
    val recordsFlow: StateFlow<List<T>> = _recordsFlow.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _recordsFlow.value = loadAll()
    }

    private fun loadAll(): List<T> {
        val files = recordsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString(serializer, file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun getAll(): List<T> = withContext(Dispatchers.IO) {
        loadAll()
    }

    suspend fun getById(id: String): T? = withContext(Dispatchers.IO) {
        val file = File(recordsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString(serializer, file.readText())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 儲存已規格化的實體照片至本機沙盒，並回傳相對於 baseDir 的純淨相對路徑清單。
     * 路徑格式統一為 "history/{imagesSubDirName}/{recordId}_{index}.jpg"。
     */
    suspend fun saveImages(recordId: String, images: List<AppImageEntity>): List<String> = withContext(Dispatchers.IO) {
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val savedRelativePaths = mutableListOf<String>()

        images.forEachIndexed { index, entity ->
            val fileName = "${recordId}_$index.jpg"
            val imageFile = File(imagesDir, fileName)
            imageFile.writeBytes(entity.byteArray)
            savedRelativePaths.add("history/$imagesSubDirName/$fileName")
        }

        savedRelativePaths
    }

    suspend fun save(record: T) = withContext(Dispatchers.IO) {
        if (!recordsDir.exists()) recordsDir.mkdirs()
        val file = File(recordsDir, "${record.id}.json")
        val content = json.encodeToString(serializer, record)
        file.writeText(content)
        reload()
    }

    /**
     * 刪除指定 ID 之紀錄，並連帶清除該紀錄參照之所有實體照片，杜絕孤兒檔案殘留。
     */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val target = getById(id)
        if (target != null) {
            deleteRecordImages(target)
        }
        val file = File(recordsDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
        reload()
    }

    /**
     * 刪除單筆紀錄對應的照片檔案，子類別可覆寫以支援如 translatedImagePaths 等額外圖片。
     */
    protected open fun deleteRecordImages(record: T) {
        record.imagePaths.forEach { relativeOrAbsolutePath ->
            deleteImageFile(relativeOrAbsolutePath)
        }
    }

    protected fun deleteImageFile(path: String) {
        if (path.isBlank()) return
        val targetFile = File(path).let { file ->
            if (file.isAbsolute) file else File(baseDir, path)
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    /**
     * 清空所有歷史紀錄，並清空對應的圖片目錄。
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        recordsDir.listFiles()?.forEach { it.delete() }
        imagesDir.listFiles()?.forEach { it.delete() }
        _recordsFlow.value = emptyList()
    }
}
