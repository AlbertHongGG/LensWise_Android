package com.lenswise.app.domain.model.log

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.SmartToy
import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * 系統預設 AI 日誌分頁型錄 (DefaultAiLogConfigs)。
 * 預先定義不可變的 7 大固定分頁與專屬空狀態，保證版面在任何資料狀態下均具備絕對確定性。
 * 完全對標 Flutter 參考專案 (全部、收據掃描、菜單掃描、菜單翻譯、商品比對、商品分析、深度探討)。
 */
object DefaultAiLogConfigs {
    val allTabs: List<AiLogTabConfig> = listOf(
        AiLogTabConfig(
            id = "all",
            tabName = "全部",
            tabIcon = Icons.Default.SmartToy,
            agentType = null,
            emptyStateTitle = "尚無任何 AI 日誌",
            emptyStateDescription = "尚未產生任何 AI 模型請求紀錄",
            emptyStateIcon = Icons.Default.SmartToy
        ),
        AiLogTabConfig(
            id = "receipt_scanner",
            tabName = "收據掃描",
            tabIcon = Icons.AutoMirrored.Filled.ReceiptLong,
            agentType = AiAgentType.RECEIPT_SCANNER,
            emptyStateTitle = "尚無收據掃描日誌",
            emptyStateDescription = "尚未產生發票辨識與解析歷程",
            emptyStateIcon = Icons.AutoMirrored.Filled.ReceiptLong
        ),
        AiLogTabConfig(
            id = "menu_scanner",
            tabName = "菜單掃描",
            tabIcon = Icons.Default.DocumentScanner,
            agentType = AiAgentType.MENU_SCANNER,
            emptyStateTitle = "尚無菜單掃描日誌",
            emptyStateDescription = "尚未產生菜單結構化擷取歷程",
            emptyStateIcon = Icons.Default.DocumentScanner
        ),
        AiLogTabConfig(
            id = "menu_translation",
            tabName = "菜單翻譯",
            tabIcon = Icons.Default.Translate,
            agentType = AiAgentType.MENU_TRANSLATION,
            emptyStateTitle = "尚無菜單翻譯日誌",
            emptyStateDescription = "尚未產生多語言菜單翻譯歷程",
            emptyStateIcon = Icons.Default.Translate
        ),
        AiLogTabConfig(
            id = "product_comparison",
            tabName = "商品比對",
            tabIcon = Icons.AutoMirrored.Filled.CompareArrows,
            agentType = AiAgentType.PRODUCT_COMPARISON,
            emptyStateTitle = "尚無商品比對日誌",
            emptyStateDescription = "尚未產生商品規格比對歷程",
            emptyStateIcon = Icons.AutoMirrored.Filled.CompareArrows
        ),
        AiLogTabConfig(
            id = "product_lens",
            tabName = "商品分析",
            tabIcon = Icons.Default.CenterFocusStrong,
            agentType = AiAgentType.PRODUCT_LENS,
            emptyStateTitle = "尚無商品分析日誌",
            emptyStateDescription = "尚未產生商品特徵辨識歷程",
            emptyStateIcon = Icons.Default.CenterFocusStrong
        ),
        AiLogTabConfig(
            id = "ai_chat",
            tabName = "深度探討",
            tabIcon = Icons.Outlined.SmartToy,
            agentType = AiAgentType.AI_CHAT,
            emptyStateTitle = "尚無深度探討日誌",
            emptyStateDescription = "尚未產生 AI 深度諮詢對話歷程",
            emptyStateIcon = Icons.Outlined.SmartToy
        )
    )
}
