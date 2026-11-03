package com.lenswise.app.features.comparison.service

import android.content.Context
import com.lenswise.app.core.scanner.BaseScannerService
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.data.ComparisonHistoryRepository

/**
 * 商品比對應用服務 (ProductComparisonService)。
 * 繼承 [BaseScannerService]，專職負責串流結束後的多商品本機實體相片存檔（相對路徑）與歷史倉儲落地。
 */
open class ProductComparisonService(
    historyRepository: ComparisonHistoryRepository
) : BaseScannerService<ProductComparisonRecord>(historyRepository) {

    constructor(context: Context, historyRepository: ComparisonHistoryRepository) : this(historyRepository)

    override fun transformRecordBeforeSave(
        record: ProductComparisonRecord,
        recordId: String,
        timestamp: Long,
        savedImagePaths: List<String>
    ): ProductComparisonRecord {
        return record.copy(
            id = recordId,
            timestamp = timestamp,
            imagePaths = savedImagePaths
        )
    }
}
