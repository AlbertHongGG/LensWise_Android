package com.lenswise.app.features.menu.service

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.data.MenuHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 菜單掃描應用服務 (MenuScannerService)。
 * 專職負責串流結束後的菜單本機實體相片存檔與歷史倉儲落地。
 */
class MenuScannerService(
    private val context: Context,
    private val historyRepository: MenuHistoryRepository
) {

    private val imagesDir = File(context.filesDir, "history/menu_images").apply {
        if (!exists()) mkdirs()
    }

    suspend fun saveCompletedRecord(
        parsedRecord: MenuScannerRecord,
        images: List<AppImageEntity>
    ): MenuScannerRecord = withContext(Dispatchers.IO) {
        val recordId = if (parsedRecord.id.isBlank()) UUID.randomUUID().toString() else parsedRecord.id
        val savedImagePaths = mutableListOf<String>()

        images.forEachIndexed { index, entity ->
            val imageFile = File(imagesDir, "${recordId}_$index.jpg")
            imageFile.writeBytes(entity.byteArray)
            savedImagePaths.add(imageFile.absolutePath)
        }

        val finalRecord = parsedRecord.copy(
            id = recordId,
            timestamp = if (parsedRecord.timestamp > 0) parsedRecord.timestamp else System.currentTimeMillis(),
            imagePaths = savedImagePaths
        )

        historyRepository.save(finalRecord)
        finalRecord
    }

    suspend fun saveRecordOnly(record: MenuScannerRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }
}
