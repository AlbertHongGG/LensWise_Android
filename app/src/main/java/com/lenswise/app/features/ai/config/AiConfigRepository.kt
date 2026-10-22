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
 * 遵循依賴反轉原則 (DIP)，提供強型別狀態流與階層式生效配置查詢。
 */
interface IAiConfigRepository {
    val configState: StateFlow<AiConfigState>
    fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig
    fun setAiAgentEnabled(enabled: Boolean)
    fun setGlobalProvider(type: AiProviderType)
    fun setEditingGlobalProvider(type: AiProviderType) = setGlobalProvider(type)
    fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsActive: Boolean = false)
    fun saveAgentProvider(agentType: AiAgentType, customProviderType: AiProviderType?)
    fun saveAgentFieldConfig(agentType: AiAgentType, providerType: AiProviderType, config: AiProviderConfig)
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
        private const val KEY_GLOBAL_ACTIVE_PROVIDER = "ai_global_active_provider"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _configState = MutableStateFlow(loadState())
    override val configState: StateFlow<AiConfigState> = _configState.asStateFlow()

    private fun serializeConfig(config: AiProviderConfig): String {
        return when (config) {
            is GeminiFlowConfig -> json.encodeToString(config)
            is GoogleAiStudioConfig -> json.encodeToString(config)
            is OllamaConfig -> json.encodeToString(config)
            is VertexAiConfig -> json.encodeToString(config)
        }
    }

    private fun deserializeConfig(type: AiProviderType, rawJson: String): AiProviderConfig? {
        return runCatching {
            when (type) {
                AiProviderType.GEMINIFLOW -> json.decodeFromString<GeminiFlowConfig>(rawJson)
                AiProviderType.GOOGLE_AI_STUDIO -> json.decodeFromString<GoogleAiStudioConfig>(rawJson)
                AiProviderType.OLLAMA -> json.decodeFromString<OllamaConfig>(rawJson)
                AiProviderType.VERTEX_AI -> json.decodeFromString<VertexAiConfig>(rawJson)
            }
        }.getOrNull()
    }

    private fun loadState(): AiConfigState {
        val isEnabled = prefs.getBoolean(KEY_IS_AI_AGENT_ENABLED, true)
        val globalProviderStr = prefs.getString(KEY_GLOBAL_ACTIVE_PROVIDER, null)
            ?: prefs.getString("ai_global_editing_provider", null)
        val globalActiveProvider = if (globalProviderStr != null) {
            AiProviderType.fromIdentifier(globalProviderStr)
        } else {
            AiProviderType.GOOGLE_AI_STUDIO
        }

        val globalConfigs = mutableMapOf<AiProviderType, AiProviderConfig>()
        for (type in AiProviderType.entries) {
            val key = "ai_global_${type.identifier}"
            val savedJson = prefs.getString(key, null)
            val parsed = if (savedJson != null) deserializeConfig(type, savedJson) else null
            globalConfigs[type] = parsed ?: type.createDefaultConfig()
        }

        // 讀取各 Agent 自訂配置
        val agentConfigs = mutableMapOf<AiAgentType, AgentAiConfig>()
        for (agentType in AiAgentType.entries) {
            val prefix = "ai_agent_${agentType.id}"
            // 若為 null 或 "follow_global"，則代表跟隨全域！
            val customProviderStr = prefs.getString("${prefix}_custom_provider", null)
            val customProvider = if (customProviderStr != null && customProviderStr != "follow_global") {
                AiProviderType.fromIdentifier(customProviderStr)
            } else {
                null
            }

            val localConfigs = mutableMapOf<AiProviderType, AiProviderConfig>()
            for (type in AiProviderType.entries) {
                val key = "${prefix}_${type.identifier}"
                val savedJson = prefs.getString(key, null)
                val parsed = if (savedJson != null) deserializeConfig(type, savedJson) else null
                localConfigs[type] = parsed ?: type.createEmptyConfig()
            }

            agentConfigs[agentType] = AgentAiConfig(
                customProviderType = customProvider,
                providerConfigs = localConfigs
            )
        }

        return AiConfigState(
            globalActiveProviderType = globalActiveProvider,
            globalProviderConfigs = globalConfigs,
            agentConfigs = agentConfigs,
            isAiAgentEnabled = isEnabled
        )
    }

    override fun setAiAgentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AI_AGENT_ENABLED, enabled).apply()
        _configState.value = _configState.value.copy(isAiAgentEnabled = enabled)
    }

    override fun setGlobalProvider(type: AiProviderType) {
        prefs.edit().putString(KEY_GLOBAL_ACTIVE_PROVIDER, type.identifier).apply()
        _configState.value = _configState.value.copy(globalActiveProviderType = type)
    }

    override fun saveGlobalProviderConfig(type: AiProviderType, config: AiProviderConfig, setAsActive: Boolean) {
        val editor = prefs.edit()
        val key = "ai_global_${type.identifier}"
        editor.putString(key, serializeConfig(config))
        if (setAsActive) {
            editor.putString(KEY_GLOBAL_ACTIVE_PROVIDER, type.identifier)
        }
        editor.apply()

        val updatedGlobals = _configState.value.globalProviderConfigs.toMutableMap()
        updatedGlobals[type] = config
        _configState.value = _configState.value.copy(
            globalProviderConfigs = updatedGlobals,
            globalActiveProviderType = if (setAsActive) type else _configState.value.globalActiveProviderType
        )
    }

    override fun saveAgentProvider(agentType: AiAgentType, customProviderType: AiProviderType?) {
        val prefix = "ai_agent_${agentType.id}"
        val editor = prefs.edit()
        if (customProviderType != null) {
            editor.putString("${prefix}_custom_provider", customProviderType.identifier)
        } else {
            editor.putString("${prefix}_custom_provider", "follow_global")
        }
        editor.apply()

        val currentAgentConfig = _configState.value.getOverrideConfigFor(agentType)
        val updatedAgentConfigs = _configState.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = currentAgentConfig.copy(customProviderType = customProviderType)
        _configState.value = _configState.value.copy(agentConfigs = updatedAgentConfigs)
    }

    override fun saveAgentFieldConfig(agentType: AiAgentType, providerType: AiProviderType, config: AiProviderConfig) {
        val prefix = "ai_agent_${agentType.id}"
        val editor = prefs.edit()
        editor.putString("${prefix}_${providerType.identifier}", serializeConfig(config))
        editor.apply()

        val currentAgentConfig = _configState.value.getOverrideConfigFor(agentType)
        val updatedLocalConfigs = currentAgentConfig.providerConfigs.toMutableMap()
        updatedLocalConfigs[providerType] = config

        val updatedAgentConfigs = _configState.value.agentConfigs.toMutableMap()
        updatedAgentConfigs[agentType] = currentAgentConfig.copy(providerConfigs = updatedLocalConfigs)
        _configState.value = _configState.value.copy(agentConfigs = updatedAgentConfigs)
    }

    override fun saveAgentConfig(agentType: AiAgentType, providerType: AiProviderType?, config: AiProviderConfig?) {
        if (providerType != null && config == null) {
            saveAgentProvider(agentType, providerType)
        } else if (config != null) {
            val targetProvider = providerType ?: _configState.value.getEffectiveConfigFor(agentType).effectiveProviderType
            saveAgentFieldConfig(agentType, targetProvider, config)
        }
    }

    override fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig {
        return _configState.value.getEffectiveConfigFor(agentType)
    }
}
