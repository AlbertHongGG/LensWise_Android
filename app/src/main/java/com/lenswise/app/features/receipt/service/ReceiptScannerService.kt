package com.lenswise.app.features.receipt.service

import android.content.Context
import com.lenswise.app.core.scanner.BaseScannerService
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository

/**
 * 收據萃取與持久化應用服務 (ReceiptScannerService)。
 * 繼承 [BaseScannerService]，在落地前進行總額二次校驗與圖片相對路徑儲存。
 */
open class ReceiptScannerService(
    historyRepository: ReceiptHistoryRepository
) : BaseScannerService<ReceiptScannerRecord>(historyRepository) {

    constructor(context: Context, historyRepository: ReceiptHistoryRepository) : this(historyRepository)

    override fun transformRecordBeforeSave(
        record: ReceiptScannerRecord,
        recordId: String,
        timestamp: Long,
        savedImagePaths: List<String>
    ): ReceiptScannerRecord {
        return record.recalculateTotals().copy(
            id = recordId,
            timestamp = timestamp,
            imagePaths = savedImagePaths
        )
    }
}
