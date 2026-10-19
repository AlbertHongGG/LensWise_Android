package com.lenswise.app.core.ai.model

/**
 * AI 生成請求實體。
 * 支援文字提示、系統提示詞、多模態影像二進位資料。
 */
data class GenerateRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val images: List<ByteArray>? = null,
    val model: String? = null,
    val temperature: Float? = null
)
