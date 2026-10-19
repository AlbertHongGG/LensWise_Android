package com.lenswise.app.domain.model.log

import androidx.compose.ui.graphics.vector.ImageVector
import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * AI 日誌分頁配置模型 (AiLogTabConfig)。
 * 封裝分頁識別元、名稱、分頁圖示、對應的 AI 代理類型以及專屬空狀態中繼資料。
 */
data class AiLogTabConfig(
    val id: String,
    val tabName: String,
    val tabIcon: ImageVector,
    val agentType: AiAgentType?, // null 代表「全部」
    val emptyStateTitle: String,
    val emptyStateDescription: String,
    val emptyStateIcon: ImageVector
)
