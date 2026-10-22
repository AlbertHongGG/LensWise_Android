package com.lenswise.app.core.ai.provider.geminiflow.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GeminiFlow 伺服端標準通訊請求 DTO。
 * 1:1 對齊參考專案服務端協定：
 * - 嚴格使用 [SerialName("system_prompt")] 對齊蛇形命名，保證系統提示詞絕對不遺失。
 * - 支援 [thinking] 思考強度 ("low", "medium", "high")。
 * - 支援 [language] 語言約束 ("zh-TW")。
 * - 支援 [session_id] 會話標記。
 */
@Serializable
data class GeminiFlowChatRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("system_prompt") val systemPrompt: String? = null,
    @SerialName("model") val model: String,
    @SerialName("thinking") val thinking: String = "low",
    @SerialName("language") val language: String = "zh-TW",
    @SerialName("images") val images: List<String> = emptyList(),
    @SerialName("session_id") val sessionId: String? = null
)
