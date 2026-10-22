package com.lenswise.app.presentation.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.ai.config.AgentAiConfig
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.features.ai.config.SelectOption
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * AI 調度中心彈窗集合宿主 (AiDispatchDialogs)。
 * 1:1 復刻參考原型之底部抽屜選擇器 (BottomSheet)，包含頂部 Handle Bar、居中標題、圖標、主副標與選中圓形 Checkmark。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDispatchDialogs(
    dialogState: AiDispatchDialogState,
    isDark: Boolean,
    currentGlobalProvider: AiProviderType,
    agentConfigs: Map<AiAgentType, AgentAiConfig>,
    onDismissRequest: () -> Unit,
    onSelectGlobalProvider: (AiProviderType) -> Unit,
    onSelectAgentProvider: (AiAgentType, AiProviderType?) -> Unit,
    onSelectGlobalFieldOption: (fieldKey: String, optionValue: String) -> Unit,
    onSelectAgentFieldOption: (agentType: AiAgentType, fieldKey: String, optionValue: String) -> Unit
) {
    val sheetBgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val textPrimaryColor = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondaryColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)

    when (dialogState) {
        is AiDispatchDialogState.None -> {
            // 無開啟中之彈窗
        }
        is AiDispatchDialogState.SelectGlobalProvider -> {
            ModalBottomSheet(
                onDismissRequest = onDismissRequest,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = sheetBgColor,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                        .navigationBarsPadding()
                ) {
                    BottomSheetHandleBar(isDark = isDark)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "選擇全域 AI 引擎",
                        style = AppTypography.dialogTitle.copy(
                            color = textPrimaryColor,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val entries = AiProviderType.entries
                    entries.forEachIndexed { index, providerType ->
                        val isSelected = currentGlobalProvider == providerType
                        val rowBgColor = if (isSelected) {
                            if (isDark) Color.White.copy(alpha = 0.08f) else AppColors.primary.copy(alpha = 0.08f)
                        } else {
                            Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(rowBgColor)
                                .clickable { onSelectGlobalProvider(providerType) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = providerType.icon,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = providerType.displayName.uppercase(),
                                style = if (isSelected) {
                                    AppTypography.listItemSelected.copy(color = textPrimaryColor)
                                } else {
                                    AppTypography.listItemUnselected.copy(color = textPrimaryColor)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "已選擇",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < entries.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        is AiDispatchDialogState.SelectAgentProvider -> {
            val agentType = dialogState.agentType
            val agentConfig = agentConfigs[agentType] ?: AgentAiConfig.defaultConfig()

            ModalBottomSheet(
                onDismissRequest = onDismissRequest,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = sheetBgColor,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                        .navigationBarsPadding()
                ) {
                    BottomSheetHandleBar(isDark = isDark)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "選擇 AI 引擎",
                        style = AppTypography.dialogTitle.copy(
                            color = textPrimaryColor,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 第一項：跟隨全域設定
                    val isFollowGlobal = agentConfig.isFollowingGlobal
                    val followGlobalBgColor = if (isFollowGlobal) {
                        if (isDark) Color.White.copy(alpha = 0.08f) else AppColors.primary.copy(alpha = 0.08f)
                    } else {
                        Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(followGlobalBgColor)
                            .clickable { onSelectAgentProvider(agentType, null) }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "跟隨全域設定",
                                style = if (isFollowGlobal) {
                                    AppTypography.listItemSelected.copy(color = textPrimaryColor)
                                } else {
                                    AppTypography.listItemUnselected.copy(color = textPrimaryColor)
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "目前繼承：${currentGlobalProvider.displayName.uppercase()}",
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = textSecondaryColor
                            )
                        }
                        if (isFollowGlobal) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已選擇",
                                tint = AppColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val entries = AiProviderType.entries
                    entries.forEachIndexed { index, providerType ->
                        val isSelected = agentConfig.customProviderType == providerType
                        val rowBgColor = if (isSelected) {
                            if (isDark) Color.White.copy(alpha = 0.08f) else AppColors.primary.copy(alpha = 0.08f)
                        } else {
                            Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(rowBgColor)
                                .clickable { onSelectAgentProvider(agentType, providerType) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = providerType.icon,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = providerType.displayName.uppercase(),
                                style = if (isSelected) {
                                    AppTypography.listItemSelected.copy(color = textPrimaryColor)
                                } else {
                                    AppTypography.listItemUnselected.copy(color = textPrimaryColor)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "已選擇",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < entries.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        is AiDispatchDialogState.SelectGlobalFieldOption -> {
            val selectField = dialogState.field
            val icon = resolveOptionSheetIcon(selectField.key)

            ModalBottomSheet(
                onDismissRequest = onDismissRequest,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = sheetBgColor,
                dragHandle = null
            ) {
                OptionBottomSheetContent(
                    title = "選擇${selectField.label}",
                    options = selectField.options,
                    selectedValue = selectField.value,
                    defaultIcon = icon,
                    isDark = isDark,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onSelect = { option ->
                        onSelectGlobalFieldOption(selectField.key, option.value)
                    }
                )
            }
        }
        is AiDispatchDialogState.SelectAgentFieldOption -> {
            val agentType = dialogState.agentType
            val selectField = dialogState.field
            val icon = resolveOptionSheetIcon(selectField.key)

            ModalBottomSheet(
                onDismissRequest = onDismissRequest,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = sheetBgColor,
                dragHandle = null
            ) {
                OptionBottomSheetContent(
                    title = "選擇${selectField.label}",
                    options = selectField.options,
                    selectedValue = selectField.value,
                    defaultIcon = icon,
                    isDark = isDark,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    onSelect = { option ->
                        onSelectAgentFieldOption(agentType, selectField.key, option.value)
                    }
                )
            }
        }
    }
}

/**
 * 頂部圓角拖曳指示條 (Handle Bar)。
 */
@Composable
private fun BottomSheetHandleBar(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f))
        )
    }
}

/**
 * 動態選項抽屜內容元件。
 */
@Composable
private fun OptionBottomSheetContent(
    title: String,
    options: List<SelectOption>,
    selectedValue: String,
    defaultIcon: ImageVector,
    isDark: Boolean,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    onSelect: (SelectOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
            .navigationBarsPadding()
    ) {
        BottomSheetHandleBar(isDark = isDark)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = AppTypography.dialogTitle.copy(
                color = textPrimaryColor,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        options.forEachIndexed { index, option ->
            val isSelected = option.value.equals(selectedValue, ignoreCase = true)
            val isEmptyOption = option.value.isEmpty()
            val itemIcon = if (isEmptyOption) Icons.Default.RemoveCircleOutline else defaultIcon
            val itemIconTint = if (isEmptyOption) textSecondaryColor else AppColors.primary

            val rowBgColor = if (isSelected) {
                if (isDark) Color.White.copy(alpha = 0.08f) else AppColors.primary.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(rowBgColor)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = itemIcon,
                    contentDescription = null,
                    tint = itemIconTint,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.label,
                        style = if (isSelected) {
                            AppTypography.listItemSelected.copy(color = textPrimaryColor)
                        } else {
                            AppTypography.listItemUnselected.copy(color = textPrimaryColor)
                        }
                    )
                    if (!option.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = option.description,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = textSecondaryColor
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已選擇",
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (index < options.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun resolveOptionSheetIcon(key: String): ImageVector {
    val k = key.lowercase()
    return when {
        k.contains("model") -> Icons.Default.SmartToy
        k.contains("thinking") -> Icons.Default.Tune
        k.contains("url") || k.contains("endpoint") -> Icons.Default.Dns
        else -> Icons.Default.Tune
    }
}
