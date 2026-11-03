package com.lenswise.app.features.menu.data

import android.content.Context
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 菜單掃描歷史紀錄本地檔案倉儲 (MenuHistoryRepository)。
 * 繼承 [FileHistoryRepository]，統一管理 JSON 與 menu_images 照片生命週期。
 */
open class MenuHistoryRepository(baseDir: File) : FileHistoryRepository<MenuScannerRecord>(
    baseDir = baseDir,
    subDirName = "menus",
    imagesSubDirName = "menu_images",
    serializer = MenuScannerRecord.serializer()
) {
    constructor(context: Context) : this(context.filesDir)

    val menusFlow: StateFlow<List<MenuScannerRecord>> get() = recordsFlow

    override fun deleteRecordImages(record: MenuScannerRecord) {
        super.deleteRecordImages(record)
        record.translatedImagePaths.values.forEach { path ->
            deleteImageFile(path)
        }
    }
}
