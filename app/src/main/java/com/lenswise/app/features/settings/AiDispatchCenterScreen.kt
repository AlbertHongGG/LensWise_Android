package com.lenswise.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenswise.app.features.ai.config.AiConfigState
import com.lenswise.app.features.ai.config.AiProviderType
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.components.PremiumConfigHeader
import com.lenswise.app.features.settings.components.AgentFleetCard
import com.lenswise.app.features.settings.components.AiDispatchDialogState
import com.lenswise.app.features.settings.components.AiDispatchDialogs
import com.lenswise.app.features.settings.components.GlobalProvidersCard
import com.lenswise.app.presentation.theme.AppColors

/**
 * AI 代理調度中心畫面 (AiDispatchCenterScreen)。
 * 遵循 MVVM 架構與單向資料流原則 (UDF)，由 [AiDispatchViewModel] 驅動。
 * 支援無 Android Context 耦合之純狀態渲染重載版本，利於 Compose 預覽與單元測試。
 */
@Composable
fun AiDispatchCenterScreen(
    viewModel: AiDispatchViewModel = viewModel(factory = AiDispatchViewModel.Factory),
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    AiDispatchCenterContent(
        state = state,
        onUpdateGlobalField = viewModel::updateGlobalField,
        onSelectGlobalProvider = viewModel::selectGlobalProvider,
        onSelectAgentProvider = viewModel::saveAgentProvider,
        onUpdateAgentField = viewModel::updateAgentField,
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun AiDispatchCenterContent(
    state: AiConfigState,
    onUpdateGlobalField: (key: String, value: String) -> Unit,
    onSelectGlobalProvider: (AiProviderType) -> Unit,
    onSelectAgentProvider: (AiAgentType, AiProviderType) -> Unit,
    onUpdateAgentField: (AiAgentType, key: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var dialogState by remember { mutableStateOf<AiDispatchDialogState>(AiDispatchDialogState.None) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumConfigHeader(
                title = "AI 代理調度中心",
                subtitle = "PER-AGENT INTELLIGENCE ROUTING"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // 1. 全域供應商配置
                GlobalProvidersCard(
                    selectedProvider = state.selectedGlobalProvider,
                    activeConfig = state.currentGlobalConfig,
                    isDark = isDark,
                    onOpenProviderSelector = {
                        dialogState = AiDispatchDialogState.SelectGlobalProvider
                    },
                    onOpenSelectFieldOption = { selectField ->
                        dialogState = AiDispatchDialogState.SelectGlobalFieldOption(selectField)
                    },
                    onFieldTextChanged = onUpdateGlobalField
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 2. 代理群調度列表 (各 Agent 皆可獨立展開與配置)
                AgentFleetCard(
                    isAiAgentEnabled = state.isAiAgentEnabled,
                    agentConfigs = state.agentConfigs,
                    globalConfigs = state.globalProviderConfigs,
                    isDark = isDark,
                    onOpenAgentProviderSelector = { agentType ->
                        dialogState = AiDispatchDialogState.SelectAgentProvider(agentType)
                    },
                    onOpenAgentSelectFieldOption = { agentType, selectField ->
                        dialogState = AiDispatchDialogState.SelectAgentFieldOption(agentType, selectField)
                    },
                    onAgentFieldTextChanged = onUpdateAgentField
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // 集中彈窗宿主 (1:1 復刻參考原型 BottomSheet)
        AiDispatchDialogs(
            dialogState = dialogState,
            isDark = isDark,
            currentGlobalProvider = state.selectedGlobalProvider,
            agentConfigs = state.agentConfigs,
            onDismissRequest = { dialogState = AiDispatchDialogState.None },
            onSelectGlobalProvider = { providerType ->
                onSelectGlobalProvider(providerType)
                dialogState = AiDispatchDialogState.None
            },
            onSelectAgentProvider = { agentType, providerType ->
                onSelectAgentProvider(agentType, providerType)
                dialogState = AiDispatchDialogState.None
            },
            onSelectGlobalFieldOption = { key, optionValue ->
                onUpdateGlobalField(key, optionValue)
                dialogState = AiDispatchDialogState.None
            },
            onSelectAgentFieldOption = { agentType, key, optionValue ->
                onUpdateAgentField(agentType, key, optionValue)
                dialogState = AiDispatchDialogState.None
            }
        )
    }
}

