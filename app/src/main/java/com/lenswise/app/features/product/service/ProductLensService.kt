package com.lenswise.app.features.product.service

import android.content.Context
import com.lenswise.app.core.scanner.BaseScannerService
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.data.ProductHistoryRepository

/**
 * 商品深度分析應用服務 (ProductLensService)。
 * 繼承 [BaseScannerService]，專職負責串流結束後的本機實體相片存檔（相對路徑）與歷史倉儲落地。
 */
open class ProductLensService(
    historyRepository: ProductHistoryRepository
) : BaseScannerService<ProductLensRecord>(historyRepository) {

    constructor(context: Context, historyRepository: ProductHistoryRepository) : this(historyRepository)

    override fun transformRecordBeforeSave(
        record: ProductLensRecord,
        recordId: String,
        timestamp: Long,
        savedImagePaths: List<String>
    ): ProductLensRecord {
        return record.copy(
            id = recordId,
            timestamp = timestamp,
            imagePaths = savedImagePaths
        )
    }
}
