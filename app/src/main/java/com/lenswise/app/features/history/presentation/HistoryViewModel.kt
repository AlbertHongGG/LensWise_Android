package com.lenswise.app.features.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.comparison.data.ComparisonHistoryRepository
import com.lenswise.app.features.menu.data.MenuHistoryRepository
import com.lenswise.app.features.product.data.ProductHistoryRepository
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 歷史紀錄畫面 UI 狀態。
 */
data class HistoryUiState(
    val selectedTabIndex: Int = 0,
    val showClearDrawer: Boolean = false
)

/**
 * 統一歷史紀錄視圖模型 (HistoryViewModel)。
 * 1:1 復刻 LensWise Flutter 版統一歷史架構：
 *
 * 核心保證：
 * 1. 頁籤持久化 (Tab Persistence)：透過 ViewModel 生命週期守護當前選中之 Tab，使用者進入細項再返回上一頁時，
 *    頁籤絕對保持在使用者停留的項目，杜絕跳回「收據」之缺陷。
 * 2. 業務職責單一 (Separation of Concerns)：收斂四大儲存庫（收據、商品、比對、菜單）的 Flow 監聽與清空抹除操作，
 *    徹底移除 UI Composable 散落的協程與倉庫呼叫。
 */
class HistoryViewModel(
    private val receiptRepo: ReceiptHistoryRepository,
    private val productRepo: ProductHistoryRepository,
    private val comparisonRepo: ComparisonHistoryRepository,
    private val menuRepo: MenuHistoryRepository,
    initialTab: Int = 0
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(selectedTabIndex = initialTab))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val receipts: StateFlow<List<ReceiptScannerRecord>> = receiptRepo.receiptsFlow
    val products: StateFlow<List<ProductLensRecord>> = productRepo.productsFlow
    val comparisons: StateFlow<List<ProductComparisonRecord>> = comparisonRepo.comparisonsFlow
    val menus: StateFlow<List<MenuScannerRecord>> = menuRepo.menusFlow

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun openClearDrawer() {
        _uiState.update { it.copy(showClearDrawer = true) }
    }

    fun closeClearDrawer() {
        _uiState.update { it.copy(showClearDrawer = false) }
    }

    fun clearCurrentTab() {
        viewModelScope.launch {
            when (_uiState.value.selectedTabIndex) {
                0 -> receiptRepo.clearAll()
                1 -> productRepo.clearAll()
                2 -> comparisonRepo.clearAll()
                3 -> menuRepo.clearAll()
            }
            closeClearDrawer()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LensWiseApplication
                HistoryViewModel(
                    receiptRepo = app.receiptHistoryRepository,
                    productRepo = app.productHistoryRepository,
                    comparisonRepo = app.comparisonHistoryRepository,
                    menuRepo = app.menuHistoryRepository
                )
            }
        }
    }
}
