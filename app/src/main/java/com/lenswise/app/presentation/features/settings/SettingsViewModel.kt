package com.lenswise.app.presentation.features.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.storage.SettingsPreferencesDataSource
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import com.lenswise.app.domain.model.settings.TargetLanguage
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.IAiConfigRepository
import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 偏好設定業務模型 (SettingsViewModel)。
 * 遵循純淨 ViewModel 與介面依賴注入架構，支援 100% JVM 單元測試。
 */
class SettingsViewModel(
    private val preferencesDataSource: SettingsPreferencesDataSource,
    private val aiConfigRepository: IAiConfigRepository = createDefaultAiConfigRepository(),
    scope: CoroutineScope? = null
) : ViewModel() {

    constructor() : this(createDefaultPreferencesDataSource(), createDefaultAiConfigRepository())

    private val vmScope = scope ?: runCatching {
        val s = viewModelScope
        Dispatchers.Main.immediate.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext)
        s
    }.getOrElse { CoroutineScope(Dispatchers.Default) }

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isDarkMode = preferencesDataSource.isDarkMode(),
            isNotificationLogEnabled = preferencesDataSource.isNotificationLogEnabled(),
            isApiLogEnabled = preferencesDataSource.isApiLogEnabled(),
            isAiAgentEnabled = aiConfigRepository.configState.value.isAiAgentEnabled,
            targetLanguage = preferencesDataSource.getTargetLanguage()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        vmScope.launch {
            aiConfigRepository.configState.collect { configState ->
                _uiState.update { it.copy(isAiAgentEnabled = configState.isAiAgentEnabled) }
            }
        }
    }

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

    fun setAiAgentEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAiAgentEnabled = enabled) }
        aiConfigRepository.setAiAgentEnabled(enabled)
    }

    fun setTargetLanguage(language: TargetLanguage) {
        _uiState.update { it.copy(targetLanguage = language) }
        preferencesDataSource.setTargetLanguage(language)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? LensWiseApplication)
                    ?: LensWiseApplication.instance
                val preferencesRepository = SettingsPreferencesRepository(application)
                val aiConfigRepo = application.aiConfigRepository
                SettingsViewModel(preferencesRepository, aiConfigRepo)
            }
        }
    }
}

private class DefaultInMemoryAiConfigRepository : IAiConfigRepository {
    private val _state = MutableStateFlow(AiConfigState())
    override val configState: StateFlow<AiConfigState> = _state.asStateFlow()
    override fun getEffectiveConfigFor(agentType: AiAgentType) =
        _state.value.getEffectiveConfigFor(agentType)
    override fun setAiAgentEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isAiAgentEnabled = enabled)
    }
    override fun setGlobalProvider(type: com.lenswise.app.features.ai.config.AiProviderType) {
        _state.value = _state.value.copy(globalActiveProviderType = type)
    }
    override fun saveGlobalProviderConfig(
        type: com.lenswise.app.features.ai.config.AiProviderType,
        config: com.lenswise.app.features.ai.config.AiProviderConfig,
        setAsActive: Boolean
    ) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(
            globalProviderConfigs = updated,
            globalActiveProviderType = if (setAsActive) type else _state.value.globalActiveProviderType
        )
    }
    override fun saveAgentProvider(
        agentType: AiAgentType,
        customProviderType: com.lenswise.app.features.ai.config.AiProviderType?
    ) {
        val currentAgent = _state.value.getOverrideConfigFor(agentType)
        val updatedAgentConfigs = _state.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = currentAgent.copy(customProviderType = customProviderType)
        _state.value = _state.value.copy(agentConfigs = updatedAgentConfigs)
    }
    override fun saveAgentFieldConfig(
        agentType: AiAgentType,
        providerType: com.lenswise.app.features.ai.config.AiProviderType,
        config: com.lenswise.app.features.ai.config.AiProviderConfig
    ) {
        val currentAgent = _state.value.getOverrideConfigFor(agentType)
        val updatedLocal = currentAgent.providerConfigs.toMutableMap()
        updatedLocal[providerType] = config
        val updatedAgentConfigs = _state.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = currentAgent.copy(providerConfigs = updatedLocal)
        _state.value = _state.value.copy(agentConfigs = updatedAgentConfigs)
    }
    override fun saveAgentConfig(
        agentType: AiAgentType,
        providerType: com.lenswise.app.features.ai.config.AiProviderType?,
        config: com.lenswise.app.features.ai.config.AiProviderConfig?
    ) {
        if (providerType != null && config == null) {
            saveAgentProvider(agentType, providerType)
        } else if (config != null) {
            val target = providerType ?: _state.value.getEffectiveConfigFor(agentType).effectiveProviderType
            saveAgentFieldConfig(agentType, target, config)
        }
    }
}

private object DefaultInMemorySettingsDataSource : SettingsPreferencesDataSource {
    private var dark = true
    private var notif = true
    private var api = true
    private var lang: TargetLanguage = TargetLanguage.TRADITIONAL_CHINESE

    override fun isDarkMode(): Boolean = dark
    override fun setDarkMode(enabled: Boolean) { dark = enabled }
    override fun isNotificationLogEnabled(): Boolean = notif
    override fun setNotificationLogEnabled(enabled: Boolean) { notif = enabled }
    override fun isApiLogEnabled(): Boolean = api
    override fun setApiLogEnabled(enabled: Boolean) { api = enabled }
    override fun getTargetLanguage(): TargetLanguage = lang
    override fun setTargetLanguage(language: TargetLanguage) { lang = language }
}

private fun createDefaultPreferencesDataSource(): SettingsPreferencesDataSource {
    return try {
        SettingsPreferencesRepository(LensWiseApplication.instance)
    } catch (_: Throwable) {
        DefaultInMemorySettingsDataSource
    }
}

private fun createDefaultAiConfigRepository(): IAiConfigRepository {
    return try {
        LensWiseApplication.instance.aiConfigRepository
    } catch (_: Throwable) {
        DefaultInMemoryAiConfigRepository()
    }
}
