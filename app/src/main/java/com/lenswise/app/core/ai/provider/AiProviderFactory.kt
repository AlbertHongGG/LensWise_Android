package com.lenswise.app.core.ai.provider

import com.lenswise.app.core.ai.provider.geminiflow.GeminiFlowProvider
import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiConfigRepository
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.GeminiFlowConfig
import com.lenswise.app.features.ai.config.IAiConfigRepository
import com.lenswise.app.features.ai.config.MissingAiConfigException
import com.lenswise.app.features.ai.config.OllamaConfig
import com.lenswise.app.features.ai.config.VertexAiConfig
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.notification.NotificationController

/**
 * AI Provider 工廠類別。
 * 支援根據動態調度中心配置，產出對應 Agent 綁定的 AI Provider 實例。
 * 實施嚴格防禦性契約校驗，杜絕任何魔法字串回退；配置缺失時主動推送錯誤通知並拋出異常。
 */
object AiProviderFactory {

    fun createDefaultProvider(baseUrl: String = "http://10.0.2.2:5000"): AiProvider {
        return GeminiFlowProvider(baseUrl = baseUrl)
    }

    fun createProviderForAgent(
        agentType: AiAgentType,
        configRepo: com.lenswise.app.features.ai.config.IAiConfigRepository
    ): AiProvider {
        val rootConfig = configRepo.configState.value
        if (!rootConfig.isAiAgentEnabled) {
            val errorMsg = "「${agentType.displayName}」執行失敗：AI Agent 功能已停用，請至 AI 代理調度中心開啟。"
            NotificationController.showError(errorMsg)
            throw IllegalStateException(errorMsg)
        }

        val effective = configRepo.getEffectiveConfigFor(agentType)

        try {
            effective.activeConfig.validateRequiredFields(agentType)
        } catch (e: MissingAiConfigException) {
            NotificationController.showError(e.message ?: "未指定必要 AI 參數")
            throw e
        }

        return when (effective.activeProviderType) {
            AiProviderType.GEMINIFLOW -> {
                val config = effective.activeConfig as? GeminiFlowConfig
                    ?: throw IllegalStateException("GeminiFlow 配置型別異常")
                GeminiFlowProvider(
                    baseUrl = config.baseUrl,
                    modelName = config.modelName
                )
            }
            AiProviderType.GOOGLE_AI_STUDIO -> {
                val config = effective.activeConfig as? com.lenswise.app.features.ai.config.GoogleAiStudioConfig
                    ?: throw IllegalStateException("Google AI Studio 配置型別異常")
                com.lenswise.app.core.ai.provider.googleaistudio.GoogleAiStudioProvider(
                    apiKey = config.apiKey,
                    modelName = config.modelName,
                    thinkingLevel = config.thinkingLevel
                )
            }
            AiProviderType.OLLAMA -> {
                val config = effective.activeConfig as? OllamaConfig
                    ?: throw IllegalStateException("Ollama 配置型別異常")
                com.lenswise.app.core.ai.provider.ollama.OllamaProvider(
                    baseUrl = config.baseUrl,
                    modelName = config.modelName
                )
            }
            AiProviderType.VERTEX_AI -> {
                val config = effective.activeConfig as? VertexAiConfig
                    ?: throw IllegalStateException("Vertex AI 配置型別異常")
                com.lenswise.app.core.ai.provider.vertexai.VertexAiProvider(
                    projectId = config.projectId,
                    location = config.location,
                    modelName = config.modelName,
                    apiKey = config.apiKey
                )
            }
        }
    }
}
