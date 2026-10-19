package com.lenswise.app.presentation.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.domain.model.history.HistoryTabConfig
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.presentation.components.SwipeToConfirmDrawer
import com.lenswise.app.presentation.components.PremiumEmptyState
import com.lenswise.app.presentation.features.history.components.ReceiptHistoryCard
import com.lenswise.app.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * 統一歷史紀錄畫面 (HistoryScreen)。
 * 1:1 復刻 LensWise Flutter 版 UnifiedHistoryPage：
 * 頂部支援 4 大核心業務膠囊分頁（收據、商品、對比、菜單），
 * 具備精美空狀態插畫、卡片列表與懸浮垃圾桶滑動抹除抽屜 (SwipeToObliterateButton)。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    initialTab: Int = 0,
    onNavigateToReceiptDetail: (ReceiptScannerRecord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val app = context.applicationContext as LensWiseApplication
    val receiptRepo = app.receiptHistoryRepository
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    var showClearDrawer by remember { mutableStateOf(false) }

    val tabs = remember {
        listOf(
            HistoryTabConfig(
                id = "receipt",
                tabName = "收據",
                tabIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                emptyStateTitle = "尚無發票帳目紀錄",
                emptyStateDescription = "所有透過相機掃描的發票資訊，\n都會有條理地整理在此。",
                emptyStateIcon = Icons.AutoMirrored.Filled.ReceiptLong
            ),
            HistoryTabConfig(
                id = "product",
                tabName = "商品",
                tabIcon = Icons.Default.QrCodeScanner,
                emptyStateTitle = "尚無商品解析紀錄",
                emptyStateDescription = "所有透過相機掃描的商品特徵與解析，\n都會有條理地整理在此。",
                emptyStateIcon = Icons.Default.QrCodeScanner
            ),
            HistoryTabConfig(
                id = "comparison",
                tabName = "對比",
                tabIcon = Icons.AutoMirrored.Filled.CompareArrows,
                emptyStateTitle = "尚無商品對比紀錄",
                emptyStateDescription = "所有透過相機進行的規格與成分對比，\n都會有條理地整理在此。",
                emptyStateIcon = Icons.AutoMirrored.Filled.CompareArrows
            ),
            HistoryTabConfig(
                id = "menu",
                tabName = "菜單",
                tabIcon = Icons.Default.RestaurantMenu,
                emptyStateTitle = "尚無菜單解析紀錄",
                emptyStateDescription = "所有透過相機拍攝的菜單翻譯與推薦，\n都會有條理地整理在此。",
                emptyStateIcon = Icons.Default.RestaurantMenu
            )
        )
    }

    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]
    val receipts by receiptRepo.receiptsFlow.collectAsState()

    if (showClearDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR ${currentTab.tabName.uppercase()} HISTORY",
            onConfirm = {
                scope.launch {
                    if (selectedTabIndex == 0) {
                        receiptRepo.clearAll()
                    }
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
        Column(modifier = Modifier.fillMaxSize()) {
            // 頂部膠囊分頁欄 (Pill TabBar)
            com.lenswise.app.presentation.components.LensWisePillTabBar(
                items = tabs,
                selectedIndex = selectedTabIndex,
                onItemSelected = { selectedTabIndex = it },
                itemTitle = { it.tabName },
                itemIcon = { it.tabIcon }
            )

            // 分頁內容容器
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> {
                        if (receipts.isEmpty()) {
                            PremiumEmptyState(
                                icon = currentTab.emptyStateIcon,
                                title = currentTab.emptyStateTitle,
                                description = currentTab.emptyStateDescription
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
                            ) {
                                items(receipts, key = { it.id }) { record ->
                                    ReceiptHistoryCard(
                                        record = record,
                                        onClick = { onNavigateToReceiptDetail(record) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        PremiumEmptyState(
                            icon = currentTab.emptyStateIcon,
                            title = currentTab.emptyStateTitle,
                            description = currentTab.emptyStateDescription
                        )
                    }
                }
            }
        }

        // 底部懸浮垃圾桶按鈕 (浮空於列表之上)
        if (selectedTabIndex == 0 && receipts.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clickable { showClearDrawer = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "清空紀錄",
                        tint = if (isDark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.70f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
