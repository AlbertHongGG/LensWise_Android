package com.lenswise.app.presentation.features.settings.components

import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.features.ai.domain.AiAgentType

/**
 * AI 代理調度中心彈窗原子狀態機。
 * 保證同一時刻最多僅有一種對話框處於開啟狀態，徹底避免狀態衝突與幽靈重繪。
 */
sealed interface AiDispatchDialogState {
    data object None : AiDispatchDialogState
    data object SelectGlobalProvider : AiDispatchDialogState
    data class SelectAgentProvider(val agentType: AiAgentType) : AiDispatchDialogState
    data class SelectGlobalFieldOption(val field: ConfigField.Select) : AiDispatchDialogState
    data class SelectAgentFieldOption(val agentType: AiAgentType, val field: ConfigField.Select) : AiDispatchDialogState
}
