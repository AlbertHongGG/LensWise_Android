package com.lenswise.app.core.scanner

import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.history.IHistoryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 掃描分析應用服務基礎抽象類別 (BaseScannerService)。
 * 統一管理串流結束後的本機實體照片存檔（相對路徑）與歷史倉儲落地。
 */
abstract class BaseScannerService<TRecord : IHistoryRecord>(
    protected val historyRepository: FileHistoryRepository<TRecord>
) {
    /**
     * 串流完成時，寫入相片並將紀錄持久化至本地歷史紀錄。
     */
    suspend fun saveCompletedRecord(
        parsedRecord: TRecord,
        images: List<AppImageEntity>
    ): TRecord = withContext(Dispatchers.IO) {
        val recordId = if (parsedRecord.id.isBlank()) UUID.randomUUID().toString() else parsedRecord.id
        val savedRelativePaths = historyRepository.saveImages(recordId, images)
        val timestamp = if (parsedRecord.timestamp > 0) parsedRecord.timestamp else System.currentTimeMillis()

        val transformedRecord = transformRecordBeforeSave(
            record = parsedRecord,
            recordId = recordId,
            timestamp = timestamp,
            savedImagePaths = savedRelativePaths
        )

        historyRepository.save(transformedRecord)
        transformedRecord
    }

    /**
     * 原地編輯或更新時直接保存紀錄。
     */
    suspend fun saveRecordOnly(record: TRecord) = withContext(Dispatchers.IO) {
        historyRepository.save(record)
    }

    /**
     * 子類別可覆寫此方法注入領域特化轉換（例如更新 ID、時間戳記、圖片路徑或重新計算數值等）。
     */
    protected abstract fun transformRecordBeforeSave(
        record: TRecord,
        recordId: String,
        timestamp: Long,
        savedImagePaths: List<String>
    ): TRecord
}
