package com.lenswise.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.logging.ApiLogManager
import com.lenswise.app.domain.model.log.ApiLogModel
import com.lenswise.app.domain.model.log.DefaultAiLogConfigs
import com.lenswise.app.features.ai.domain.AiAgentType
import com.lenswise.app.presentation.components.PremiumEmptyState
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.components.SwipeToConfirmDrawer
import com.lenswise.app.presentation.features.log.components.DragDropTrashContainer
import com.lenswise.app.presentation.features.log.components.draggableToTrash
import com.lenswise.app.presentation.features.log.components.rememberDragDropTrashState
import com.lenswise.app.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * AI / API 請求日誌畫面 (AiLogViewerScreen)。
 * 1:1 復刻歷史紀錄 (HistoryScreen) 之確定性固定版面架構：
 * - 頂部固定常駐 8 大代理人膠囊分頁欄 (Pill TabBar)，無論本機有無日誌均穩定存在，拒絕動態跳動。
 * - 各分頁具備專屬領域配置與尊榮空狀態 (PremiumEmptyState)，無日誌時優雅引導。
 * - 支援列表卡片點擊查看原始 JSON 詳情、長按拖曳至垃圾桶刪除。
 * - 底部浮空毛玻璃垃圾桶支援點擊開啟滑動抹除抽屜 (SwipeToConfirmDrawer)，精確清空當前分類日誌。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AiLogViewerScreen(
    apiLogManager: ApiLogManager,
    onNavigateToDetail: (rawJson: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val dragDropState = rememberDragDropTrashState<ApiLogModel>()
    val logs by apiLogManager.logsFlow.collectAsState()

    val tabs = remember { DefaultAiLogConfigs.allTabs }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearDrawer by remember { mutableStateOf(false) }

    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]

    // 依據目前選取之固定分頁篩選日誌
    val filteredLogs = remember(logs, currentTab) {
        val targetAgent = currentTab.agentType
        if (targetAgent == null) {
            logs
        } else {
            logs.filter { it.agentType == targetAgent }
        }
    }

    if (showClearDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR ${currentTab.tabName.uppercase()} LOGS",
            onConfirm = {
                scope.launch {
                    apiLogManager.clearLogsByAgentType(currentTab.agentType)
                    showClearDrawer = false
                }
            },
            onDismiss = { showClearDrawer = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        DragDropTrashContainer(
            state = dragDropState,
            onDropOnTrash = { item ->
                scope.launch {
                    apiLogManager.clearLog(item)
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 頂部膠囊分頁欄 (Pill TabBar，1:1 對齊 HistoryScreen)
                com.lenswise.app.presentation.components.LensWisePillTabBar(
                    items = tabs,
                    selectedIndex = selectedTabIndex,
                    onItemSelected = { selectedTabIndex = it },
                    itemTitle = { it.tabName },
                    itemIcon = { it.tabIcon }
                )

                // 2. 分頁內容視窗 (有資料顯示列表，無資料固定呈現對應之 PremiumEmptyState)
                Box(modifier = Modifier.weight(1f)) {
                    if (filteredLogs.isEmpty()) {
                        PremiumEmptyState(
                            icon = currentTab.emptyStateIcon,
                            title = currentTab.emptyStateTitle,
                            description = currentTab.emptyStateDescription
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 100.dp
                            )
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == log.id
                                ApiLogCardContent(
                                    log = log,
                                    isDark = isDark,
                                    onClick = { onNavigateToDetail(log.resolveRawJson()) },
                                    modifier = Modifier
                                        .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                        .draggableToTrash(
                                            item = log,
                                            state = dragDropState,
                                            feedback = {
                                                ApiLogCardContent(log = log, isDark = isDark)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // 3. 底部懸浮垃圾桶按鈕 (僅當前分頁有日誌時呈現，支援點擊清空與拖曳刪除)
            if (filteredLogs.isNotEmpty()) {
                val isHovering = dragDropState.isHoveringTrash
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                        .onGloballyPositioned { coordinates ->
                            dragDropState.trashBoundsInWindow = coordinates.boundsInWindow()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isHovering) 64.dp else 56.dp)
                            .shadow(
                                elevation = if (isHovering) 16.dp else 8.dp,
                                shape = CircleShape,
                                spotColor = if (isHovering) Color.Red.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f)
                            )
                            .clip(CircleShape)
                            .background(
                                when {
                                    isHovering -> Color(0xFFEF4444)
                                    isDark -> Color(0xFF1E1E1E)
                                    else -> Color.White
                                }
                            )
                            .border(
                                width = if (isHovering) 2.dp else 1.dp,
                                color = when {
                                    isHovering -> Color.Red
                                    isDark -> Color.White.copy(alpha = 0.15f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                },
                                shape = CircleShape
                            )
                            .clickable { showClearDrawer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHovering) Icons.Outlined.DeleteForever else Icons.Outlined.Delete,
                            contentDescription = "清空日誌",
                            tint = when {
                                isHovering -> Color.White
                                isDark -> Color.White.copy(alpha = 0.75f)
                                else -> Color.Black.copy(alpha = 0.70f)
                            },
                            modifier = Modifier.size(if (isHovering) 28.dp else 24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 單筆 API 日誌項目卡片。
 */
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
            val agentIcon = when (log.agentType) {
                AiAgentType.RECEIPT_SCANNER -> Icons.AutoMirrored.Filled.ReceiptLong
                AiAgentType.RECEIPT_CHAT -> Icons.Default.ChatBubble
                AiAgentType.MENU_SCANNER -> Icons.Default.DocumentScanner
                AiAgentType.MENU_TRANSLATION -> Icons.Default.Translate
                AiAgentType.PRODUCT_LENS -> Icons.Default.CenterFocusStrong
                AiAgentType.PRODUCT_COMPARISON -> Icons.AutoMirrored.Filled.CompareArrows
                else -> Icons.Outlined.SmartToy
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = agentIcon,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    style = AppTypography.cardTitle.copy(
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )
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
