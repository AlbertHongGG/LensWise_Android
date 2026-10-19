package com.lenswise.app.features.ai.agents.receipt

import com.lenswise.app.core.ai.agent.BaseAgent
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
 * 收據專屬領域顧問 Chat Agent (ReceiptChatAgent)。
 * 專職處理針對特定收據的後續追問、明細解釋、AA制拆帳與消費建議。
 */
class ReceiptChatAgent(
    provider: AiProvider,
    logger: AiLogger
) : BaseAgent<AiChatRequest, String>(
    agentType = AiAgentType.RECEIPT_CHAT.id,
    provider = provider,
    logger = logger
) {

    private fun buildRequest(input: AiChatRequest): GenerateRequest {
        val promptBuilder = StringBuilder()

        // 注入收據動態上下文提示詞
        promptBuilder.appendLine(input.systemContextPrompt)
        promptBuilder.appendLine("\n--- 對話歷史 ---")

        for (msg in input.history) {
            val role = if (msg.isUser) "User" else "AI"
            promptBuilder.appendLine("$role: ${msg.text}")
        }

        promptBuilder.appendLine("User: ${input.newQuery}")
        promptBuilder.append("AI: ")

        return GenerateRequest(
            systemPrompt = "你是一個專業的收據與消費顧問助理，請根據提供的收據內容與情境，簡潔、專業、精確地回答問題。",
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
            emit(StreamingSnapshot(data = buffer.toString(), isComplete = false, rawChunk = chunk))
        }
        emit(StreamingSnapshot(data = buffer.toString(), isComplete = true))
    }
}
