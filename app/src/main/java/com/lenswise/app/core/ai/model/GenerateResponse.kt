package com.lenswise.app.core.ai.model

/**
 * AI 生成回應實體。
 * 支援文字內容、生成的圖像 URL 或 Base64 字串，以及是否完成之標記。
 */
data class GenerateResponse(
    val text: String,
    val images: List<String>? = null,
    val isDone: Boolean = false
)
