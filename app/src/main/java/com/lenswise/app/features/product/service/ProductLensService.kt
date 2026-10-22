package com.lenswise.app.features.product.service

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.data.ProductHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 商品深度分析應用服務 (ProductLensService)。
 * 專職負責串流結束後的本機實體相片存檔與歷史倉儲落地。
 */
class ProductLensService(
    private val context: Context,
    private val historyRepository: ProductHistoryRepository
) {

    private val imagesDir = File(context.filesDir, "history/product_images").apply {
        if (!exists()) mkdirs()
    }

    suspend fun saveCompletedRecord(
        parsedRecord: ProductLensRecord,
        images: List<AppImageEntity>
    ): ProductLensRecord = withContext(Dispatchers.IO) {
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

    suspend fun saveRecordOnly(record: ProductLensRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }
}
