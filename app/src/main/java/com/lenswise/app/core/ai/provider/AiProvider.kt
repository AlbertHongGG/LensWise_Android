package com.lenswise.app.core.ai.provider

import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import kotlinx.coroutines.flow.Flow

/**
 * AI Provider 統一契約介面。
 * 支援非串流單次生成 (`generate`) 與響應式 SSE 串流生成 (`generateStream`)。
 */
interface AiProvider {
    val name: String
    suspend fun generate(request: GenerateRequest): GenerateResponse
    fun generateStream(request: GenerateRequest): Flow<GenerateResponse>
}
