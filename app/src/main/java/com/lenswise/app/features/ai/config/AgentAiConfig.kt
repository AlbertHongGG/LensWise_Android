package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * 個別 Agent 的 AI 引擎配置。
 */
data class AgentAiConfig(
    val activeProviderType: AiProviderType = AiProviderType.GEMINIFLOW,
    val providerConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createEmptyConfig() }
) {
    val activeConfig: AiProviderConfig
        get() = providerConfigs[activeProviderType] ?: activeProviderType.createEmptyConfig()

    fun getEffectiveConfig(globalConfigs: Map<AiProviderType, AiProviderConfig>): AgentAiConfig {
        val effectiveMap = mutableMapOf<AiProviderType, AiProviderConfig>()
        for (type in AiProviderType.entries) {
            val local = providerConfigs[type] ?: type.createEmptyConfig()
            val global = globalConfigs[type]
            effectiveMap[type] = if (global != null) local.fallbackTo(global) else local
        }
        return copy(providerConfigs = effectiveMap)
    }

    companion object {
        fun defaultConfig() = AgentAiConfig()
    }
}

/**
 * 全域 AI 設定聚合狀態。
 */
data class AiConfigState(
    val globalProviderConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createDefaultConfig() },
    val editingGlobalProviderType: AiProviderType = AiProviderType.GEMINIFLOW,
    val agentConfigs: Map<AiAgentType, AgentAiConfig> = emptyMap(),
    val isAiAgentEnabled: Boolean = true
) {
    val editingGlobalConfig: AiProviderConfig
        get() = globalProviderConfigs[editingGlobalProviderType] ?: editingGlobalProviderType.createDefaultConfig()

    fun getOverrideConfigFor(agentType: AiAgentType): AgentAiConfig {
        return agentConfigs[agentType] ?: AgentAiConfig.defaultConfig()
    }

    fun getEffectiveConfigFor(agentType: AiAgentType): AgentAiConfig {
        return getOverrideConfigFor(agentType).getEffectiveConfig(globalProviderConfigs)
    }
}
