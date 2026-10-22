package com.lenswise.app.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.domain.model.history.HistoryTabConfig
import com.lenswise.app.domain.model.history.IHistoryRecord
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.history.components.ComparisonHistoryCard
import com.lenswise.app.features.history.components.MenuHistoryCard
import com.lenswise.app.features.history.components.ProductHistoryCard
import com.lenswise.app.features.history.components.ReceiptHistoryCard
import com.lenswise.app.features.history.presentation.HistoryViewModel
import com.lenswise.app.presentation.components.LensWisePillTabBar
import com.lenswise.app.presentation.components.PremiumEmptyState
import com.lenswise.app.presentation.components.SwipeToConfirmDrawer
import com.lenswise.app.presentation.components.trash.DragDropTrashContainer
import com.lenswise.app.presentation.components.trash.GlassmorphicTrashControl
import com.lenswise.app.presentation.components.trash.draggableToTrash
import com.lenswise.app.presentation.components.trash.rememberDragDropTrashState
import com.lenswise.app.presentation.theme.AppColors

/**
 * 統一歷史紀錄畫面 (HistoryScreen)。
 * 1:1 復刻 LensWise Flutter 版 UnifiedHistoryPage：
 * 頂部支援 4 大核心業務膠囊分頁（收據、商品、對比、菜單），
 * 具備精美空狀態插畫、專屬卡片列表與懸浮垃圾桶滑動抹除抽屜 (SwipeToConfirmDrawer)。
 *
 * 架構保證：
 * 1. 由 HistoryViewModel 完整守護頁籤狀態與資料流，點進細項後返回保證保持於當前選中 Tab。
 * 2. 支援 GeminiFlowApp 風格長按抓取卡片並拖曳進垃圾桶單獨刪除。
 * 3. 懸浮垃圾桶僅針對「當前選中 Tab」進行全部分類清空。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    initialTab: Int = 0,
    viewModel: HistoryViewModel? = null,
    onNavigateToReceiptDetail: (ReceiptScannerRecord) -> Unit = {},
    onNavigateToProductDetail: (ProductLensRecord) -> Unit = {},
    onNavigateToComparisonResult: (ProductComparisonRecord) -> Unit = {},
    onNavigateToMenuResult: (MenuScannerRecord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val vm: HistoryViewModel = viewModel ?: viewModel(factory = HistoryViewModel.Factory)
    val uiState by vm.uiState.collectAsState()
    val selectedTabIndex = uiState.selectedTabIndex
    val dragDropState = rememberDragDropTrashState<IHistoryRecord>()

    val tabs = remember {
        listOf(
            HistoryTabConfig(
                id = "receipt",
                tabName = "收據",
                tabIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                emptyStateTitle = "尚無發票帳目紀錄",
                emptyStateDescription = "尚未儲存任何發票明細",
                emptyStateIcon = Icons.AutoMirrored.Filled.ReceiptLong
            ),
            HistoryTabConfig(
                id = "product",
                tabName = "商品",
                tabIcon = Icons.Default.QrCodeScanner,
                emptyStateTitle = "尚無商品解析紀錄",
                emptyStateDescription = "尚未儲存任何商品特徵",
                emptyStateIcon = Icons.Default.QrCodeScanner
            ),
            HistoryTabConfig(
                id = "comparison",
                tabName = "對比",
                tabIcon = Icons.AutoMirrored.Filled.CompareArrows,
                emptyStateTitle = "尚無商品對比紀錄",
                emptyStateDescription = "尚未儲存任何商品對比",
                emptyStateIcon = Icons.AutoMirrored.Filled.CompareArrows
            ),
            HistoryTabConfig(
                id = "menu",
                tabName = "菜單",
                tabIcon = Icons.Default.RestaurantMenu,
                emptyStateTitle = "尚無菜單解析紀錄",
                emptyStateDescription = "尚未儲存任何菜單紀錄",
                emptyStateIcon = Icons.Default.RestaurantMenu
            )
        )
    }

    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]
    val receipts by vm.receipts.collectAsState()
    val products by vm.products.collectAsState()
    val comparisons by vm.comparisons.collectAsState()
    val menus by vm.menus.collectAsState()

    val hasActiveRecords = when (selectedTabIndex) {
        0 -> receipts.isNotEmpty()
        1 -> products.isNotEmpty()
        2 -> comparisons.isNotEmpty()
        3 -> menus.isNotEmpty()
        else -> false
    }

    if (uiState.showClearDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR ${currentTab.tabName.uppercase()} HISTORY",
            onConfirm = {
                vm.clearCurrentTab()
            },
            onDismiss = { vm.closeClearDrawer() }
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
                vm.deleteRecord(item)
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 頂部膠囊分頁欄 (Pill TabBar，固定 3 席次視窗與平滑滑動指示器)
                LensWisePillTabBar(
                    items = tabs,
                    selectedIndex = selectedTabIndex,
                    onItemSelected = { vm.selectTab(it) },
                    itemTitle = { it.tabName },
                    itemIcon = { it.tabIcon }
                )

                // 2. 分頁內容容器
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
                                        val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == record.id
                                        ReceiptHistoryCard(
                                            record = record,
                                            onClick = { onNavigateToReceiptDetail(record) },
                                            modifier = Modifier
                                                .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                                .draggableToTrash(
                                                    item = record,
                                                    state = dragDropState,
                                                    feedback = { ReceiptHistoryCard(record = record) }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (products.isEmpty()) {
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
                                    items(products, key = { it.id }) { record ->
                                        val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == record.id
                                        ProductHistoryCard(
                                            record = record,
                                            onClick = { onNavigateToProductDetail(record) },
                                            modifier = Modifier
                                                .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                                .draggableToTrash(
                                                    item = record,
                                                    state = dragDropState,
                                                    feedback = { ProductHistoryCard(record = record) }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (comparisons.isEmpty()) {
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
                                    items(comparisons, key = { it.id }) { record ->
                                        val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == record.id
                                        ComparisonHistoryCard(
                                            record = record,
                                            onClick = { onNavigateToComparisonResult(record) },
                                            modifier = Modifier
                                                .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                                .draggableToTrash(
                                                    item = record,
                                                    state = dragDropState,
                                                    feedback = { ComparisonHistoryCard(record = record) }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (menus.isEmpty()) {
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
                                    items(menus, key = { it.id }) { record ->
                                        val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == record.id
                                        MenuHistoryCard(
                                            record = record,
                                            onClick = { onNavigateToMenuResult(record) },
                                            modifier = Modifier
                                                .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                                .draggableToTrash(
                                                    item = record,
                                                    state = dragDropState,
                                                    feedback = { MenuHistoryCard(record = record) }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. 底部懸浮毛玻璃垃圾桶按鈕 (僅當前 Tab 有紀錄時浮現，支援點擊清空與拖曳感應懸停)
            if (hasActiveRecords) {
                GlassmorphicTrashControl(
                    onClick = { vm.openClearDrawer() },
                    isHovering = dragDropState.isHoveringTrash,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            dragDropState.trashBoundsInWindow = coordinates.boundsInWindow()
                        }
                )
            }
        }
    }
}
