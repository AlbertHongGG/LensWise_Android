package com.lenswise.app.features.product.data

import android.content.Context
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.product.ProductLensRecord
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 商品歷史紀錄本地檔案倉儲 (ProductHistoryRepository)。
 * 繼承 [FileHistoryRepository]，統一管理 JSON 與 product_images 照片生命週期。
 */
open class ProductHistoryRepository(baseDir: File) : FileHistoryRepository<ProductLensRecord>(
    baseDir = baseDir,
    subDirName = "products",
    imagesSubDirName = "product_images",
    serializer = ProductLensRecord.serializer()
) {
    constructor(context: Context) : this(context.filesDir)

    val productsFlow: StateFlow<List<ProductLensRecord>> get() = recordsFlow
}
