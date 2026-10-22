package com.lenswise.app

import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.storage.ScannerPreferencesDataSource
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.features.scanner.ScannerUiState
import com.lenswise.app.presentation.features.scanner.ScannerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScannerLogicTest {

    private fun createDummyImageEntity(): AppImageEntity {
        return AppImageEntity(
            file = File("dummy.jpg"),
            width = 1920,
            height = 1080,
            source = ImageSource.CAMERA
        )
    }

    private class FakeScannerPreferencesDataSource(
        private var currentMode: ScannerMode = ScannerMode.PRODUCT
    ) : ScannerPreferencesDataSource {
        override fun getSavedMode(): ScannerMode = currentMode
        override fun saveMode(mode: ScannerMode) {
            this.currentMode = mode
        }
    }

    @Test
    fun testScannerModeMinShots() {
        assertEquals(1, ScannerMode.RECEIPT.minShots)
        assertEquals(1, ScannerMode.PRODUCT.minShots)
        assertEquals(2, ScannerMode.COMPARISON.minShots)
        assertEquals(1, ScannerMode.MENU.minShots)
    }

    @Test
    fun testCanTriggerAnalysisConditions() {
        // 1. 0 張照片時，任何模式皆不可觸發分析
        val emptyState = ScannerUiState(
            currentMode = ScannerMode.PRODUCT,
            capturedImages = emptyList()
        )
        assertFalse(emptyState.canTriggerAnalysis)

        // 2. 1 張照片時：商品、收據、菜單可觸發，比對模式不可觸發
        val oneImageState = ScannerUiState(
            currentMode = ScannerMode.PRODUCT,
            capturedImages = listOf(createDummyImageEntity())
        )
        assertTrue(oneImageState.canTriggerAnalysis)

        val oneImageCompareState = ScannerUiState(
            currentMode = ScannerMode.COMPARISON,
            capturedImages = listOf(createDummyImageEntity())
        )
        assertFalse(oneImageCompareState.canTriggerAnalysis)

        // 3. 2 張照片時：比對模式可以觸發
        val twoImageCompareState = ScannerUiState(
            currentMode = ScannerMode.COMPARISON,
            capturedImages = listOf(createDummyImageEntity(), createDummyImageEntity())
        )
        assertTrue(twoImageCompareState.canTriggerAnalysis)
    }

    @Test
    fun testScannerViewModelInitialModeRestoration() {
        val fakeSource = FakeScannerPreferencesDataSource(currentMode = ScannerMode.COMPARISON)
        val viewModel = ScannerViewModel(fakeSource)

        assertEquals(ScannerMode.COMPARISON, viewModel.uiState.value.currentMode)
    }

    @Test
    fun testScannerViewModelSetModeUpdatesStateAndPersistence() {
        val fakeSource = FakeScannerPreferencesDataSource(currentMode = ScannerMode.RECEIPT)
        val viewModel = ScannerViewModel(fakeSource)

        viewModel.setMode(ScannerMode.MENU)

        assertEquals(ScannerMode.MENU, viewModel.uiState.value.currentMode)
        assertEquals(ScannerMode.MENU, fakeSource.getSavedMode())
    }

    @Test
    fun testScannerViewModel_imagesPersistAcrossModeSwitching() {
        val fakeSource = FakeScannerPreferencesDataSource(currentMode = ScannerMode.RECEIPT)
        val img1 = createDummyImageEntity()
        val img2 = createDummyImageEntity()
        val fakeRepo = com.lenswise.app.core.image.DefaultCapturedImageRepository(listOf(img1, img2))
        val viewModel = ScannerViewModel(fakeSource, fakeRepo)

        // 初始狀態下包含 2 張照片
        assertEquals(2, viewModel.uiState.value.imageCount)
        assertEquals(ScannerMode.RECEIPT, viewModel.uiState.value.currentMode)

        // 切換至「商品」模式
        viewModel.setMode(ScannerMode.PRODUCT)
        assertEquals(ScannerMode.PRODUCT, viewModel.uiState.value.currentMode)
        assertEquals(2, viewModel.uiState.value.imageCount) // 照片依然保持存在！

        // 切換至「比對」模式
        viewModel.setMode(ScannerMode.COMPARISON)
        assertEquals(ScannerMode.COMPARISON, viewModel.uiState.value.currentMode)
        assertEquals(2, viewModel.uiState.value.imageCount) // 照片依然保持存在！

        // 切換至「菜單」模式
        viewModel.setMode(ScannerMode.MENU)
        assertEquals(ScannerMode.MENU, viewModel.uiState.value.currentMode)
        assertEquals(2, viewModel.uiState.value.imageCount) // 照片依然保持存在！
    }
}
