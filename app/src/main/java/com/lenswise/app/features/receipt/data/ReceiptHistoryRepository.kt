package com.lenswise.app.features.receipt.data

import android.content.Context
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 收據歷史紀錄本地檔案倉儲 (ReceiptHistoryRepository)。
 * 繼承 [FileHistoryRepository]，統一管理 JSON 與 receipt_images 照片生命週期。
 */
open class ReceiptHistoryRepository(baseDir: File) : FileHistoryRepository<ReceiptScannerRecord>(
    baseDir = baseDir,
    subDirName = "receipts",
    imagesSubDirName = "receipt_images",
    serializer = ReceiptScannerRecord.serializer()
) {
    constructor(context: Context) : this(context.filesDir)

    val receiptsFlow: StateFlow<List<ReceiptScannerRecord>> get() = recordsFlow
}
