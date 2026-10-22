package com.lenswise.app.features.comparison.agent

import com.lenswise.app.core.ai.agent.BaseAgent
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.core.ai.streaming.JsonStreamPipeline
import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.domain.model.log.AiLogger
import com.lenswise.app.domain.model.settings.TargetLanguage
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.features.comparison.domain.IProductComparisonAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * 商品比對專用 Agent (ProductComparisonAgent)。
 * 實作業務契約 [IProductComparisonAgent]，並繼承 [BaseAgent] 獲得計時與統一 API 日誌能力。
 */
class ProductComparisonAgent(
    provider: AiProvider,
    logger: AiLogger,
    private val targetLanguage: TargetLanguage = TargetLanguage.TRADITIONAL_CHINESE
) : BaseAgent<List<AppImageEntity>, ProductComparisonRecord>(
    agentType = AiAgentType.PRODUCT_COMPARISON.id,
    provider = provider,
    logger = logger
), IProductComparisonAgent {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun executeStreamSnapshot(input: List<AppImageEntity>): Flow<StreamingSnapshot<ProductComparisonRecord>> = flow {
        val request = GenerateRequest(
            prompt = ProductComparisonPrompts.buildUserPrompt(),
            systemPrompt = ProductComparisonPrompts.buildSystemPrompt(targetLanguage, input.size),
            images = input.map { it.byteArray },
            language = targetLanguage.code
        )

        emitAll(
            executeStreamingPipeline(
                request = request,
                mapper = ProductComparisonPayloadParser::parseStreamingSnapshot,
                initialData = ProductComparisonRecord.empty()
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun execute(input: List<AppImageEntity>): ProductComparisonRecord = withContext(Dispatchers.IO) {
        val request = GenerateRequest(
            prompt = ProductComparisonPrompts.buildUserPrompt(),
            systemPrompt = ProductComparisonPrompts.buildSystemPrompt(targetLanguage, input.size),
            images = input.map { it.byteArray },
            language = targetLanguage.code
        )

        val response = executePrompt(request)
        val candidate = JsonStreamPipeline.extractJsonSubstring(response.text)
        val repaired = JsonStreamPipeline.repairPartialJson(candidate)
        val parsedJson = json.parseToJsonElement(repaired).jsonObject
        ProductComparisonPayloadParser.parseStreamingSnapshot(parsedJson)
    }
}

