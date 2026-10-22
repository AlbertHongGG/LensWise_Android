package com.lenswise.app.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.IAiConfigRepository
import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.coroutines.flow.StateFlow

/**
 * AI 代理調度中心業務模型 (AiDispatchViewModel)。
 * 遵循 MVVM 架構與單向資料流原則 (UDF)，依賴抽象介面 [IAiConfigRepository]，
 * 負責調度各 AI 供應商與 Agent 專屬配置。
 */
class AiDispatchViewModel(
    private val configRepository: IAiConfigRepository
) : ViewModel() {

    val uiState: StateFlow<AiConfigState> = configRepository.configState

    fun setAiAgentEnabled(enabled: Boolean) {
        configRepository.setAiAgentEnabled(enabled)
    }

    fun setEditingGlobalProvider(type: AiProviderType) {
        configRepository.setGlobalProvider(type)
    }

    fun setGlobalProvider(type: AiProviderType) {
        configRepository.setGlobalProvider(type)
    }

    fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsEditing: Boolean = false) {
        configRepository.saveGlobalProviderConfig(type, config, setAsEditing)
    }

    fun updateGlobalField(key: String, value: String) {
        val currentState = uiState.value
        val updated = currentState.editingGlobalConfig.copyWithField(key, value)
        configRepository.saveGlobalProviderConfig(currentState.globalActiveProviderType, updated)
    }

    fun saveAgentProvider(agentType: AiAgentType, providerType: AiProviderType?) {
        configRepository.saveAgentProvider(agentType = agentType, customProviderType = providerType)
    }

    fun updateAgentField(agentType: AiAgentType, key: String, value: String) {
        val currentState = uiState.value
        val effective = currentState.getEffectiveConfigFor(agentType)
        val updated = effective.activeConfig.copyWithField(key, value)
        configRepository.saveAgentFieldConfig(agentType = agentType, providerType = effective.effectiveProviderType, config = updated)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LensWiseApplication
                AiDispatchViewModel(
                    configRepository = app.aiConfigRepository
                )
            }
        }
    }
}
