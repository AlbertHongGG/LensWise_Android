package com.lenswise.app.presentation.features.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.features.ai.config.EffectiveAgentAiConfig
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 個別 AI 代理展開折疊項目元件 (AgentFleetItemTile)。
 * 1:1 復刻參考專案之 Accordion 展開效果，支援個別指派引擎與覆蓋模型參數（具備全域 fallback 機制）。
 */
@Composable
fun AgentFleetItemTile(
    agentType: AiAgentType,
    agentConfig: AgentAiConfig,
    effectiveConfig: EffectiveAgentAiConfig,
    globalFallbackConfig: AiProviderConfig?,
    isEnabled: Boolean,
    isDark: Boolean,
    cardBorderColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onOpenProviderSelector: () -> Unit,
    onOpenSelectFieldOption: (ConfigField.Select) -> Unit,
    onFieldTextChanged: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val effectiveProvider = effectiveConfig.effectiveProviderType
    val isFollowing = agentConfig.isFollowingGlobal

    Column(modifier = modifier.fillMaxWidth()) {
        // 卡片主列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) { isExpanded = !isExpanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAgentIcon(agentType),
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agentType.displayName,
                    style = AppTypography.tileTitle.copy(color = textPrimaryColor)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Public else effectiveProvider.icon,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val providerDesc = if (isFollowing) {
                        "${effectiveProvider.displayName.uppercase()} (跟隨全域)"
                    } else {
                        "${effectiveProvider.displayName.uppercase()} (個別覆蓋)"
                    }
                    Text(
                        text = providerDesc,
                        style = AppTypography.heroSubtitle.copy(
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = textSecondaryColor
                        )
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = textSecondaryColor
            )
        }

        // 展開表單 (與 GlobalProvidersCard 具備一致的階層結構)
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFFF4F5F7))
                    .border(width = 0.5.dp, color = cardBorderColor)
                    .padding(vertical = 8.dp)
            ) {
                // 1. 代理引擎切換列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProviderSelector() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Public else effectiveProvider.icon,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    val dropdownLabel = if (isFollowing) {
                        "跟隨全域 (${effectiveProvider.displayName.uppercase()})"
                    } else {
                        effectiveProvider.displayName.uppercase()
                    }
                    Text(
                        text = dropdownLabel,
                        style = AppTypography.itemValue.copy(color = textPrimaryColor),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textSecondaryColor
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    thickness = 0.5.dp,
                    color = cardBorderColor
                )

                // 2. 代理專屬動態欄位 (支援 fallback 至全域設定)
                val fields = effectiveConfig.activeConfig.getFormFields(globalFallbackConfig)
                fields.forEachIndexed { index, field ->
                    ConfigFieldTile(
                        field = field,
                        isDark = isDark,
                        isGlobal = false,
                        onSelectOptionClick = onOpenSelectFieldOption,
                        onTextChanged = { newVal -> onFieldTextChanged(field.key, newVal) }
                    )

                    if (index < fields.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            thickness = 0.5.dp,
                            color = cardBorderColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun getAgentIcon(type: AiAgentType): ImageVector {
    return when (type) {
        AiAgentType.RECEIPT_SCANNER -> Icons.AutoMirrored.Filled.ReceiptLong
        AiAgentType.RECEIPT_CHAT -> Icons.Default.ChatBubble
        AiAgentType.MENU_SCANNER -> Icons.Default.DocumentScanner
        AiAgentType.MENU_TRANSLATION -> Icons.Default.Translate
        AiAgentType.PRODUCT_COMPARISON -> Icons.AutoMirrored.Filled.CompareArrows
        AiAgentType.PRODUCT_LENS -> Icons.Default.CenterFocusStrong
        AiAgentType.AI_CHAT -> Icons.Default.AutoAwesome
    }
}
