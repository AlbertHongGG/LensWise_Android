package com.lenswise.app.features.menu.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.scanner.ScanAnalysisStatus
import com.lenswise.app.domain.model.menu.MenuScannerItem
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.domain.IMenuScannerAgent
import com.lenswise.app.features.menu.service.MenuScannerService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.IOException

class MenuScannerViewModelTest {

    private fun createDummyImageEntity(id: String): AppImageEntity {
        return AppImageEntity(
            id = id,
            file = File("dummy_$id.jpg"),
            width = 1000,
            height = 1000,
            source = ImageSource.CAMERA
        )
    }

    @Test
    fun testAnalyzeMenu_onSuccess_clearsConsumedImagesFromRepository() = runBlocking {
        val img1 = createDummyImageEntity("menu_1")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1))

        val dummyRecord = MenuScannerRecord.empty().copy(
            storeName = "一蘭拉麵",
            items = listOf(MenuScannerItem(originalName = "ラーメン", translatedName = "拉麵", price = 890.0))
        )

        val mockAgent = mock(IMenuScannerAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord, isComplete = true))
            }
        )

        val mockService = mock(MenuScannerService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord, listOf(img1))).thenReturn(dummyRecord)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = MenuScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(1, imageRepo.count)
        viewModel.analyzeMenu(listOf(img1))

        assertEquals(0, imageRepo.count)
        assertEquals(ScanAnalysisStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals("一蘭拉麵", viewModel.uiState.value.record.storeName)
    }

    @Test
    fun testAnalyzeMenu_onFailure_preservesImagesInRepository() = runBlocking {
        val img1 = createDummyImageEntity("menu_1")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1))

        val mockAgent = mock(IMenuScannerAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                throw IOException("Menu OCR failure")
            }
        )

        val mockService = mock(MenuScannerService::class.java)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = MenuScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(1, imageRepo.count)
        viewModel.analyzeMenu(listOf(img1))

        assertEquals(1, imageRepo.count)
        assertEquals(ScanAnalysisStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Menu OCR failure") == true)
    }

    @Test
    fun testAnalyzeMenu_secondRun_immediatelyResetsStateToConnectingAndClearsPreviousRecord() = runBlocking {
        val img1 = createDummyImageEntity("menu_1")
        val img2 = createDummyImageEntity("menu_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val dummyRecord1 = MenuScannerRecord.empty().copy(storeName = "第一次菜單")
        val dummyRecord2 = MenuScannerRecord.empty().copy(storeName = "第二次菜單")

        val gate = CompletableDeferred<Unit>()

        val mockAgent = mock(IMenuScannerAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord1, isComplete = true))
            }
        )
        `when`(mockAgent.executeStreamSnapshot(listOf(img2))).thenReturn(
            flow {
                gate.await()
                emit(StreamingSnapshot(data = dummyRecord2, isComplete = true))
            }
        )

        val mockService = mock(MenuScannerService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord1, listOf(img1))).thenReturn(dummyRecord1)
        `when`(mockService.saveCompletedRecord(dummyRecord2, listOf(img2))).thenReturn(dummyRecord2)

        val testScope = CoroutineScope(Dispatchers.Default)
        val viewModel = MenuScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        viewModel.analyzeMenu(listOf(img1))
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        val firstSessionId = viewModel.uiState.value.sessionId
        assertEquals("第一次菜單", viewModel.uiState.value.record.storeName)

        viewModel.analyzeMenu(listOf(img2))

        // 核心驗證：在 gate 未打開前，狀態必須「立刻同步」變為 CONNECTING，且舊資料必須被完全清空！
        assertEquals(ScanAnalysisStatus.CONNECTING, viewModel.uiState.value.status)
        assertNotEquals(firstSessionId, viewModel.uiState.value.sessionId)
        assertEquals("", viewModel.uiState.value.record.storeName)

        gate.complete(Unit)
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        assertEquals("第二次菜單", viewModel.uiState.value.record.storeName)
    }
}
