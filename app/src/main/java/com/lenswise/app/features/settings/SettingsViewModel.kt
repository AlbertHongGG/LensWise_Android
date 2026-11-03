package com.lenswise.app.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.backup.BackupRestoreManager
import com.lenswise.app.core.backup.model.RestoreStrategy
import com.lenswise.app.core.storage.SettingsPreferencesDataSource
import com.lenswise.app.core.storage.SettingsPreferencesRepository
import com.lenswise.app.domain.model.settings.TargetLanguage
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.IAiConfigRepository
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.notification.NotificationController
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
    private val backupRestoreManager: BackupRestoreManager? = null,
    scope: CoroutineScope? = null
) : ViewModel() {

    constructor() : this(createDefaultPreferencesDataSource(), createDefaultAiConfigRepository(), null, null)

    constructor(
        preferencesDataSource: SettingsPreferencesDataSource,
        aiConfigRepository: IAiConfigRepository,
        scope: CoroutineScope?
    ) : this(preferencesDataSource, aiConfigRepository, null, scope)

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

    fun getDefaultBackupFileName(): String {
        return BackupRestoreManager.generateDefaultBackupFileName()
    }

    fun exportBackupToUri(uri: Uri) {
        if (_uiState.value.isBackupInProgress) return
        val manager = backupRestoreManager ?: run {
            NotificationController.showError("備份服務未初始化")
            return
        }

        _uiState.update { it.copy(isBackupInProgress = true, backupOperationMessage = "正在打包匯出備份資料...") }
        vmScope.launch {
            try {
                val result = manager.exportToUri(uri)
                result.onSuccess { summary ->
                    val meta = summary.manifest
                    NotificationController.showSuccess(
                        "備份成功：已匯出 ${meta.totalRecords} 筆歷史、${meta.totalPhotos} 張照片與 ${meta.totalLogs} 筆日誌"
                    )
                }.onFailure { e ->
                    NotificationController.showError("備份匯出失敗：${e.localizedMessage ?: e.message}")
                }
            } finally {
                _uiState.update { it.copy(isBackupInProgress = false, backupOperationMessage = null) }
            }
        }
    }

    fun importBackupFromUri(
        uri: Uri,
        strategy: RestoreStrategy = RestoreStrategy.MERGE_OVERWRITE
    ) {
        if (_uiState.value.isBackupInProgress) return
        val manager = backupRestoreManager ?: run {
            NotificationController.showError("備份還原服務未初始化")
            return
        }

        _uiState.update { it.copy(isBackupInProgress = true, backupOperationMessage = "正在解壓驗證並還原資料...") }
        vmScope.launch {
            try {
                val result = manager.importFromUri(uri, strategy)
                result.onSuccess { summary ->
                    NotificationController.showSuccess(
                        "還原成功：已還原 ${summary.totalRecordsRestored} 筆歷史、${summary.totalPhotosRestored} 張照片與 ${summary.totalLogsRestored} 筆日誌"
                    )
                }.onFailure { e ->
                    NotificationController.showError("還原失敗：${e.localizedMessage ?: e.message}")
                }
            } finally {
                _uiState.update { it.copy(isBackupInProgress = false, backupOperationMessage = null) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? LensWiseApplication)
                    ?: LensWiseApplication.instance
                val preferencesRepository = SettingsPreferencesRepository(application)
                val aiConfigRepo = application.aiConfigRepository
                val backupManager = application.backupRestoreManager
                SettingsViewModel(preferencesRepository, aiConfigRepo, backupManager)
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
    override fun selectGlobalProvider(type: com.lenswise.app.features.ai.config.AiProviderType) {
        _state.value = _state.value.copy(selectedGlobalProvider = type)
    }
    override fun saveGlobalProviderConfig(
        type: com.lenswise.app.features.ai.config.AiProviderType,
        config: com.lenswise.app.features.ai.config.AiProviderConfig
    ) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(globalProviderConfigs = updated)
    }
    override fun saveAgentProvider(
        agentType: AiAgentType,
        providerType: com.lenswise.app.features.ai.config.AiProviderType
    ) {
        val currentAgent = _state.value.getAgentConfig(agentType)
        val updatedAgentConfigs = _state.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = currentAgent.copy(providerType = providerType)
        _state.value = _state.value.copy(agentConfigs = updatedAgentConfigs)
    }
    override fun saveAgentFieldConfig(
        agentType: AiAgentType,
        providerType: com.lenswise.app.features.ai.config.AiProviderType,
        config: com.lenswise.app.features.ai.config.AiProviderConfig
    ) {
        val currentAgent = _state.value.getAgentConfig(agentType)
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
        if (providerType != null) {
            saveAgentProvider(agentType, providerType)
        }
        if (config != null) {
            val target = providerType ?: _state.value.getAgentConfig(agentType).providerType
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
