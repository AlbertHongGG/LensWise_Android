package com.lenswise.app.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * 偏好設定本地持久化倉庫 (SettingsPreferencesRepository)。
 * 實作 [SettingsPreferencesDataSource]，管理 SharedPreferences 本地存取。
 */
class SettingsPreferencesRepository(
    private val prefs: SharedPreferences
) : SettingsPreferencesDataSource {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    override fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    override fun isNotificationLogEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_LOG, true)
    }

    override fun setNotificationLogEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_LOG, enabled).apply()
    }

    override fun isApiLogEnabled(): Boolean {
        return prefs.getBoolean(KEY_API_LOG, true)
    }

    override fun setApiLogEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_API_LOG, enabled).apply()
    }

    override fun getTargetLanguage(): String {
        return prefs.getString(KEY_TARGET_LANGUAGE, "繁體中文") ?: "繁體中文"
    }

    override fun setTargetLanguage(language: String) {
        prefs.edit().putString(KEY_TARGET_LANGUAGE, language).apply()
    }

    companion object {
        private const val PREFS_NAME = "lenswise_settings"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_NOTIFICATION_LOG = "pref_notification_logging_enabled"
        private const val KEY_API_LOG = "pref_api_logging_enabled"
        private const val KEY_TARGET_LANGUAGE = "pref_target_language"
    }
}
