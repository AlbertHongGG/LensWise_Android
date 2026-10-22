package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigRepositoryTest {

    @Test
    fun testDefaultAiConfigState_hasDefaultGlobalProfiles() {
        val state = AiConfigState()
        assertTrue(state.isAiAgentEnabled)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, state.editingGlobalProfileType)

        val studioConfig = state.globalProviderConfigs[AiProviderType.GOOGLE_AI_STUDIO] as? GoogleAiStudioConfig
        assertNotNull(studioConfig)
        assertEquals(GoogleAiStudioModel.FLASH_3_8, studioConfig?.modelName)

        val geminiConfig = state.globalProviderConfigs[AiProviderType.GEMINIFLOW] as? GeminiFlowConfig
        assertNotNull(geminiConfig)
        assertEquals("http://127.0.0.1:5000", geminiConfig?.baseUrl)
        assertEquals(GeminiFlowModel.FLASH, geminiConfig?.modelName)
    }

    @Test
    fun testAgentExplicitProviderSelection_parameterFallbackToGlobalProfile() {
        val state = AiConfigState(
            editingGlobalProfileType = AiProviderType.GOOGLE_AI_STUDIO,
            globalProviderConfigs = mapOf(
                AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                    apiKey = "AIzaSyGlobalStudioKey",
                    modelName = GoogleAiStudioModel.FLASH_3_8,
                    thinkingLevel = "medium"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    providerType = AiProviderType.GOOGLE_AI_STUDIO,
                    providerConfigs = mapOf(
                        AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig.empty() // 本地參數皆為空，應繼承全域 Profile
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, effective.effectiveProviderType)

        val active = effective.activeConfig as GoogleAiStudioConfig
        assertEquals("AIzaSyGlobalStudioKey", active.apiKey)
        assertEquals(GoogleAiStudioModel.FLASH_3_8, active.modelName)
        assertEquals("medium", active.thinkingLevel)
    }

    @Test
    fun testAgentParameterOverride_takesPrecedenceOverGlobalProfile() {
        val state = AiConfigState(
            globalProviderConfigs = mapOf(
                AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                    apiKey = "AIzaSyGlobalKey",
                    modelName = GoogleAiStudioModel.FLASH_3_8,
                    thinkingLevel = "low"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    providerType = AiProviderType.GOOGLE_AI_STUDIO,
                    providerConfigs = mapOf(
                        AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                            apiKey = "AIzaSyAgentCustomKey",
                            modelName = GoogleAiStudioModel.FLASH_3_6,
                            thinkingLevel = "high"
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GoogleAiStudioConfig

        // 局部覆蓋優先
        assertEquals("AIzaSyAgentCustomKey", active.apiKey)
        assertEquals(GoogleAiStudioModel.FLASH_3_6, active.modelName)
        assertEquals("high", active.thinkingLevel)
    }

    @Test
    fun testPartialParameterFallback_agentOverridesModelButInheritsApiKey() {
        val state = AiConfigState(
            globalProviderConfigs = mapOf(
                AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                    apiKey = "AIzaSyGlobalKey123",
                    modelName = GoogleAiStudioModel.FLASH_3_8
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    providerType = AiProviderType.GOOGLE_AI_STUDIO,
                    providerConfigs = mapOf(
                        AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                            apiKey = "", // 空白代表繼承全域
                            modelName = GoogleAiStudioModel.FLASH_LITE_3_5
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GoogleAiStudioConfig

        assertEquals(GoogleAiStudioModel.FLASH_LITE_3_5, active.modelName)
        assertEquals("AIzaSyGlobalKey123", active.apiKey)
        // 嚴格校驗應通過
        active.validateRequiredFields(AiAgentType.RECEIPT_SCANNER, AiProviderType.GOOGLE_AI_STUDIO)
    }

    @Test
    fun testValidateRequiredFields_throwsWhenBothAgentAndGlobalEmpty_apiKey() {
        val state = AiConfigState(
            globalProviderConfigs = mapOf(
                AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                    apiKey = "",
                    modelName = GoogleAiStudioModel.FLASH_3_8
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    providerType = AiProviderType.GOOGLE_AI_STUDIO,
                    providerConfigs = mapOf(
                        AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                            apiKey = "",
                            modelName = GoogleAiStudioModel.FLASH_3_8
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GoogleAiStudioConfig

        try {
            active.validateRequiredFields(AiAgentType.RECEIPT_SCANNER, AiProviderType.GOOGLE_AI_STUDIO)
            org.junit.Assert.fail("Should have thrown MissingAiConfigException when apiKey is empty")
        } catch (e: MissingAiConfigException) {
            assertEquals(AiAgentType.RECEIPT_SCANNER, e.agentType)
            assertEquals("apiKey", e.fieldKey)
            assertTrue(e.message?.contains("未指定 API Key") == true)
            assertTrue(e.message?.contains("Google AI Studio") == true)
        }
    }

    @Test
    fun testAiProviderFactory_throwsWhenAgentDisabled() {
        val fakeRepo = TestAiConfigRepository(AiConfigState(isAiAgentEnabled = false))

        try {
            com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
                AiAgentType.RECEIPT_SCANNER,
                fakeRepo
            )
            org.junit.Assert.fail("Should have thrown IllegalStateException when AI agent is disabled")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("AI Agent 功能已停用") == true)
        }
    }

    @Test
    fun testAiProviderFactory_createsGoogleAiStudioProviderWhenValid() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                        apiKey = "AIzaSyTestStudioKey123",
                        modelName = GoogleAiStudioModel.FLASH_3_8
                    )
                ),
                agentConfigs = mapOf(
                    AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                        providerType = AiProviderType.GOOGLE_AI_STUDIO
                    )
                )
            )
        )

        val provider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("google_ai_studio", provider.name)
    }

    @Test
    fun testAiProviderFactory_throwsWhenRequiredFieldMissingInBothAgentAndGlobal() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig.empty()
                ),
                agentConfigs = mapOf(
                    AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                        providerType = AiProviderType.GOOGLE_AI_STUDIO,
                        providerConfigs = mapOf(
                            AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig.empty()
                        )
                    )
                )
            )
        )

        try {
            com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
                AiAgentType.RECEIPT_SCANNER,
                fakeRepo
            )
            org.junit.Assert.fail("Should have thrown MissingAiConfigException")
        } catch (e: MissingAiConfigException) {
            assertEquals(AiAgentType.RECEIPT_SCANNER, e.agentType)
            assertTrue(e.message?.contains("未指定 API Key") == true)
        }
    }

    @Test
    fun testAgentCanIndependentlySwitchProviders() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                        apiKey = "AIzaSyGlobalKey",
                        modelName = GoogleAiStudioModel.FLASH_3_8
                    ),
                    AiProviderType.VERTEX_AI to VertexAiConfig(
                        projectId = "gcp-proj",
                        location = "us-central1",
                        apiKey = "AIzaSyVertexKey",
                        modelName = "gemini-2.5-flash"
                    )
                ),
                agentConfigs = mapOf(
                    AiAgentType.RECEIPT_SCANNER to AgentAiConfig(providerType = AiProviderType.GOOGLE_AI_STUDIO)
                )
            )
        )

        // 初始：Google AI Studio
        val initialProvider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("google_ai_studio", initialProvider.name)

        // 指派為 Vertex AI
        fakeRepo.saveAgentProvider(AiAgentType.RECEIPT_SCANNER, AiProviderType.VERTEX_AI)
        val updatedEffective = fakeRepo.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.VERTEX_AI, updatedEffective.effectiveProviderType)

        val updatedProvider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("vertexai", updatedProvider.name)
    }

    @Test
    fun testGoogleAiStudioModelCatalog_hasOnlySpecifiedThreeModels() {
        val models = GoogleAiStudioModel.ITEMS
        assertEquals(3, models.size)
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite"), models.map { it.id })
        assertEquals("Gemini 3.8 Flash", GoogleAiStudioModel.getDisplayName("gemini-3.8-flash"))
        assertEquals("Gemini 3.6 Flash", GoogleAiStudioModel.getDisplayName("gemini-3.6-flash"))
        assertEquals("Gemini 3.5 Flash Lite", GoogleAiStudioModel.getDisplayName("gemini-3.5-flash-lite"))
    }

    @Test
    fun testConfigProperty_hasZeroParenthesesInAllDescriptors() {
        val properties = listOf(
            ConfigProperty.API_KEY,
            ConfigProperty.API_KEY_OR_TOKEN,
            ConfigProperty.BASE_URL,
            ConfigProperty.ENDPOINT_URL,
            ConfigProperty.PROJECT_ID,
            ConfigProperty.LOCATION,
            ConfigProperty.MODEL,
            ConfigProperty.MODEL_TAG,
            ConfigProperty.THINKING_LEVEL
        )
        for (prop in properties) {
            assertFalse("Label should not have parentheses: ${prop.label}", prop.label.contains("(") || prop.label.contains("（"))
            assertFalse("Placeholder should not have parentheses: ${prop.placeholder}", prop.placeholder.contains("(") || prop.placeholder.contains("（"))
        }
    }
}

private class TestAiConfigRepository(
    initialState: AiConfigState
) : IAiConfigRepository {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(initialState)
    override val configState: kotlinx.coroutines.flow.StateFlow<AiConfigState> = _state

    override fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig =
        _state.value.getEffectiveConfigFor(agentType)

    override fun setAiAgentEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isAiAgentEnabled = enabled)
    }

    override fun setEditingGlobalProfile(type: AiProviderType) {
        _state.value = _state.value.copy(editingGlobalProfileType = type)
    }

    override fun saveGlobalProfileConfig(type: AiProviderType, config: AiProviderConfig) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(globalProviderConfigs = updated)
    }

    override fun saveAgentProvider(agentType: AiAgentType, providerType: AiProviderType) {
        val current = _state.value.getAgentConfig(agentType)
        val updatedAgents = _state.value.agentConfigs.toMutableMap()
        updatedAgents[agentType] = current.copy(providerType = providerType)
        _state.value = _state.value.copy(agentConfigs = updatedAgents)
    }

    override fun saveAgentFieldConfig(agentType: AiAgentType, providerType: AiProviderType, config: AiProviderConfig) {
        val current = _state.value.getAgentConfig(agentType)
        val localConfigs = current.providerConfigs.toMutableMap()
        localConfigs[providerType] = config
        val updatedAgents = _state.value.agentConfigs.toMutableMap()
        updatedAgents[agentType] = current.copy(providerConfigs = localConfigs)
        _state.value = _state.value.copy(agentConfigs = updatedAgents)
    }

    override fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType?, config: AiProviderConfig?) {
        if (providerType != null) {
            saveAgentProvider(agentType, providerType)
        }
        if (config != null) {
            val target = providerType ?: _state.value.getAgentConfig(agentType).providerType
            saveAgentFieldConfig(agentType, target, config)
        }
    }
}
