package com.lenswise.app

import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.features.scanner.ScannerUiState
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
}
