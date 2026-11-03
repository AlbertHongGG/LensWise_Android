package com.lenswise.app.core.backup.contract

import com.lenswise.app.core.backup.model.ModuleMetadata
import com.lenswise.app.core.backup.model.RestoreStrategy
import java.io.File

/**
 * 模組化備份與還原貢獻契約介面 (BackupContributionModule)。
 * 任何參與 ZIP 備份與還原的子系統（歷史紀錄、日誌系統等）皆實作此契約。
 * 遵循開放封閉原則 (OCP)，未來新增掃描或日誌類型無需修改備份核心引擎。
 */
interface BackupContributionModule {
    /** 模組唯一識別碼 (例如: "receipts", "products", "logs_api") */
    val moduleId: String

    /** 模組可讀描述 (例如: "收據歷史紀錄與照片") */
    val displayName: String

    /**
     * 將模組檔案導出複製至臨時暫存目錄 stagingRoot (例如 stagingRoot/history/receipts)
     * @return 該模組備份的中繼資料 (筆數、照片數)
     */
    suspend fun exportToStaging(stagingRoot: File): ModuleMetadata

    /**
     * 從解壓縮後的暫存目錄 stagingRoot 還原檔案至本機目標路徑
     * @param stagingRoot 已通過校驗的 ZIP 解壓臨時目錄
     * @param strategy 還原合併策略
     * @return 該模組實際還原的中繼資料
     */
    suspend fun importFromStaging(stagingRoot: File, strategy: RestoreStrategy): ModuleMetadata

    /**
     * 還原落地完成後，通知該模組重載記憶體狀態與 StateFlow
     */
    fun reloadMemoryState()
}
