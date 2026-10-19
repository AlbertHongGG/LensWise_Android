package com.lenswise.app.domain.model.log

/**
 * AI / 視覺推論請求與互動日誌紀錄契約介面 (AiLogger)。
 *
 * 遵循依賴反轉原則 (DIP)：
 * 讓未來的 AI 執行引擎（GeminiClient, ReceiptAnalyzer, ProductComparisonEngine 等）
 * 僅需依賴此介面寫入日誌，徹底與底層檔案儲存、UI 列表及檢視器解耦。
 */
interface AiLogger {

    /**
     * 是否啟用 AI 請求日誌紀錄。
     */
    fun isLoggingEnabled(): Boolean

    /**
     * 設定是否啟用 AI 請求日誌紀錄。
     */
    fun setLoggingEnabled(enabled: Boolean)

    /**
     * 寫入一筆標準 AI / API 互動紀錄。
     */
    fun logInteraction(record: ApiLogRecord)
}
