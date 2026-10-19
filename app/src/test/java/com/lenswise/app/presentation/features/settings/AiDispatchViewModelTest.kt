package com.lenswise.app.presentation.features.settings

import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.GeminiFlowConfig
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
    fun `change global editing provider updates state`() {
        assertEquals(AiProviderType.GEMINIFLOW, viewModel.uiState.value.editingGlobalProviderType)

        viewModel.setEditingGlobalProvider(AiProviderType.OLLAMA)
        assertEquals(AiProviderType.OLLAMA, viewModel.uiState.value.editingGlobalProviderType)
    }

    @Test
    fun `update global field updates config correctly`() {
        viewModel.setEditingGlobalProvider(AiProviderType.OLLAMA)
        viewModel.updateGlobalField("baseUrl", "http://192.168.1.100:11434")

        val currentConfig = viewModel.uiState.value.globalProviderConfigs[AiProviderType.OLLAMA] as OllamaConfig
        assertEquals("http://192.168.1.100:11434", currentConfig.baseUrl)
    }

    @Test
    fun `save agent provider switches agent target provider`() {
        val targetAgent = AiAgentType.RECEIPT_SCANNER
        assertEquals(AiProviderType.GEMINIFLOW, viewModel.uiState.value.getEffectiveConfigFor(targetAgent).activeProviderType)

        viewModel.saveAgentProvider(targetAgent, AiProviderType.VERTEX_AI)
        assertEquals(AiProviderType.VERTEX_AI, viewModel.uiState.value.getEffectiveConfigFor(targetAgent).activeProviderType)
    }

    @Test
    fun `update agent field updates local agent override`() {
        val targetAgent = AiAgentType.RECEIPT_SCANNER
        viewModel.saveAgentProvider(targetAgent, AiProviderType.GEMINIFLOW)
        viewModel.updateAgentField(targetAgent, "modelName", "custom-model-2.5")

        val effective = viewModel.uiState.value.getEffectiveConfigFor(targetAgent)
        val config = effective.activeConfig as GeminiFlowConfig
        assertEquals("custom-model-2.5", config.modelName)
    }
}

private class FakeAiConfigRepository : IAiConfigRepository {
    private val _state = MutableStateFlow(
        AiConfigState(
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(),
                AiProviderType.OLLAMA to OllamaConfig(),
                AiProviderType.VERTEX_AI to VertexAiConfig()
            ),
            editingGlobalProviderType = AiProviderType.GEMINIFLOW,
            agentConfigs = AiAgentType.entries.associateWith { AgentAiConfig() },
            isAiAgentEnabled = true
        )
    )
    override val configState: StateFlow<AiConfigState> = _state.asStateFlow()

    override fun getEffectiveConfigFor(agentType: AiAgentType): AgentAiConfig =
        _state.value.getEffectiveConfigFor(agentType)

    override fun setAiAgentEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isAiAgentEnabled = enabled)
    }

    override fun setEditingGlobalProvider(type: AiProviderType) {
        _state.value = _state.value.copy(editingGlobalProviderType = type)
    }

    override fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsEditing: Boolean) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(
            globalProviderConfigs = updated,
            editingGlobalProviderType = if (setAsEditing) type else _state.value.editingGlobalProviderType
        )
    }

    override fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType?, config: AiProviderConfig?) {
        val current = _state.value.getOverrideConfigFor(agentType)
        val newProvider = providerType ?: current.activeProviderType
        val localConfigs = current.providerConfigs.toMutableMap()
        if (config != null) localConfigs[newProvider] = config
        val updatedAgents = _state.value.agentConfigs.toMutableMap()
        updatedAgents[agentType] = AgentAiConfig(
            activeProviderType = newProvider,
            providerConfigs = localConfigs
        )
        _state.value = _state.value.copy(agentConfigs = updatedAgents)
    }
}
