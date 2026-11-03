package com.lenswise.app.features.settings

import com.lenswise.app.domain.model.settings.TargetLanguage

/**
 * 備份與還原操作互斥狀態機 (BackupOperationState)。
 * 透過 Sealed Interface 在編譯期保證「閒置、匯出中、還原中」三態嚴格互斥，
 * 徹底消滅雙重轉圈與狀態污染。
 */
sealed interface BackupOperationState {
    /** 閒置狀態：無任何背景任務 */
    object Idle : BackupOperationState

    /** 正在執行匯出打包 */
    object Exporting : BackupOperationState

    /** 正在執行解壓還原 */
    object Importing : BackupOperationState

    val isInProgress: Boolean
        get() = this !is Idle

    val isExporting: Boolean
        get() = this is Exporting

    val isImporting: Boolean
        get() = this is Importing
}

/**
 * 設定頁面狀態 (SettingsUiState)。
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isNotificationLogEnabled: Boolean = true,
    val isApiLogEnabled: Boolean = true,
    val isAiAgentEnabled: Boolean = true,
    val targetLanguage: TargetLanguage = TargetLanguage.TRADITIONAL_CHINESE,
    val backupState: BackupOperationState = BackupOperationState.Idle
)
