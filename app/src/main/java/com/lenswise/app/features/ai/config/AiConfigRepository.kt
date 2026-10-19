package com.lenswise.app.features.ai.config

import android.content.Context
import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI 配置倉儲抽象介面 (IAiConfigRepository)。
 * 遵循依賴反轉原則 (DIP)，提供強型別狀態流與生效配置查詢。
 */
interface IAiConfigRepository {
    val configState: StateFlow<AiConfigState>
    fun getEffectiveConfigFor(agentType: AiAgentType): AgentAiConfig
    fun setAiAgentEnabled(enabled: Boolean)
    fun setEditingGlobalProvider(type: AiProviderType)
    fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsEditing: Boolean = false)
    fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType? = null, config: AiProviderConfig? = null)
}

/**
 * AI 代理與供應商配置倉儲 (AiConfigRepository)。
 * 透過 SharedPreferences 提供各 Agent 與全域供應商之強型別參數持久化。
 */
class AiConfigRepository(context: Context) : IAiConfigRepository {

    companion object {
        private const val PREFS_NAME = "lenswise_ai_config"
        private const val KEY_IS_AI_AGENT_ENABLED = "is_ai_agent_enabled"
        private const val KEY_GLOBAL_EDITING_PROVIDER = "ai_global_editing_provider"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _configState = MutableStateFlow(loadState())
    override val configState: StateFlow<AiConfigState> = _configState.asStateFlow()

    private fun loadState(): AiConfigState {
        val isEnabled = prefs.getBoolean(KEY_IS_AI_AGENT_ENABLED, true)
        val editingTypeStr = prefs.getString(KEY_GLOBAL_EDITING_PROVIDER, null)
        val editingType = AiProviderType.fromIdentifier(editingTypeStr)

        val globalConfigs = mutableMapOf<AiProviderType, AiProviderConfig>(
            AiProviderType.GEMINIFLOW to GeminiFlowConfig(),
            AiProviderType.OLLAMA to OllamaConfig(),
            AiProviderType.VERTEX_AI to VertexAiConfig()
        )

        // 讀取全域自訂配置
        prefs.getString("ai_global_geminiflow", null)?.let {
            runCatching { globalConfigs[AiProviderType.GEMINIFLOW] = json.decodeFromString<GeminiFlowConfig>(it) }
        }
        prefs.getString("ai_global_ollama", null)?.let {
            runCatching { globalConfigs[AiProviderType.OLLAMA] = json.decodeFromString<OllamaConfig>(it) }
        }
        prefs.getString("ai_global_vertexai", null)?.let {
            runCatching { globalConfigs[AiProviderType.VERTEX_AI] = json.decodeFromString<VertexAiConfig>(it) }
        }

        // 讀取各 Agent 自訂配置
        val agentConfigs = mutableMapOf<AiAgentType, AgentAiConfig>()
        for (agentType in AiAgentType.entries) {
            val prefix = "ai_agent_${agentType.id}"
            val activeProviderStr = prefs.getString("${prefix}_provider", null)
            val activeProvider = if (activeProviderStr != null) AiProviderType.fromIdentifier(activeProviderStr) else AiProviderType.GEMINIFLOW

            val localConfigs = mutableMapOf<AiProviderType, AiProviderConfig>(
                AiProviderType.GEMINIFLOW to GeminiFlowConfig.empty(),
                AiProviderType.OLLAMA to OllamaConfig.empty(),
                AiProviderType.VERTEX_AI to VertexAiConfig.empty()
            )

            prefs.getString("${prefix}_geminiflow", null)?.let {
                runCatching { localConfigs[AiProviderType.GEMINIFLOW] = json.decodeFromString<GeminiFlowConfig>(it) }
            }
            prefs.getString("${prefix}_ollama", null)?.let {
                runCatching { localConfigs[AiProviderType.OLLAMA] = json.decodeFromString<OllamaConfig>(it) }
            }
            prefs.getString("${prefix}_vertexai", null)?.let {
                runCatching { localConfigs[AiProviderType.VERTEX_AI] = json.decodeFromString<VertexAiConfig>(it) }
            }

            agentConfigs[agentType] = AgentAiConfig(
                activeProviderType = activeProvider,
                providerConfigs = localConfigs
            )
        }

        return AiConfigState(
            globalProviderConfigs = globalConfigs,
            editingGlobalProviderType = editingType,
            agentConfigs = agentConfigs,
            isAiAgentEnabled = isEnabled
        )
    }

    override fun setAiAgentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AI_AGENT_ENABLED, enabled).apply()
        _configState.value = _configState.value.copy(isAiAgentEnabled = enabled)
    }

    override fun setEditingGlobalProvider(type: AiProviderType) {
        prefs.edit().putString(KEY_GLOBAL_EDITING_PROVIDER, type.identifier).apply()
        _configState.value = _configState.value.copy(editingGlobalProviderType = type)
    }

    override fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsEditing: Boolean) {
        val editor = prefs.edit()
        when (config) {
            is GeminiFlowConfig -> editor.putString("ai_global_geminiflow", json.encodeToString(config))
            is OllamaConfig -> editor.putString("ai_global_ollama", json.encodeToString(config))
            is VertexAiConfig -> editor.putString("ai_global_vertexai", json.encodeToString(config))
        }
        if (setAsEditing) {
            editor.putString(KEY_GLOBAL_EDITING_PROVIDER, type.identifier)
        }
        editor.apply()

        val updatedGlobals = _configState.value.globalProviderConfigs.toMutableMap()
        updatedGlobals[type] = config
        _configState.value = _configState.value.copy(
            globalProviderConfigs = updatedGlobals,
            editingGlobalProviderType = if (setAsEditing) type else _configState.value.editingGlobalProviderType
        )
    }

    override fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType?, config: AiProviderConfig?) {
        val currentAgentConfig = _configState.value.getOverrideConfigFor(agentType)
        val newProviderType = providerType ?: currentAgentConfig.activeProviderType
        val updatedLocalConfigs = currentAgentConfig.providerConfigs.toMutableMap()

        if (config != null) {
            updatedLocalConfigs[newProviderType] = config
        }

        val prefix = "ai_agent_${agentType.id}"
        val editor = prefs.edit()
        if (providerType != null) {
            editor.putString("${prefix}_provider", newProviderType.identifier)
        }
        if (config != null) {
            when (config) {
                is GeminiFlowConfig -> editor.putString("${prefix}_geminiflow", json.encodeToString(config))
                is OllamaConfig -> editor.putString("${prefix}_ollama", json.encodeToString(config))
                is VertexAiConfig -> editor.putString("${prefix}_vertexai", json.encodeToString(config))
            }
        }
        editor.apply()

        val updatedAgentConfigs = _configState.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = AgentAiConfig(
            activeProviderType = newProviderType,
            providerConfigs = updatedLocalConfigs
        )
        _configState.value = _configState.value.copy(agentConfigs = updatedAgentConfigs)
    }

    override fun getEffectiveConfigFor(agentType: AiAgentType): AgentAiConfig {
        return _configState.value.getEffectiveConfigFor(agentType)
    }
}
