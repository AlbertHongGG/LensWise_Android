package com.lenswise.app.presentation.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * AI 代理群調度列表卡片元件 (AgentFleetCard)。
 * 承載 7 大代理列表，每個代理皆為可獨立展開配置的 Accordion 項目。
 */
@Composable
fun AgentFleetCard(
    isAiAgentEnabled: Boolean,
    agentConfigs: Map<AiAgentType, AgentAiConfig>,
    globalConfigs: Map<AiProviderType, AiProviderConfig>,
    isDark: Boolean,
    onOpenAgentProviderSelector: (AiAgentType) -> Unit,
    onOpenAgentSelectFieldOption: (AiAgentType, ConfigField.Select) -> Unit,
    onAgentFieldTextChanged: (agentType: AiAgentType, key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textPrimaryColor = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondaryColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "AGENT FLEET CONFIGURATION",
            style = AppTypography.sectionHeader.copy(color = textSecondaryColor),
            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 0.dp else 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(cardBgColor)
                .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                .alpha(if (isAiAgentEnabled) 1.0f else 0.40f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val agents = AiAgentType.entries
                agents.forEachIndexed { index, agentType ->
                    val agentConfig = agentConfigs[agentType] ?: AgentAiConfig.defaultConfig()
                    val globalFallback = globalConfigs[agentConfig.activeProviderType]

                    AgentFleetItemTile(
                        agentType = agentType,
                        agentConfig = agentConfig,
                        globalFallbackConfig = globalFallback,
                        isEnabled = isAiAgentEnabled,
                        isDark = isDark,
                        cardBorderColor = cardBorderColor,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        onOpenProviderSelector = { onOpenAgentProviderSelector(agentType) },
                        onOpenSelectFieldOption = { onOpenAgentSelectFieldOption(agentType, it) },
                        onFieldTextChanged = { key, newVal -> onAgentFieldTextChanged(agentType, key, newVal) }
                    )

                    if (index < agents.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = cardBorderColor
                        )
                    }
                }
            }
        }
    }
}
