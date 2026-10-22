package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * 生效之 Agent AI 配置領域模型 (EffectiveAgentAiConfig)。
 * 不可變值物件，完整封裝解析完成之生效引擎、生效參數與全域跟隨狀態。
 */
data class EffectiveAgentAiConfig(
    val effectiveProviderType: AiProviderType,
    val activeConfig: AiProviderConfig,
    val isFollowingGlobal: Boolean,
    val providerConfigs: Map<AiProviderType, AiProviderConfig>
) {
    val activeProviderType: AiProviderType
        get() = effectiveProviderType
}

/**
 * 個別 Agent 的 AI 引擎配置。
 * 遵循雙層繼承架構：
 * 1. 引擎層級：[customProviderType] 為 null 時，100% 動態繼承全域生效引擎；非 null 時為個別獨立覆蓋。
 * 2. 參數層級：[providerConfigs] 儲存該 Agent 針對各 Provider 的覆寫值，空白欄位自動 Fallback 繼承全域該引擎設定。
 */
data class AgentAiConfig(
    val customProviderType: AiProviderType? = null,
    val providerConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createEmptyConfig() }
) {
    val isFollowingGlobal: Boolean
        get() = customProviderType == null

    fun resolveEffective(
        globalProviderType: AiProviderType,
        globalConfigs: Map<AiProviderType, AiProviderConfig>
    ): EffectiveAgentAiConfig {
        val effectiveType = customProviderType ?: globalProviderType
        val effectiveConfigs = AiProviderType.entries.associateWith { type ->
            val local = providerConfigs[type] ?: type.createEmptyConfig()
            val global = globalConfigs[type]
            if (global != null) local.fallbackTo(global) else local
        }
        val activeCfg = effectiveConfigs[effectiveType] ?: effectiveType.createDefaultConfig()

        return EffectiveAgentAiConfig(
            effectiveProviderType = effectiveType,
            activeConfig = activeCfg,
            isFollowingGlobal = isFollowingGlobal,
            providerConfigs = effectiveConfigs
        )
    }

    companion object {
        fun defaultConfig() = AgentAiConfig()
    }
}

/**
 * 全域 AI 設定聚合狀態。
 * 封裝全域生效引擎 [globalActiveProviderType]、各引擎全域配置與各 Agent 調度路由。
 */
data class AiConfigState(
    val globalActiveProviderType: AiProviderType = AiProviderType.GOOGLE_AI_STUDIO,
    val globalProviderConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createDefaultConfig() },
    val agentConfigs: Map<AiAgentType, AgentAiConfig> = emptyMap(),
    val isAiAgentEnabled: Boolean = true
) {
    val editingGlobalProviderType: AiProviderType
        get() = globalActiveProviderType

    val editingGlobalConfig: AiProviderConfig
        get() = globalProviderConfigs[globalActiveProviderType] ?: globalActiveProviderType.createDefaultConfig()

    fun getOverrideConfigFor(agentType: AiAgentType): AgentAiConfig {
        return agentConfigs[agentType] ?: AgentAiConfig.defaultConfig()
    }

    fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig {
        return getOverrideConfigFor(agentType).resolveEffective(
            globalProviderType = globalActiveProviderType,
            globalConfigs = globalProviderConfigs
        )
    }
}
