package com.lenswise.app.core.ai.agent

import com.lenswise.app.core.ai.model.AiChatRequest
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.domain.model.log.AiLogger
import com.lenswise.app.features.ai.domain.AiAgentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 系統全域統一 AI 對話顧問 Agent (UniversalChatAgent)。
 *
 * 核心權責：
 * 1. 統一承接所有業務場景（發票、商品分析、商品比對、菜單）的 AI 對話追問。
 * 2. 統一代碼樣板：動態注入業務 System Context Prompt，由單一 Agent 類別提供服務。
 * 3. 統一日誌歸類：無論從何種業務場景發起對話，API 日誌的 agentType 一律為 [AiAgentType.AI_CHAT]，
 *    自動且乾淨地歸類於「AI 深度探討」日誌分頁中，徹底消除舊版「消費顧問」等冗餘技術債。
 */
class UniversalChatAgent(
    provider: AiProvider,
    logger: AiLogger,
    private val defaultSystemPrompt: String = "你是一個具備深厚專業知識的頂級顧問 AI，請簡潔、專業、精確地回答問題。"
) : BaseAgent<AiChatRequest, String>(
    agentType = AiAgentType.AI_CHAT.id,
    provider = provider,
    logger = logger
) {

    private fun buildRequest(input: AiChatRequest): GenerateRequest {
        val promptBuilder = StringBuilder()

        if (input.systemContextPrompt.isNotBlank()) {
            promptBuilder.appendLine(input.systemContextPrompt)
        }
        promptBuilder.appendLine("\n--- 對話歷史 ---")

        for (msg in input.history) {
            val role = if (msg.isUser) "User" else "AI"
            promptBuilder.appendLine("$role: ${msg.text}")
        }

        promptBuilder.appendLine("User: ${input.newQuery}")
        promptBuilder.append("AI: ")

        return GenerateRequest(
            systemPrompt = defaultSystemPrompt,
            prompt = promptBuilder.toString()
        )
    }

    fun executeStream(input: AiChatRequest): Flow<String> {
        val request = buildRequest(input)
        return executePromptStream(request).map { it.text }
    }

    override suspend fun execute(input: AiChatRequest): String {
        val request = buildRequest(input)
        val response = executePrompt(request)
        return response.text
    }

    override fun executeStreamSnapshot(input: AiChatRequest): Flow<StreamingSnapshot<String>> = flow {
        val buffer = StringBuilder()
        executeStream(input).collect { chunk ->
            buffer.append(chunk)
            emit(StreamingSnapshot(data = buffer.toString(), isComplete = false))
        }
        emit(StreamingSnapshot(data = buffer.toString(), isComplete = true))
    }
}
