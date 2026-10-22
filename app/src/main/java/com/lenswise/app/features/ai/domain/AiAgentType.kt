package com.lenswise.app.features.ai.domain

/**
 * 系統全域 AI Agent 類型列舉型錄 (AiAgentType)。
 * 完全對標參考專案 (LensWise)，為全域模型配置、個別覆蓋 (Override) 與 API 日誌提供型別基準。
 */
enum class AiAgentType(
    val id: String,
    val displayName: String
) {
    RECEIPT_SCANNER("ReceiptScanner", "收據掃描"),
    MENU_SCANNER("MenuScanner", "菜單掃描"),
    MENU_TRANSLATION("MenuTranslation", "菜單翻譯"),
    PRODUCT_COMPARISON("ProductComparison", "商品比對"),
    PRODUCT_LENS("ProductLens", "商品分析"),
    AI_CHAT("AiChat", "AI 深度探討");

    companion object {
        fun fromId(id: String): AiAgentType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
