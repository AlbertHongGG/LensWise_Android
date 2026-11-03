package com.lenswise.app.core.backup.model

/**
 * 還原合併策略 (RestoreStrategy)。
 */
enum class RestoreStrategy {
    /** 智慧合併覆蓋：保留手機現有資料，若有相同識別碼則以備份資料覆蓋，其餘併存 */
    MERGE_OVERWRITE,

    /** 完全鏡像替換：清空目標模組所有本機資料，完全以備份資料替換 */
    CLEAN_REPLACE
}
