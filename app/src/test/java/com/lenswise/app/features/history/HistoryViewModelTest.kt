package com.lenswise.app.features.history

import com.lenswise.app.features.comparison.data.ComparisonHistoryRepository
import com.lenswise.app.features.history.presentation.HistoryViewModel
import com.lenswise.app.features.menu.data.MenuHistoryRepository
import com.lenswise.app.features.product.data.ProductHistoryRepository
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class HistoryViewModelTest {

    private lateinit var receiptRepo: ReceiptHistoryRepository
    private lateinit var productRepo: ProductHistoryRepository
    private lateinit var comparisonRepo: ComparisonHistoryRepository
    private lateinit var menuRepo: MenuHistoryRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        receiptRepo = Mockito.mock(ReceiptHistoryRepository::class.java)
        productRepo = Mockito.mock(ProductHistoryRepository::class.java)
        comparisonRepo = Mockito.mock(ComparisonHistoryRepository::class.java)
        menuRepo = Mockito.mock(MenuHistoryRepository::class.java)

        viewModel = HistoryViewModel(
            receiptRepo = receiptRepo,
            productRepo = productRepo,
            comparisonRepo = comparisonRepo,
            menuRepo = menuRepo,
            initialTab = 0,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial state respects initialTab and clear drawer closed`() {
        assertEquals(0, viewModel.uiState.value.selectedTabIndex)
        assertFalse(viewModel.uiState.value.showClearDrawer)
    }

    @Test
    fun `selectTab updates selectedTabIndex and persists across calls`() {
        viewModel.selectTab(2) // Select 'Comparison'
        assertEquals(2, viewModel.uiState.value.selectedTabIndex)

        viewModel.selectTab(1) // Select 'Product'
        assertEquals(1, viewModel.uiState.value.selectedTabIndex)
    }

    @Test
    fun `clear drawer opens and closes properly`() {
        assertFalse(viewModel.uiState.value.showClearDrawer)
        viewModel.openClearDrawer()
        assertTrue(viewModel.uiState.value.showClearDrawer)
        viewModel.closeClearDrawer()
        assertFalse(viewModel.uiState.value.showClearDrawer)
    }

    @Test
    fun `deleteRecord dispatches to correct repository`() = kotlinx.coroutines.runBlocking {
        val receiptRecord = com.lenswise.app.domain.model.receipt.ReceiptScannerRecord(id = "rec-1")
        viewModel.deleteRecord(receiptRecord)
        Mockito.verify(receiptRepo).delete("rec-1")

        val productRecord = com.lenswise.app.domain.model.product.ProductLensRecord(id = "prod-1")
        viewModel.deleteRecord(productRecord)
        Mockito.verify(productRepo).delete("prod-1")
    }

    @Test
    fun `clearCurrentTab only clears active repository and closes drawer`() = kotlinx.coroutines.runBlocking {
        viewModel.selectTab(0)
        viewModel.openClearDrawer()
        viewModel.clearCurrentTab()
        Mockito.verify(receiptRepo).clearAll()
        Mockito.verify(productRepo, Mockito.never()).clearAll()
        assertFalse(viewModel.uiState.value.showClearDrawer)

        viewModel.selectTab(2)
        viewModel.openClearDrawer()
        viewModel.clearCurrentTab()
        Mockito.verify(comparisonRepo).clearAll()
        Mockito.verify(menuRepo, Mockito.never()).clearAll()
        assertFalse(viewModel.uiState.value.showClearDrawer)
    }
}
