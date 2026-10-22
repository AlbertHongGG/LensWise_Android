package com.lenswise.app.core.ai.agent

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 系統全域掃描分析型 Agent 通用標準契約 (IScannerAgent)。
 * 統一規範所有業務掃描分析 Agent 的單次完整推論與漸進串流合約。
 */
interface IScannerAgent<TRecord> {
    suspend fun execute(images: List<AppImageEntity>): TRecord
    fun executeStreamSnapshot(images: List<AppImageEntity>): Flow<StreamingSnapshot<TRecord>>
}
