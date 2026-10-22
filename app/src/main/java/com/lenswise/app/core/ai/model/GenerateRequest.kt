package com.lenswise.app.core.ai.model

/**
 * AI 生成請求領域模型 (GenerateRequest)。
 * 契約驅動且扁平的 Value Object，封裝一次 AI 生成所需的使用者提示詞、系統指令、多模態圖片與推理參數。
 */
data class GenerateRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val images: List<ByteArray>? = null,
    val model: String? = null,
    val thinking: String = "low", // "low", "medium", "high"
    val language: String = "zh-TW",
    val sessionId: String? = null,
    val temperature: Float? = null
)
