package com.lenswise.app.features.comparison.data

import android.content.Context
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 商品比對歷史紀錄本地檔案倉儲 (ComparisonHistoryRepository)。
 * 繼承 [FileHistoryRepository]，統一管理 JSON 與 comparison_images 照片生命週期。
 */
open class ComparisonHistoryRepository(baseDir: File) : FileHistoryRepository<ProductComparisonRecord>(
    baseDir = baseDir,
    subDirName = "comparisons",
    imagesSubDirName = "comparison_images",
    serializer = ProductComparisonRecord.serializer()
) {
    constructor(context: Context) : this(context.filesDir)

    val comparisonsFlow: StateFlow<List<ProductComparisonRecord>> get() = recordsFlow
}
