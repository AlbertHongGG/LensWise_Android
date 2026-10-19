package com.lenswise.app.core.ai.provider

import com.lenswise.app.core.ai.provider.geminiflow.GeminiFlowProvider

/**
 * AI Provider 工廠類別。
 * 支援根據設定動態生產對應的 AI Provider 實例。
 */
object AiProviderFactory {

    fun createDefaultProvider(baseUrl: String = "http://10.0.2.2:5000"): AiProvider {
        return GeminiFlowProvider(baseUrl = baseUrl)
    }
}
