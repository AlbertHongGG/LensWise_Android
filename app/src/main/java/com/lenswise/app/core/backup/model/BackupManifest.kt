package com.lenswise.app.core.backup.model

import kotlinx.serialization.Serializable

/**
 * 備份 ZIP 中繼資料清單 (BackupManifest)。
 * 位於備份 ZIP 檔根目錄 manifest.json，記錄格式版本、產生時間與各模組統計筆數。
 */
@Serializable
data class BackupManifest(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appId: String = APP_IDENTIFIER,
    val createdAtIso: String = "",
    val totalRecords: Int = 0,
    val totalPhotos: Int = 0,
    val totalLogs: Int = 0,
    val modules: Map<String, ModuleMetadata> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_IDENTIFIER = "com.lenswise.android"
        const val MANIFEST_FILE_NAME = "manifest.json"
    }
}

@Serializable
data class ModuleMetadata(
    val recordsCount: Int = 0,
    val photosCount: Int = 0,
    val description: String = ""
)

/**
 * 匯出操作統計摘要。
 */
data class BackupSummary(
    val manifest: BackupManifest,
    val outputSizeBytes: Long
)

/**
 * 還原操作統計摘要。
 */
data class RestoreSummary(
    val totalRecordsRestored: Int,
    val totalPhotosRestored: Int,
    val totalLogsRestored: Int,
    val moduleDetails: Map<String, ModuleMetadata>
)
