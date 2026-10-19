package com.lenswise.app.domain.model.history

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 歷史紀錄分頁配置模型 (HistoryTabConfig)。
 * 封裝分頁識別元、圖示、名稱與專屬空狀態文案。
 */
data class HistoryTabConfig(
    val id: String,
    val tabName: String,
    val tabIcon: ImageVector,
    val emptyStateTitle: String,
    val emptyStateDescription: String,
    val emptyStateIcon: ImageVector
)
