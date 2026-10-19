package com.lenswise.app.core.ai.agent

import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.core.ai.streaming.JsonStreamPipeline
import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.domain.model.log.AiLogger
import com.lenswise.app.domain.model.log.ApiLogRecord
import com.lenswise.app.domain.model.log.ApiLogRequest
import com.lenswise.app.domain.model.log.ApiLogResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 所有 AI 功能 Agent 的核心抽象基類 (BaseAgent)。
 *
 * 核心權責：
 * 1. 統一管理 AI Provider 呼叫與計時生命週期。
 * 2. 嚴格延遲日誌策略 (End-of-Stream API Logging)：在串流完全結束 (onCompletion) 時，
 *    才將耗時毫秒數、完整累計回應與 Prompt 包裝為 [ApiLogRecord] 寫入 [AiLogger]。
 * 3. 整合 [JsonStreamPipeline] 串流管線，提供強型別 [StreamingSnapshot] 漸進輸出。
 */
abstract class BaseAgent<TIn, TOut>(
    val agentType: String,
    val provider: AiProvider,
    val logger: AiLogger
) {

    abstract suspend fun execute(input: TIn): TOut

    abstract fun executeStreamSnapshot(input: TIn): Flow<StreamingSnapshot<TOut>>

    protected fun executeStreamingPipeline(
        request: GenerateRequest,
        mapper: (JsonObject) -> TOut,
        initialData: TOut,
        throttleMs: Long = 50L
    ): Flow<StreamingSnapshot<TOut>> {
        val rawStream = executePromptStream(request).map { it.text }
        return JsonStreamPipeline.bind(
            rawStream = rawStream,
            mapper = mapper,
            initialData = initialData,
            throttleMs = throttleMs
        )
    }

    protected suspend fun executePrompt(request: GenerateRequest): GenerateResponse {
        val startTime = System.currentTimeMillis()
        try {
            val response = provider.generate(request)
            val durationMs = System.currentTimeMillis() - startTime
            logRecord(request, response.text, null, durationMs)
            return response
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            logRecord(request, null, e.message, durationMs)
            throw e
        }
    }

    protected fun executePromptStream(request: GenerateRequest): Flow<GenerateResponse> {
        val startTime = System.currentTimeMillis()
        val buffer = StringBuilder()

        return provider.generateStream(request)
            .onEach { chunk ->
                if (chunk.text.isNotEmpty()) {
                    buffer.append(chunk.text)
                }
            }
            .onCompletion { cause ->
                val durationMs = System.currentTimeMillis() - startTime
                val fullResponse = if (cause == null) buffer.toString() else null
                val errorMsg = cause?.message
                logRecord(request, fullResponse, errorMsg, durationMs)
            }
    }

    private fun logRecord(
        request: GenerateRequest,
        responseText: String?,
        errorText: String?,
        durationMs: Long
    ) {
        val now = Date()
        val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).format(now)

        val imageSummaries = request.images?.map { "${it.size} bytes" }

        val record = ApiLogRecord(
            timestamp = isoTimestamp,
            agentName = agentType,
            durationMs = durationMs,
            request = ApiLogRequest(
                model = provider.name,
                prompt = request.prompt,
                systemPrompt = request.systemPrompt,
                images = imageSummaries
            ),
            response = ApiLogResponse(
                text = responseText,
                error = errorText
            )
        )
        logger.logInteraction(record)
    }
}
