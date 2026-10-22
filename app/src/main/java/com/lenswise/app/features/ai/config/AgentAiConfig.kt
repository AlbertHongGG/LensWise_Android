package com.lenswise.app.features.ai.config

import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * 生效之 Agent AI 配置領域模型 (EffectiveAgentAiConfig)。
 * 不可變值物件，完整封裝解析完成之生效引擎、生效參數與該 Agent 未合併之本機配置。
 */
data class EffectiveAgentAiConfig(
    val effectiveProviderType: AiProviderType,
    val activeConfig: AiProviderConfig,
    val localConfig: AiProviderConfig,
    val providerConfigs: Map<AiProviderType, AiProviderConfig>
) {
    val activeProviderType: AiProviderType
        get() = effectiveProviderType
}

/**
 * 個別 Agent 的 AI 引擎配置。
 * 遵循領域驅動設計原則：
 * 1. 引擎層級：[providerType] 必須強制顯式指定具體的 [AiProviderType]，絕不可為 null 或跟隨全域。
 * 2. 參數層級：[providerConfigs] 儲存該 Agent 針對各 Provider 的本機覆寫值；空白欄位自動 Fallback 繼承全域該引擎設定檔。
 */
data class AgentAiConfig(
    val providerType: AiProviderType = AiProviderType.GOOGLE_AI_STUDIO,
    val providerConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createEmptyConfig() }
) {
    fun getLocalConfig(type: AiProviderType = providerType): AiProviderConfig {
        return providerConfigs[type] ?: type.createEmptyConfig()
    }

    fun resolveEffective(
        globalConfigs: Map<AiProviderType, AiProviderConfig>
    ): EffectiveAgentAiConfig {
        val local = getLocalConfig(providerType)
        val global = globalConfigs[providerType]
        val activeCfg = if (global != null) local.fallbackTo(global) else local

        val effectiveConfigs = AiProviderType.entries.associateWith { type ->
            val l = providerConfigs[type] ?: type.createEmptyConfig()
            val g = globalConfigs[type]
            if (g != null) l.fallbackTo(g) else l
        }

        return EffectiveAgentAiConfig(
            effectiveProviderType = providerType,
            activeConfig = activeCfg,
            localConfig = local,
            providerConfigs = effectiveConfigs
        )
    }

    companion object {
        fun defaultConfig() = AgentAiConfig()
    }
}

/**
 * 全域 AI 設定聚合狀態。
 * [selectedGlobalProvider]：目前全域卡片所選定展示之供應商設定檔。
 * [globalProviderConfigs]：四大供應商全域設定檔庫。
 * [agentConfigs]：各 Agent 個別指派之引擎與覆寫參數。
 */
data class AiConfigState(
    val selectedGlobalProvider: AiProviderType = AiProviderType.GOOGLE_AI_STUDIO,
    val globalProviderConfigs: Map<AiProviderType, AiProviderConfig> = AiProviderType.entries.associateWith { it.createDefaultConfig() },
    val agentConfigs: Map<AiAgentType, AgentAiConfig> = emptyMap(),
    val isAiAgentEnabled: Boolean = true
) {
    val currentGlobalConfig: AiProviderConfig
        get() = globalProviderConfigs[selectedGlobalProvider] ?: selectedGlobalProvider.createDefaultConfig()

    fun getAgentConfig(agentType: AiAgentType): AgentAiConfig {
        return agentConfigs[agentType] ?: AgentAiConfig.defaultConfig()
    }

    fun getOverrideConfigFor(agentType: AiAgentType): AgentAiConfig {
        return getAgentConfig(agentType)
    }

    fun getEffectiveConfigFor(agentType: AiAgentType): EffectiveAgentAiConfig {
        return getAgentConfig(agentType).resolveEffective(globalConfigs = globalProviderConfigs)
    }
}
