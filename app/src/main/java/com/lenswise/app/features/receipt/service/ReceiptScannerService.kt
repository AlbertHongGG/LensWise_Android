package com.lenswise.app.features.receipt.service

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 收據萃取與持久化應用服務 (ReceiptScannerService)。
 * 專職負責串流結束後的本機實體相片存檔、總計金額二次校驗與歷史倉儲落地。
 */
class ReceiptScannerService(
    private val context: Context,
    private val historyRepository: ReceiptHistoryRepository
) {

    private val imagesDir = File(context.filesDir, "history/receipt_images").apply {
        if (!exists()) mkdirs()
    }

    /**
     * 串流終止或解析完成時持久化發票記錄與實體圖片。
     */
    suspend fun saveCompletedRecord(
        parsedRecord: ReceiptScannerRecord,
        images: List<AppImageEntity>
    ): ReceiptScannerRecord = withContext(Dispatchers.IO) {
        val recordId = if (parsedRecord.id.isBlank()) UUID.randomUUID().toString() else parsedRecord.id
        val savedImagePaths = mutableListOf<String>()

        images.forEachIndexed { index, entity ->
            val imageFile = File(imagesDir, "${recordId}_$index.jpg")
            imageFile.writeBytes(entity.byteArray)
            savedImagePaths.add(imageFile.absolutePath)
        }

        val finalRecord = parsedRecord.recalculateTotals().copy(
            id = recordId,
            timestamp = if (parsedRecord.timestamp > 0) parsedRecord.timestamp else System.currentTimeMillis(),
            imagePaths = savedImagePaths
        )

        historyRepository.save(finalRecord)
        finalRecord
    }

    /**
     * 原地編輯後直接更新保存歷史記錄。
     */
    suspend fun saveRecordOnly(record: ReceiptScannerRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }
}
