package com.lenswise.app.core.backup.module

import com.lenswise.app.core.backup.contract.BackupContributionModule
import com.lenswise.app.core.backup.model.ModuleMetadata
import com.lenswise.app.core.backup.model.RestoreStrategy
import com.lenswise.app.core.storage.FileHistoryRepository
import com.lenswise.app.domain.model.history.IHistoryRecord
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 歷史紀錄領域備份貢獻模組 (HistoryBackupContributionModule)。
 * 負責單一歷史倉儲（收據、商品、比對、菜單）的 JSON 紀錄與關聯實體照片的打包、
 * 孤兒照片過濾導出、原子級還原寫入與 StateFlow 熱重載。
 */
class HistoryBackupContributionModule<T : IHistoryRecord>(
    val repository: FileHistoryRepository<T>,
    override val displayName: String,
    override val moduleId: String = repository.subDirName
) : BackupContributionModule {

    override suspend fun exportToStaging(stagingRoot: File): ModuleMetadata = withContext(Dispatchers.IO) {
        val stagingRecordsDir = File(stagingRoot, "history/${repository.subDirName}").apply { mkdirs() }
        val stagingImagesDir = File(stagingRoot, "history/${repository.imagesSubDirName}").apply { mkdirs() }

        val allRecords = repository.getAll()
        var photosCount = 0

        allRecords.forEach { record ->
            // 1. 複製歷史紀錄 JSON
            val srcJson = File(repository.recordsDir, "${record.id}.json")
            if (srcJson.exists()) {
                val destJson = File(stagingRecordsDir, srcJson.name)
                srcJson.copyTo(destJson, overwrite = true)
            }

            // 2. 精確複製該紀錄參照的實體照片（杜絕孤兒照片進入備份檔）
            val allImagePaths = mutableListOf<String>().apply {
                addAll(record.imagePaths)
                if (record is MenuScannerRecord) {
                    addAll(record.translatedImagePaths.values)
                }
            }

            allImagePaths.forEach { relativeOrAbsolutePath ->
                if (relativeOrAbsolutePath.isNotBlank()) {
                    val srcPhoto = File(relativeOrAbsolutePath).let { f ->
                        if (f.isAbsolute) f else File(repository.baseDir, relativeOrAbsolutePath)
                    }
                    if (srcPhoto.exists()) {
                        val destPhoto = File(stagingImagesDir, srcPhoto.name)
                        srcPhoto.copyTo(destPhoto, overwrite = true)
                        photosCount++
                    }
                }
            }
        }

        ModuleMetadata(
            recordsCount = allRecords.size,
            photosCount = photosCount,
            description = displayName
        )
    }

    override suspend fun importFromStaging(
        stagingRoot: File,
        strategy: RestoreStrategy
    ): ModuleMetadata = withContext(Dispatchers.IO) {
        val stagingRecordsDir = File(stagingRoot, "history/${repository.subDirName}")
        val stagingImagesDir = File(stagingRoot, "history/${repository.imagesSubDirName}")

        if (!stagingRecordsDir.exists() && !stagingImagesDir.exists()) {
            return@withContext ModuleMetadata(0, 0, displayName)
        }

        if (strategy == RestoreStrategy.CLEAN_REPLACE) {
            repository.clearAll()
        }

        if (!repository.recordsDir.exists()) repository.recordsDir.mkdirs()
        if (!repository.imagesDir.exists()) repository.imagesDir.mkdirs()

        var restoredRecordsCount = 0
        var restoredPhotosCount = 0

        // 1. 還原實體照片
        stagingImagesDir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) || f.extension.equals("png", ignoreCase = true) }
            ?.forEach { photoFile ->
                val destFile = File(repository.imagesDir, photoFile.name)
                photoFile.copyTo(destFile, overwrite = true)
                restoredPhotosCount++
            }

        // 2. 還原 JSON 紀錄
        stagingRecordsDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }
            ?.forEach { jsonFile ->
                val destFile = File(repository.recordsDir, jsonFile.name)
                jsonFile.copyTo(destFile, overwrite = true)
                restoredRecordsCount++
            }

        ModuleMetadata(
            recordsCount = restoredRecordsCount,
            photosCount = restoredPhotosCount,
            description = displayName
        )
    }

    override fun reloadMemoryState() {
        repository.reload()
    }
}
