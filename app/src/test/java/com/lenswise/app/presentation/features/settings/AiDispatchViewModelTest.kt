package com.lenswise.app.presentation.features.settings

import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.EffectiveAgentAiConfig
import com.lenswise.app.features.ai.config.GeminiFlowConfig
import com.lenswise.app.features.ai.config.GoogleAiStudioConfig
import com.lenswise.app.features.ai.config.GoogleAiStudioModel
import com.lenswise.app.features.ai.config.IAiConfigRepository
import com.lenswise.app.features.ai.config.OllamaConfig
import com.lenswise.app.features.ai.config.VertexAiConfig
import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiDispatchViewModelTest {

    private lateinit var mockRepo: FakeAiConfigRepository
    private lateinit var viewModel: AiDispatchViewModel

    @Before
    fun setup() {
        mockRepo = FakeAiConfigRepository()
        viewModel = AiDispatchViewModel(mockRepo)
    }

    @Test
    fun `toggle ai agent enabled updates state correctly`() {
        assertTrue(viewModel.uiState.value.isAiAgentEnabled)

        viewModel.setAiAgentEnabled(false)
        assertFalse(viewModel.uiState.value.isAiAgentEnabled)

        viewModel.setAiAgentEnabled(true)
        assertTrue(viewModel.uiState.value.isAiAgentEnabled)
    }

    @Test
    fun `switch selected global provider updates state`() {
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, viewModel.uiState.value.selectedGlobalProvider)

        viewModel.selectGlobalProvider(AiProviderType.OLLAMA)
        assertEquals(AiProviderType.OLLAMA, viewModel.uiState.value.selectedGlobalProvider)
    }

    @Test
    fun `update global field updates global profile correctly`() {
        viewModel.selectGlobalProvider(AiProviderType.OLLAMA)
        viewModel.updateGlobalField("baseUrl", "http://192.168.1.100:11434")

        val currentConfig = viewModel.uiState.value.globalProviderConfigs[AiProviderType.OLLAMA] as OllamaConfig
        assertEquals("http://192.168.1.100:11434", currentConfig.baseUrl)
    }

    @Test
    fun `save agent provider explicitly switches agent target provider`() {
        val targetAgent = AiAgentType.RECEIPT_SCANNER
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, viewModel.uiState.value.getEffectiveConfigFor(targetAgent).effectiveProviderType)

        viewModel.saveAgentProvider(targetAgent, AiProviderType.VERTEX_AI)
        assertEquals(AiProviderType.VERTEX_AI, viewModel.uiState.value.getEffectiveConfigFor(targetAgent).effectiveProviderType)
    }

    @Test
    fun `update agent field updates local agent parameter without pinning`() {
        val targetAgent = AiAgentType.RECEIPT_SCANNER
        viewModel.saveAgentProvider(targetAgent, AiProviderType.GOOGLE_AI_STUDIO)

        // 1. 覆寫 Model 為 3.6 Flash
        viewModel.updateAgentField(targetAgent, "modelName", GoogleAiStudioModel.FLASH_3_6)
        val agentConfig = viewModel.uiState.value.getAgentConfig(targetAgent)
        val localConfig = agentConfig.getLocalConfig(AiProviderType.GOOGLE_AI_STUDIO) as GoogleAiStudioConfig
        assertEquals(GoogleAiStudioModel.FLASH_3_6, localConfig.modelName)

        val effective = viewModel.uiState.value.getEffectiveConfigFor(targetAgent)
        val active = effective.activeConfig as GoogleAiStudioConfig
        assertEquals(GoogleAiStudioModel.FLASH_3_6, active.modelName)

        // 2. 切換為空字串，應順利還原為繼承全域 Profile，絕對不被釘住
        viewModel.updateAgentField(targetAgent, "modelName", "")
        val localReverted = viewModel.uiState.value.getAgentConfig(targetAgent).getLocalConfig(AiProviderType.GOOGLE_AI_STUDIO) as GoogleAiStudioConfig
        assertEquals("", localReverted.modelName)

        val effectiveReverted = viewModel.uiState.value.getEffectiveConfigFor(targetAgent)
        val activeReverted = effectiveReverted.activeConfig as GoogleAiStudioConfig
        assertEquals(GoogleAiStudioModel.FLASH_3_8, activeReverted.modelName) // 成功繼承全域預設 3.8
    }
}

private class FakeAiConfigRepository : IAiConfigRepository {
    private val _state = MutableStateFlow(
        AiConfigState(
            selectedGlobalProvider = AiProviderType.GOOGLE_AI_STUDIO,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(),
                AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(modelName = GoogleAiStudioModel.FLASH_3_8),
                AiProviderType.OLLAMA to OllamaConfig(),
                AiProviderType.VERTEX_AI to VertexAiConfig()
            ),
            agentConfigs = AiAgentType.entries.associateWith {
                AgentAiConfig(providerType = AiProviderType.GOOGLE_AI_STUDIO)
            }
        )
    )

    override val configState: StateFlow<AiConfigState> = _state.asStateFlow()

    override fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig {
        return _state.value.getEffectiveConfigFor(agentType)
    }

    override fun setAiAgentEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isAiAgentEnabled = enabled)
    }

    override fun selectGlobalProvider(type: AiProviderType) {
        _state.value = _state.value.copy(selectedGlobalProvider = type)
    }

    override fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(globalProviderConfigs = updated)
    }

    override fun saveAgentProvider(agentType: AiAgentType, providerType: AiProviderType) {
        val current = _state.value.getAgentConfig(agentType)
        val updated = _state.value.agentConfigs.toMutableMap()
        updated[agentType] = current.copy(providerType = providerType)
        _state.value = _state.value.copy(agentConfigs = updated)
    }

    override fun saveAgentFieldConfig(
        agentType: AiAgentType,
        providerType: AiProviderType,
        config: AiProviderConfig
    ) {
        val current = _state.value.getAgentConfig(agentType)
        val local = current.providerConfigs.toMutableMap()
        local[providerType] = config
        val updated = _state.value.agentConfigs.toMutableMap()
        updated[agentType] = current.copy(providerConfigs = local)
        _state.value = _state.value.copy(agentConfigs = updated)
    }

    override fun saveAgentConfig(
        agentType: AiAgentType,
        providerType: AiProviderType?,
        config: AiProviderConfig?
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
