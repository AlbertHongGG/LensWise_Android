package com.lenswise.app.features.menu.service

import android.content.Context
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.data.MenuHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 菜單掃描應用服務 (MenuScannerService)。
 * 專職負責串流結束後的菜單本機實體相片存檔（相對路徑）與歷史倉儲落地。
 */
class MenuScannerService(
    private val context: Context,
    private val historyRepository: MenuHistoryRepository
) {
    suspend fun saveCompletedRecord(
        parsedRecord: MenuScannerRecord,
        images: List<AppImageEntity>
    ): MenuScannerRecord = withContext(Dispatchers.IO) {
        val recordId = if (parsedRecord.id.isBlank()) UUID.randomUUID().toString() else parsedRecord.id
        val savedRelativePaths = historyRepository.saveImages(recordId, images)

        val finalRecord = parsedRecord.copy(
            id = recordId,
            timestamp = if (parsedRecord.timestamp > 0) parsedRecord.timestamp else System.currentTimeMillis(),
            imagePaths = savedRelativePaths
        )

        historyRepository.save(finalRecord)
        finalRecord
    }

    suspend fun saveRecordOnly(record: MenuScannerRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }
}
