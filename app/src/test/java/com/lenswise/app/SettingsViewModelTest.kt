package com.lenswise.app

import com.lenswise.app.core.storage.SettingsPreferencesDataSource
import com.lenswise.app.domain.model.settings.TargetLanguage
import com.lenswise.app.presentation.features.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    private class FakeSettingsPreferencesDataSource(
        private var dark: Boolean = true,
        private var notif: Boolean = true,
        private var api: Boolean = true,
        private var lang: TargetLanguage = TargetLanguage.TRADITIONAL_CHINESE
    ) : SettingsPreferencesDataSource {
        override fun isDarkMode(): Boolean = dark
        override fun setDarkMode(enabled: Boolean) { dark = enabled }
        override fun isNotificationLogEnabled(): Boolean = notif
        override fun setNotificationLogEnabled(enabled: Boolean) { notif = enabled }
        override fun isApiLogEnabled(): Boolean = api
        override fun setApiLogEnabled(enabled: Boolean) { api = enabled }
        override fun getTargetLanguage(): TargetLanguage = lang
        override fun setTargetLanguage(language: TargetLanguage) { lang = language }
    }

    private class FakeAiConfigRepository(
        initialEnabled: Boolean = true
    ) : com.lenswise.app.features.ai.config.IAiConfigRepository {
        private val _state = kotlinx.coroutines.flow.MutableStateFlow(
            com.lenswise.app.features.ai.config.AiConfigState(isAiAgentEnabled = initialEnabled)
        )
        override val configState: kotlinx.coroutines.flow.StateFlow<com.lenswise.app.features.ai.config.AiConfigState> = _state
        override fun getEffectiveConfigFor(agentType: com.lenswise.app.features.ai.domain.AiAgentType) =
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
            agentType: com.lenswise.app.features.ai.domain.AiAgentType,
            customProviderType: com.lenswise.app.features.ai.config.AiProviderType?
        ) {
            val current = _state.value.agentConfigs[agentType] ?: com.lenswise.app.features.ai.config.AgentAiConfig()
            val updatedAgents = _state.value.agentConfigs.toMutableMap()
            updatedAgents[agentType] = current.copy(customProviderType = customProviderType)
            _state.value = _state.value.copy(agentConfigs = updatedAgents)
        }
        override fun saveAgentFieldConfig(
            agentType: com.lenswise.app.features.ai.domain.AiAgentType,
            providerType: com.lenswise.app.features.ai.config.AiProviderType,
            config: com.lenswise.app.features.ai.config.AiProviderConfig
        ) {
            val current = _state.value.agentConfigs[agentType] ?: com.lenswise.app.features.ai.config.AgentAiConfig()
            val localConfigs = current.providerConfigs.toMutableMap()
            localConfigs[providerType] = config
            val updatedAgents = _state.value.agentConfigs.toMutableMap()
            updatedAgents[agentType] = current.copy(providerConfigs = localConfigs)
            _state.value = _state.value.copy(agentConfigs = updatedAgents)
        }
        override fun saveAgentConfig(
            agentType: com.lenswise.app.features.ai.domain.AiAgentType,
            providerType: com.lenswise.app.features.ai.config.AiProviderType?,
            config: com.lenswise.app.features.ai.config.AiProviderConfig?
        ) {
            val current = _state.value.agentConfigs[agentType] ?: com.lenswise.app.features.ai.config.AgentAiConfig()
            val localConfigs = current.providerConfigs.toMutableMap()
            val targetProvider = providerType ?: current.customProviderType ?: _state.value.globalActiveProviderType
            if (config != null) localConfigs[targetProvider] = config
            val updatedAgents = _state.value.agentConfigs.toMutableMap()
            updatedAgents[agentType] = current.copy(
                customProviderType = providerType ?: current.customProviderType,
                providerConfigs = localConfigs
            )
            _state.value = _state.value.copy(agentConfigs = updatedAgents)
        }
    }

    @Test
    fun testSettingsInitialStateRestoration() {
        val fakeSource = FakeSettingsPreferencesDataSource(
            dark = false,
            notif = true,
            api = false,
            lang = TargetLanguage.ENGLISH
        )
        val fakeAiRepo = FakeAiConfigRepository(initialEnabled = false)
        val viewModel = SettingsViewModel(fakeSource, fakeAiRepo)

        assertFalse(viewModel.uiState.value.isDarkMode)
        assertTrue(viewModel.uiState.value.isNotificationLogEnabled)
        assertFalse(viewModel.uiState.value.isApiLogEnabled)
        assertFalse(viewModel.uiState.value.isAiAgentEnabled)
        assertEquals(TargetLanguage.ENGLISH, viewModel.uiState.value.targetLanguage)
        assertEquals("English", viewModel.uiState.value.targetLanguage.displayName)
    }

    @Test
    fun testSettingsToggleUpdatesStateAndPersistence() {
        val fakeSource = FakeSettingsPreferencesDataSource()
        val fakeAiRepo = FakeAiConfigRepository(initialEnabled = true)
        val viewModel = SettingsViewModel(fakeSource, fakeAiRepo)

        viewModel.setDarkMode(false)
        assertFalse(viewModel.uiState.value.isDarkMode)
        assertFalse(fakeSource.isDarkMode())

        viewModel.setNotificationLogEnabled(false)
        assertFalse(viewModel.uiState.value.isNotificationLogEnabled)
        assertFalse(fakeSource.isNotificationLogEnabled())

        viewModel.setApiLogEnabled(false)
        assertFalse(viewModel.uiState.value.isApiLogEnabled)
        assertFalse(fakeSource.isApiLogEnabled())

        viewModel.setAiAgentEnabled(false)
        assertFalse(viewModel.uiState.value.isAiAgentEnabled)
        assertFalse(fakeAiRepo.configState.value.isAiAgentEnabled)

        viewModel.setAiAgentEnabled(true)
        assertTrue(viewModel.uiState.value.isAiAgentEnabled)
        assertTrue(fakeAiRepo.configState.value.isAiAgentEnabled)

        viewModel.setTargetLanguage(TargetLanguage.JAPANESE)
        assertEquals(TargetLanguage.JAPANESE, viewModel.uiState.value.targetLanguage)
        assertEquals("日本語", viewModel.uiState.value.targetLanguage.displayName)
        assertEquals(TargetLanguage.JAPANESE, fakeSource.getTargetLanguage())

        viewModel.setTargetLanguage(TargetLanguage.KOREAN)
        assertEquals(TargetLanguage.KOREAN, viewModel.uiState.value.targetLanguage)
        assertEquals("한국어", viewModel.uiState.value.targetLanguage.displayName)
        assertEquals(TargetLanguage.KOREAN, fakeSource.getTargetLanguage())
    }
}
