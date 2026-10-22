package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigRepositoryTest {

    @Test
    fun testDefaultAiConfigState_hasDefaultGlobalProviders() {
        val state = AiConfigState()
        assertTrue(state.isAiAgentEnabled)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, state.globalActiveProviderType)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, state.editingGlobalProviderType)

        val geminiConfig = state.globalProviderConfigs[AiProviderType.GEMINIFLOW] as? GeminiFlowConfig
        assertNotNull(geminiConfig)
        assertEquals("http://127.0.0.1:5000", geminiConfig?.baseUrl)
        assertEquals(GeminiFlowModel.FLASH, geminiConfig?.modelName)
    }

    @Test
    fun testEffectiveConfigFallback_inheritsGlobalWhenAgentConfigEmpty() {
        val state = AiConfigState(
            globalActiveProviderType = AiProviderType.GEMINIFLOW,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                    modelName = "gemini-2.5-pro",
                    baseUrl = "http://my-custom-flow:5000",
                    thinkingLevel = "high"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    customProviderType = null, // 跟隨全域
                    providerConfigs = mapOf(
                        AiProviderType.GEMINIFLOW to GeminiFlowConfig.empty()
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.GEMINIFLOW, effective.effectiveProviderType)
        assertTrue(effective.isFollowingGlobal)

        val active = effective.activeConfig as GeminiFlowConfig
        // 應繼承自全域設定
        assertEquals("gemini-2.5-pro", active.modelName)
        assertEquals("http://my-custom-flow:5000", active.baseUrl)
        assertEquals("high", active.thinkingLevel)
    }

    @Test
    fun testEffectiveConfigOverride_takesPrecedenceOverGlobal() {
        val state = AiConfigState(
            globalActiveProviderType = AiProviderType.GEMINIFLOW,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                    modelName = "gemini-2.5-flash",
                    baseUrl = "http://127.0.0.1:5000"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_CHAT to AgentAiConfig(
                    customProviderType = AiProviderType.GEMINIFLOW,
                    providerConfigs = mapOf(
                        AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                            modelName = "gemini-2.5-pro",
                            baseUrl = "http://chat-server:5000"
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_CHAT)
        val active = effective.activeConfig as GeminiFlowConfig

        // 局部覆蓋應優先
        assertEquals("gemini-2.5-pro", active.modelName)
        assertEquals("http://chat-server:5000", active.baseUrl)
    }

    @Test
    fun testPartialInheritance_agentOverridesModelButInheritsBaseUrl() {
        val state = AiConfigState(
            globalActiveProviderType = AiProviderType.GEMINIFLOW,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                    modelName = "gemini-3.8-flash",
                    baseUrl = "http://global-server:5000"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    customProviderType = AiProviderType.GEMINIFLOW,
                    providerConfigs = mapOf(
                        AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                            modelName = "gemini-3-pro",
                            baseUrl = ""
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GeminiFlowConfig

        // modelName 採用 Agent 設定，baseUrl 繼承自 Global
        assertEquals("gemini-3-pro", active.modelName)
        assertEquals("http://global-server:5000", active.baseUrl)
        // 嚴格校驗應順利通過
        active.validateRequiredFields(AiAgentType.RECEIPT_SCANNER)
    }

    @Test
    fun testValidateRequiredFields_throwsWhenBothAgentAndGlobalEmpty_modelName() {
        val state = AiConfigState(
            globalActiveProviderType = AiProviderType.GEMINIFLOW,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                    modelName = "",
                    baseUrl = "http://127.0.0.1:5000"
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    customProviderType = AiProviderType.GEMINIFLOW,
                    providerConfigs = mapOf(
                        AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                            modelName = "",
                            baseUrl = ""
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GeminiFlowConfig

        try {
            active.validateRequiredFields(AiAgentType.RECEIPT_SCANNER)
            org.junit.Assert.fail("Should have thrown MissingAiConfigException for empty modelName")
        } catch (e: MissingAiConfigException) {
            assertEquals(AiAgentType.RECEIPT_SCANNER, e.agentType)
            assertEquals("modelName", e.fieldKey)
            assertTrue(e.message?.contains("未指定模型") == true)
        }
    }

    @Test
    fun testValidateRequiredFields_throwsWhenBothAgentAndGlobalEmpty_baseUrl() {
        val state = AiConfigState(
            globalActiveProviderType = AiProviderType.GEMINIFLOW,
            globalProviderConfigs = mapOf(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                    modelName = "gemini-3.8-flash",
                    baseUrl = ""
                )
            ),
            agentConfigs = mapOf(
                AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                    customProviderType = AiProviderType.GEMINIFLOW,
                    providerConfigs = mapOf(
                        AiProviderType.GEMINIFLOW to GeminiFlowConfig(
                            modelName = "",
                            baseUrl = ""
                        )
                    )
                )
            )
        )

        val effective = state.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        val active = effective.activeConfig as GeminiFlowConfig

        try {
            active.validateRequiredFields(AiAgentType.RECEIPT_SCANNER)
            org.junit.Assert.fail("Should have thrown MissingAiConfigException for empty baseUrl")
        } catch (e: MissingAiConfigException) {
            assertEquals(AiAgentType.RECEIPT_SCANNER, e.agentType)
            assertEquals("baseUrl", e.fieldKey)
            assertTrue(e.message?.contains("未指定伺服器位址") == true)
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
    fun testAiProviderFactory_throwsWhenRequiredFieldMissing() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                globalActiveProviderType = AiProviderType.GEMINIFLOW,
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GEMINIFLOW to GeminiFlowConfig.empty()
                ),
                agentConfigs = mapOf(
                    AiAgentType.RECEIPT_SCANNER to AgentAiConfig(
                        customProviderType = AiProviderType.GEMINIFLOW,
                        providerConfigs = mapOf(
                            AiProviderType.GEMINIFLOW to GeminiFlowConfig.empty()
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
            assertTrue(e.message?.contains("未指定") == true)
        }
    }

    @Test
    fun testGlobalProviderChange_automaticallyChangesAllFollowingAgents() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                globalActiveProviderType = AiProviderType.GOOGLE_AI_STUDIO,
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                        apiKey = "AIzaSyTestStudioKey123",
                        modelName = "gemini-3.8-flash"
                    )
                ),
                agentConfigs = mapOf(
                    AiAgentType.RECEIPT_SCANNER to AgentAiConfig() // default: customProviderType = null
                )
            )
        )

        // 1. 驗證預設即跟隨全域 Google AI Studio
        val effective = fakeRepo.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, effective.effectiveProviderType)
        assertTrue(effective.isFollowingGlobal)

        val provider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        // 絕對不得為 geminiflow (localhost)！
        assertEquals("google_ai_studio", provider.name)

        // 2. 切換全域為 Vertex AI
        fakeRepo.saveGlobalProviderConfig(
            AiProviderType.VERTEX_AI,
            VertexAiConfig(projectId = "my-p", location = "us", apiKey = "my-k", modelName = "gemini-2.5-flash"),
            setAsActive = true
        )

        // 驗證未自訂的 Agent 自動同步切換至 Vertex AI
        val updatedEffective = fakeRepo.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.VERTEX_AI, updatedEffective.effectiveProviderType)
        assertTrue(updatedEffective.isFollowingGlobal)

        val updatedProvider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("vertexai", updatedProvider.name)
    }

    @Test
    fun testAgentCustomOverride_preemptsGlobalProvider() {
        val fakeRepo = TestAiConfigRepository(
            AiConfigState(
                globalActiveProviderType = AiProviderType.GOOGLE_AI_STUDIO,
                isAiAgentEnabled = true,
                globalProviderConfigs = mapOf(
                    AiProviderType.GOOGLE_AI_STUDIO to GoogleAiStudioConfig(
                        apiKey = "AIzaSyGlobalKey",
                        modelName = "gemini-3.8-flash"
                    ),
                    AiProviderType.VERTEX_AI to VertexAiConfig(
                        projectId = "gcp-proj",
                        location = "us-central1",
                        apiKey = "AIzaSyVertexKey",
                        modelName = "gemini-2.5-flash"
                    )
                )
            )
        )

        // 個別覆蓋 RECEIPT_SCANNER 為 Vertex AI
        fakeRepo.saveAgentProvider(AiAgentType.RECEIPT_SCANNER, AiProviderType.VERTEX_AI)

        val effective = fakeRepo.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.VERTEX_AI, effective.effectiveProviderType)
        assertFalse(effective.isFollowingGlobal)

        val provider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("vertexai", provider.name)

        // 還原為跟隨全域 (傳入 null)
        fakeRepo.saveAgentProvider(AiAgentType.RECEIPT_SCANNER, null)
        val revertedEffective = fakeRepo.getEffectiveConfigFor(AiAgentType.RECEIPT_SCANNER)
        assertEquals(AiProviderType.GOOGLE_AI_STUDIO, revertedEffective.effectiveProviderType)
        assertTrue(revertedEffective.isFollowingGlobal)

        val revertedProvider = com.lenswise.app.core.ai.provider.AiProviderFactory.createProviderForAgent(
            AiAgentType.RECEIPT_SCANNER,
            fakeRepo
        )
        assertEquals("google_ai_studio", revertedProvider.name)
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
    fun testGoogleAiStudioConfig_validateRequiredFields_throwsWhenApiKeyMissing() {
        val config = GoogleAiStudioConfig(
            apiKey = "",
            modelName = "gemini-3.8-flash"
        )
        try {
            config.validateRequiredFields(AiAgentType.RECEIPT_SCANNER)
            org.junit.Assert.fail("Expected MissingAiConfigException when apiKey is blank")
        } catch (e: MissingAiConfigException) {
            assertEquals("apiKey", e.fieldKey)
        }
    }

    @Test
    fun testGoogleAiStudioConfig_fallbackTo_inheritsGlobalSettings() {
        val global = GoogleAiStudioConfig(
            apiKey = "AIzaSyGlobalKey123",
            modelName = "gemini-3.8-flash",
            thinkingLevel = "medium"
        )
        val localEmpty = GoogleAiStudioConfig.empty()
        val effective = localEmpty.fallbackTo(global) as GoogleAiStudioConfig

        assertEquals("AIzaSyGlobalKey123", effective.apiKey)
        assertEquals("gemini-3.8-flash", effective.modelName)
        assertEquals("medium", effective.thinkingLevel)
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

    override fun setGlobalProvider(type: AiProviderType) {
        _state.value = _state.value.copy(globalActiveProviderType = type)
    }

    override fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsActive: Boolean) {
        val updated = _state.value.globalProviderConfigs.toMutableMap()
        updated[type] = config
        _state.value = _state.value.copy(
            globalProviderConfigs = updated,
            globalActiveProviderType = if (setAsActive) type else _state.value.globalActiveProviderType
        )
    }

    override fun saveAgentProvider(agentType: AiAgentType, customProviderType: AiProviderType?) {
        val current = _state.value.getOverrideConfigFor(agentType)
        val updatedAgents = _state.value.agentConfigs.toMutableMap()
        updatedAgents[agentType] = current.copy(customProviderType = customProviderType)
        _state.value = _state.value.copy(agentConfigs = updatedAgents)
    }

    override fun saveAgentFieldConfig(agentType: AiAgentType, providerType: AiProviderType, config: AiProviderConfig) {
        val current = _state.value.getOverrideConfigFor(agentType)
        val localConfigs = current.providerConfigs.toMutableMap()
        localConfigs[providerType] = config
        val updatedAgents = _state.value.agentConfigs.toMutableMap()
        updatedAgents[agentType] = current.copy(providerConfigs = localConfigs)
        _state.value = _state.value.copy(agentConfigs = updatedAgents)
    }

    override fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType?, config: AiProviderConfig?) {
        if (providerType != null && config == null) {
            saveAgentProvider(agentType, providerType)
        } else if (config != null) {
            val target = providerType ?: _state.value.getEffectiveConfigFor(agentType).effectiveProviderType
            saveAgentFieldConfig(agentType, target, config)
        }
    }
}
