package com.lenswise.app.presentation.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lenswise.app.presentation.components.AppSwitch
import com.lenswise.app.presentation.components.PremiumConfigHeader
import com.lenswise.app.presentation.features.settings.components.SettingsDivider
import com.lenswise.app.presentation.features.settings.components.SettingsSection
import com.lenswise.app.presentation.features.settings.components.SettingsTile
import com.lenswise.app.presentation.notification.NotificationController
import com.lenswise.app.presentation.theme.AppColors

/**
 * LensWise 經典偏好設定頁面 (SettingsScreen)。
 * 1:1 還原原版 LensWise 排版與三大分組設計（系統、AI 解析與引擎設定、資料備份與還原），支援全手勢向右滑動返回。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToNotificationLogs: () -> Unit,
    onNavigateToAiLogs: () -> Unit,
    modifier: Modifier = Modifier,
    onConfigureAiEngine: (() -> Unit)? = null,
    onExportBackup: (() -> Unit)? = null,
    onImportBackup: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsState()

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
            // 1. 頂部標頭與 3D 裝飾 ThinkingOrb
            PremiumConfigHeader(
                title = "偏好設定",
                subtitle = "SYSTEM CONFIGURATION"
            )

            // 2. 第一分組：系統
            SettingsSection(title = "系統") {
                SettingsTile(
                    icon = Icons.Default.DarkMode,
                    iconColor = AppColors.primary,
                    title = "深色模式",
                    trailing = {
                        AppSwitch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) }
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.History,
                    iconColor = AppColors.primary,
                    title = "通知日誌",
                    onTap = onNavigateToNotificationLogs,
                    trailing = {
                        AppSwitch(
                            checked = uiState.isNotificationLogEnabled,
                            onCheckedChange = { viewModel.setNotificationLogEnabled(it) }
                        )
                    }
                )
            }

            // 3. 第二分組：AI 解析與引擎設定
            SettingsSection(title = "AI 解析與引擎設定") {
                SettingsTile(
                    icon = Icons.Default.Language,
                    iconColor = AppColors.primary,
                    title = "目標翻譯語言",
                    subtitle = uiState.targetLanguage,
                    onTap = {
                        NotificationController.showInfo("目前預設語言為「${uiState.targetLanguage}」")
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.SmartToy,
                    iconColor = AppColors.primary,
                    title = "API 與引擎設定",
                    subtitle = "模型端點與金鑰配置",
                    onTap = onConfigureAiEngine ?: {
                        NotificationController.showInfo("AI 模組配置即將於下一階段推出")
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.DataObject,
                    iconColor = AppColors.primary,
                    title = "AI 日誌",
                    subtitle = "API 請求與模型推論紀錄",
                    onTap = onNavigateToAiLogs,
                    trailing = {
                        AppSwitch(
                            checked = uiState.isApiLogEnabled,
                            onCheckedChange = { viewModel.setApiLogEnabled(it) }
                        )
                    }
                )
            }

            // 4. 第三分組：資料備份與還原 (1:1 復刻 LensWise backup_settings_section)
            SettingsSection(title = "資料備份與還原") {
                SettingsTile(
                    icon = Icons.Default.UploadFile,
                    iconColor = AppColors.primary,
                    title = "匯出歷史紀錄",
                    subtitle = "打包為 ZIP 檔",
                    onTap = onExportBackup ?: {
                        NotificationController.showInfo("匯出備份功能即將於後續版本推出")
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.SettingsBackupRestore,
                    iconColor = AppColors.primary,
                    title = "匯入歷史紀錄",
                    subtitle = "從 ZIP 檔還原",
                    onTap = onImportBackup ?: {
                        NotificationController.showInfo("匯入還原功能即將於後續版本推出")
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
