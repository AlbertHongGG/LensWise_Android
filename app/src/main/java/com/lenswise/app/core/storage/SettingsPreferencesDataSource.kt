package com.lenswise.app.core.storage

import com.lenswise.app.domain.model.settings.TargetLanguage

/**
 * 偏好設定資料來源契約介面 (SettingsPreferencesDataSource)。
 * 負責系統深色模式、通知日誌開關、API 日誌開關與目標翻譯語言之讀取與保存。
 */
interface SettingsPreferencesDataSource {
    fun isDarkMode(): Boolean
    fun setDarkMode(enabled: Boolean)

    fun isNotificationLogEnabled(): Boolean
    fun setNotificationLogEnabled(enabled: Boolean)

    fun isApiLogEnabled(): Boolean
    fun setApiLogEnabled(enabled: Boolean)

    fun getTargetLanguage(): TargetLanguage
    fun setTargetLanguage(language: TargetLanguage)
}
