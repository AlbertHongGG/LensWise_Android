package com.lenswise.app.core.backup.module

import com.lenswise.app.core.backup.contract.BackupContributionModule
import com.lenswise.app.core.backup.model.ModuleMetadata
import com.lenswise.app.core.backup.model.RestoreStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 目錄型日誌備份貢獻模組 (DirectoryLogBackupContributionModule)。
 * 負責系統通知日誌 (logs/notifications) 或 AI API 日誌 (logs/api) 的非同步匯出、
 * 原子覆蓋還原與 StateFlow 熱重載。
 */
class DirectoryLogBackupContributionModule(
    val baseDir: File,
    val relativeDir: String,
    override val moduleId: String,
    override val displayName: String,
    private val onReload: () -> Unit
) : BackupContributionModule {

    val sourceDir: File get() = File(baseDir, relativeDir)

    override suspend fun exportToStaging(stagingRoot: File): ModuleMetadata = withContext(Dispatchers.IO) {
        val stagingLogDir = File(stagingRoot, relativeDir).apply { mkdirs() }
        var logsCount = 0

        if (sourceDir.exists()) {
            sourceDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }?.forEach { logFile ->
                val destFile = File(stagingLogDir, logFile.name)
                logFile.copyTo(destFile, overwrite = true)
                logsCount++
            }
        }

        ModuleMetadata(
            recordsCount = logsCount,
            photosCount = 0,
            description = displayName
        )
    }

    override suspend fun importFromStaging(
        stagingRoot: File,
        strategy: RestoreStrategy
    ): ModuleMetadata = withContext(Dispatchers.IO) {
        val stagingLogDir = File(stagingRoot, relativeDir)
        if (!stagingLogDir.exists()) {
            return@withContext ModuleMetadata(0, 0, displayName)
        }

        if (strategy == RestoreStrategy.CLEAN_REPLACE && sourceDir.exists()) {
            sourceDir.listFiles()?.forEach { it.delete() }
        }

        if (!sourceDir.exists()) sourceDir.mkdirs()
        var restoredCount = 0

        stagingLogDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }?.forEach { logFile ->
            val destFile = File(sourceDir, logFile.name)
            logFile.copyTo(destFile, overwrite = true)
            restoredCount++
        }

        ModuleMetadata(
            recordsCount = restoredCount,
            photosCount = 0,
            description = displayName
        )
    }

    override fun reloadMemoryState() {
        onReload()
    }
}
