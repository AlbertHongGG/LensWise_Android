package com.lenswise.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.logging.ApiLogManager
import com.lenswise.app.domain.model.log.ApiLogModel
import com.lenswise.app.presentation.features.log.components.GenericLogViewerScaffold
import com.lenswise.app.presentation.theme.AppColors

/**
 * AI / API 請求日誌畫面 (AiLogViewerScreen)。
 * 100% 依據 LensWise / GeminiFlowApp 規範重構：
 * 徹底移除 PremiumConfigHeader 與 ThinkingOrb，頂部僅保留純淨對稱的膠囊分頁欄 (Pill TabBar)。
 */
@Composable
fun AiLogViewerScreen(
    apiLogManager: ApiLogManager,
    onNavigateToDetail: (rawJson: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val logs by apiLogManager.logsFlow.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = remember(logs) {
        val customSources = logs.map { it.source }.distinct().filter { it != "全部" }
        listOf("全部") + customSources
    }

    val filteredLogs = remember(logs, selectedTabIndex, tabs) {
        if (selectedTabIndex == 0 || selectedTabIndex >= tabs.size) {
            logs
        } else {
            val selectedSource = tabs[selectedTabIndex]
            logs.filter { it.source == selectedSource }
        }
    }

    GenericLogViewerScaffold(
        items = filteredLogs,
        onDeleteSingle = { apiLogManager.clearLog(it.file) },
        onClearAll = { apiLogManager.clearAllLogs() },
        clearDrawerTitle = "CLEAR AI LOGS",
        emptyMessage = "目前沒有系統日誌。",
        modifier = modifier,
        header = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.05f)
                        else Color.Black.copy(alpha = 0.04f)
                    )
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTabIndex == index
                        val tabBg = when {
                            isSelected && isDark -> Color.White.copy(alpha = 0.15f)
                            isSelected -> Color.White
                            else -> Color.Transparent
                        }
                        val tabTextColor = when {
                            isSelected && isDark -> Color.White
                            isSelected -> AppColors.primary
                            isDark -> Color.White.copy(alpha = 0.54f)
                            else -> Color.Black.copy(alpha = 0.54f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .shadow(
                                    elevation = if (isSelected && !isDark) 4.dp else 0.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = Color.Black.copy(alpha = 0.08f)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(tabBg)
                                .clickable { selectedTabIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabTextColor
                            )
                        }
                    }
                }
            }
        },
        itemContent = { log, isDragging, itemModifier ->
            ApiLogCardContent(
                log = log,
                isDark = isDark,
                onClick = { onNavigateToDetail(log.resolveRawJson()) },
                modifier = itemModifier.alpha(if (isDragging) 0.3f else 1.0f)
            )
        }
    )
}

@Composable
private fun ApiLogCardContent(
    log: ApiLogModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (log.durationMs > 0) "${log.displayTime} (${log.durationMs}ms)" else log.displayTime,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.45f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "查看詳情",
                tint = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
