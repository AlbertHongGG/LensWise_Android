package com.lenswise.app.features.receipt.domain

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import kotlinx.coroutines.flow.Flow

/**
 * 收據萃取與分析業務契約介面 (IReceiptScannerAgent)。
 *
 * 遵循依賴反轉原則 (DIP)：
 * 業務模組 (features.receipt) 僅依賴此純粹介面，不包含任何 LLM 模型或網路呼叫細節。
 * 具體實作由 AI 平台子系統 (features.ai.agents.receipt.ReceiptScannerAgent) 提供。
 */
interface IReceiptScannerAgent {

    /**
     * 響應式串流快照解析：
     * 將輸入的照片串流映射為漸進式的 [StreamingSnapshot]，驅動 UI 即時展開。
     */
    fun executeStreamSnapshot(input: List<AppImageEntity>): Flow<StreamingSnapshot<ReceiptScannerRecord>>

    /**
     * 單次非串流批次解析。
     */
    suspend fun execute(input: List<AppImageEntity>): ReceiptScannerRecord
}
