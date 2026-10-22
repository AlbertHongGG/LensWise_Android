package com.lenswise.app.features.settings

import com.lenswise.app.domain.model.settings.TargetLanguage

/**
 * 設定頁面狀態 (SettingsUiState)。
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isNotificationLogEnabled: Boolean = true,
    val isApiLogEnabled: Boolean = true,
    val isAiAgentEnabled: Boolean = true,
    val targetLanguage: TargetLanguage = TargetLanguage.TRADITIONAL_CHINESE
)

