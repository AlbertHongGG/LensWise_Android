package com.lenswise.app.presentation.features.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.storage.SettingsPreferencesDataSource
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 偏好設定業務模型 (SettingsViewModel)。
 * 遵循純淨 ViewModel 與介面依賴注入架構，支援 100% JVM 單元測試。
 */
class SettingsViewModel(
    private val preferencesDataSource: SettingsPreferencesDataSource
) : ViewModel() {

    constructor() : this(createDefaultPreferencesDataSource())

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isDarkMode = preferencesDataSource.isDarkMode(),
            isNotificationLogEnabled = preferencesDataSource.isNotificationLogEnabled(),
            isApiLogEnabled = preferencesDataSource.isApiLogEnabled(),
            targetLanguage = preferencesDataSource.getTargetLanguage()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
        preferencesDataSource.setDarkMode(enabled)
    }

    fun setNotificationLogEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isNotificationLogEnabled = enabled) }
        preferencesDataSource.setNotificationLogEnabled(enabled)
    }

    fun setApiLogEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isApiLogEnabled = enabled) }
        preferencesDataSource.setApiLogEnabled(enabled)
    }

    fun setTargetLanguage(language: String) {
        _uiState.update { it.copy(targetLanguage = language) }
        preferencesDataSource.setTargetLanguage(language)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? Application)
                    ?: LensWiseApplication.instance
                val preferencesRepository = SettingsPreferencesRepository(application)
                SettingsViewModel(preferencesRepository)
            }
        }
    }
}

private object DefaultInMemorySettingsDataSource : SettingsPreferencesDataSource {
    private var dark = true
    private var notif = true
    private var api = true
    private var lang = "繁體中文"

    override fun isDarkMode(): Boolean = dark
    override fun setDarkMode(enabled: Boolean) { dark = enabled }
    override fun isNotificationLogEnabled(): Boolean = notif
    override fun setNotificationLogEnabled(enabled: Boolean) { notif = enabled }
    override fun isApiLogEnabled(): Boolean = api
    override fun setApiLogEnabled(enabled: Boolean) { api = enabled }
    override fun getTargetLanguage(): String = lang
    override fun setTargetLanguage(language: String) { lang = language }
}

private fun createDefaultPreferencesDataSource(): SettingsPreferencesDataSource {
    return try {
        SettingsPreferencesRepository(LensWiseApplication.instance)
    } catch (_: Throwable) {
        DefaultInMemorySettingsDataSource
    }
}
