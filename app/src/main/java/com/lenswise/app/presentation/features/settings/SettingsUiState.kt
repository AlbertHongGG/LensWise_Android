package com.lenswise.app.presentation.features.settings

/**
 * 設定頁面狀態 (SettingsUiState)。
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val isNotificationLogEnabled: Boolean = true,
    val isApiLogEnabled: Boolean = true,
    val targetLanguage: String = "繁體中文"
)
