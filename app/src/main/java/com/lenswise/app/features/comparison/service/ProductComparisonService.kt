package com.lenswise.app.features.comparison.service

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.data.ComparisonHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 商品比對應用服務 (ProductComparisonService)。
 * 專職負責串流結束後的多商品本機實體相片存檔與歷史倉儲落地。
 */
class ProductComparisonService(
    private val context: Context,
    private val historyRepository: ComparisonHistoryRepository
) {

    private val imagesDir = File(context.filesDir, "history/comparison_images").apply {
        if (!exists()) mkdirs()
    }

    suspend fun saveCompletedRecord(
        parsedRecord: ProductComparisonRecord,
        images: List<AppImageEntity>
    ): ProductComparisonRecord = withContext(Dispatchers.IO) {
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

    suspend fun saveRecordOnly(record: ProductComparisonRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }
}
