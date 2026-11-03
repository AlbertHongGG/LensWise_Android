package com.lenswise.app.features.menu.service

import android.content.Context
import com.lenswise.app.core.scanner.BaseScannerService
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.data.MenuHistoryRepository

/**
 * 菜單掃描應用服務 (MenuScannerService)。
 * 繼承 [BaseScannerService]，專職負責串流結束後的菜單本機實體相片存檔（相對路徑）與歷史倉儲落地。
 */
open class MenuScannerService(
    historyRepository: MenuHistoryRepository
) : BaseScannerService<MenuScannerRecord>(historyRepository) {

    constructor(context: Context, historyRepository: MenuHistoryRepository) : this(historyRepository)

    override fun transformRecordBeforeSave(
        record: MenuScannerRecord,
        recordId: String,
        timestamp: Long,
        savedImagePaths: List<String>
    ): MenuScannerRecord {
        return record.copy(
            id = recordId,
            timestamp = timestamp,
            imagePaths = savedImagePaths
        )
    }
}
