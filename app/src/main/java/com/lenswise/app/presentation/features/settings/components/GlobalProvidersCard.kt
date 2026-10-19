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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.ai.config.AiProviderConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 全域 AI 供應商配置卡片元件 (GlobalProvidersCard)。
 */
@Composable
fun GlobalProvidersCard(
    editingType: AiProviderType,
    activeConfig: AiProviderConfig,
    isDark: Boolean,
    onOpenProviderSelector: () -> Unit,
    onOpenSelectFieldOption: (ConfigField.Select) -> Unit,
    onFieldTextChanged: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val cardBgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textPrimaryColor = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondaryColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "GLOBAL PROVIDERS CONFIGURATION",
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
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 卡片標頭列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
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
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "全域供應商設定",
                            style = AppTypography.tileTitle.copy(color = textPrimaryColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = editingType.icon,
                                contentDescription = null,
                                tint = textSecondaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = editingType.displayName.uppercase(),
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

                // 展開表單
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFFF4F5F7))
                            .border(width = 0.5.dp, color = cardBorderColor)
                            .padding(vertical = 8.dp)
                    ) {
                        // 引擎切換 Tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProviderSelector() }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = editingType.icon,
                                contentDescription = null,
                                tint = textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = editingType.displayName.uppercase(),
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

                        // 動態表單欄位
                        val fields = activeConfig.getFormFields()
                        fields.forEachIndexed { index, field ->
                            ConfigFieldTile(
                                field = field,
                                isDark = isDark,
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
    }
}
