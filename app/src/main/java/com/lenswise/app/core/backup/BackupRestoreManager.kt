package com.lenswise.app.core.backup

import android.content.Context
import android.net.Uri
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.backup.contract.BackupContributionModule
import com.lenswise.app.core.backup.engine.BackupArchiveEngine
import com.lenswise.app.core.backup.model.BackupSummary
import com.lenswise.app.core.backup.model.RestoreStrategy
import com.lenswise.app.core.backup.model.RestoreSummary
import com.lenswise.app.core.backup.module.DirectoryLogBackupContributionModule
import com.lenswise.app.core.backup.module.HistoryBackupContributionModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 備份與還原系統高階管理器 (BackupRestoreManager)。
 * 封裝 Android SAF (Storage Access Framework) 之 Uri 串流橋接，
 * 統一提供應用層呼叫。
 */
class BackupRestoreManager(
    private val context: Context,
    val engine: BackupArchiveEngine
) {
    /**
     * 將所有歷史紀錄、照片與日誌匯出至 SAF 給予之目標 Uri (application/zip)。
     */
    suspend fun exportToUri(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: return@withContext Result.failure(IllegalStateException("無法開啟目標輸出串流: $uri"))

        outputStream.use { os ->
            engine.createBackupZip(os)
        }
    }

    /**
     * 從 SAF 給予之 ZIP 檔案 Uri 解壓縮並還原。
     */
    suspend fun importFromUri(
        uri: Uri,
        strategy: RestoreStrategy = RestoreStrategy.MERGE_OVERWRITE
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(IllegalStateException("無法開啟目標輸入串流: $uri"))

        inputStream.use { `is` ->
            engine.restoreFromZip(`is`, strategy)
        }
    }

    companion object {
        fun generateDefaultBackupFileName(): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            return "LensWise_Backup_$timestamp.zip"
        }

        fun createDefault(app: LensWiseApplication): BackupRestoreManager {
            val modules = listOf<BackupContributionModule>(
                HistoryBackupContributionModule(app.receiptHistoryRepository, "收據歷史紀錄"),
                HistoryBackupContributionModule(app.productHistoryRepository, "商品歷史紀錄"),
                HistoryBackupContributionModule(app.comparisonHistoryRepository, "商品比對歷史紀錄"),
                HistoryBackupContributionModule(app.menuHistoryRepository, "菜單掃描歷史紀錄"),
                DirectoryLogBackupContributionModule(
                    baseDir = app.filesDir,
                    relativeDir = "logs/notifications",
                    moduleId = "logs_notifications",
                    displayName = "系統通知日誌",
                    onReload = { app.notificationLogManager.reload() }
                ),
                DirectoryLogBackupContributionModule(
                    baseDir = app.filesDir,
                    relativeDir = "logs/api",
                    moduleId = "logs_api",
                    displayName = "AI API 互動日誌",
                    onReload = { app.apiLogManager.reload() }
                )
            )

            val tempDir = File(app.cacheDir, "backup_temp").apply { if (!exists()) mkdirs() }
            val engine = BackupArchiveEngine(modules = modules, tempDir = tempDir)
            return BackupRestoreManager(app, engine)
        }
    }
}
