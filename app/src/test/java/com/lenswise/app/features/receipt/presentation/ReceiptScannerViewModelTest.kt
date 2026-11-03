package com.lenswise.app.features.receipt.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.scanner.ScanAnalysisStatus
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.domain.IReceiptScannerAgent
import com.lenswise.app.features.receipt.service.ReceiptScannerService
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

class ReceiptScannerViewModelTest {

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
    fun testAnalyzeReceipt_onSuccess_clearsConsumedImagesFromRepository() = runBlocking {
        val img1 = createDummyImageEntity("img_1")
        val img2 = createDummyImageEntity("img_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val dummyRecord = ReceiptScannerRecord.empty().copy(storeName = "Test Store", totalAmount = 100.0)

        val mockAgent = mock(IReceiptScannerAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1, img2))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord, isComplete = true))
            }
        )

        val mockService = mock(ReceiptScannerService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord, listOf(img1, img2))).thenReturn(dummyRecord)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ReceiptScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(2, imageRepo.count)

        viewModel.analyzeReceipt(listOf(img1, img2))

        // 成功完成且落地後，相片暫存池中對應的相片已被安全清除！
        assertEquals(0, imageRepo.count)
        assertEquals(ScanAnalysisStatus.SUCCESS, viewModel.uiState.value.status)
    }

    @Test
    fun testAnalyzeReceipt_onFailure_preservesImagesInRepository() = runBlocking {
        val img1 = createDummyImageEntity("img_1")
        val img2 = createDummyImageEntity("img_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val mockAgent = mock(IReceiptScannerAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1, img2))).thenReturn(
            flow {
                throw IOException("Network connection lost")
            }
        )

        val mockService = mock(ReceiptScannerService::class.java)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ReceiptScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(2, imageRepo.count)

        viewModel.analyzeReceipt(listOf(img1, img2))

        // 分析失敗時，相片暫存池中的相片必須完整保留，以利使用者重試！
        assertEquals(2, imageRepo.count)
        assertEquals(ScanAnalysisStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Network connection lost") == true)
    }

    @Test
    fun testAnalyzeReceipt_secondRun_immediatelyResetsStateToConnectingAndClearsPreviousRecord() = runBlocking {
        val img1 = createDummyImageEntity("receipt_1")
        val img2 = createDummyImageEntity("receipt_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val dummyRecord1 = ReceiptScannerRecord.empty().copy(storeName = "商家A", totalAmount = 50.0)
        val dummyRecord2 = ReceiptScannerRecord.empty().copy(storeName = "商家B", totalAmount = 150.0)

        val gate = CompletableDeferred<Unit>()

        val mockAgent = mock(IReceiptScannerAgent::class.java)
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

        val mockService = mock(ReceiptScannerService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord1, listOf(img1))).thenReturn(dummyRecord1)
        `when`(mockService.saveCompletedRecord(dummyRecord2, listOf(img2))).thenReturn(dummyRecord2)

        val testScope = CoroutineScope(Dispatchers.Default)
        val viewModel = ReceiptScannerViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        viewModel.analyzeReceipt(listOf(img1))
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        val firstSessionId = viewModel.uiState.value.sessionId
        assertEquals("商家A", viewModel.uiState.value.record.storeName)

        viewModel.analyzeReceipt(listOf(img2))

        // 核心驗證：在 gate 未打開前，狀態必須「立刻同步」變為 CONNECTING，且舊資料必須被完全清空！
        assertEquals(ScanAnalysisStatus.CONNECTING, viewModel.uiState.value.status)
        assertNotEquals(firstSessionId, viewModel.uiState.value.sessionId)
        assertEquals("", viewModel.uiState.value.record.storeName)

        gate.complete(Unit)
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        assertEquals("商家B", viewModel.uiState.value.record.storeName)
    }
}
