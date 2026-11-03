package com.lenswise.app.features.comparison.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.scanner.ScanAnalysisStatus
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.domain.IProductComparisonAgent
import com.lenswise.app.features.comparison.service.ProductComparisonService
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

class ProductComparisonViewModelTest {

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
    fun testAnalyzeComparison_onSuccess_clearsConsumedImagesFromRepository() = runBlocking {
        val img1 = createDummyImageEntity("comp_1")
        val img2 = createDummyImageEntity("comp_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val dummyRecord = ProductComparisonRecord.empty().copy(title = "泡麵比對")

        val mockAgent = mock(IProductComparisonAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1, img2))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord, isComplete = true))
            }
        )

        val mockService = mock(ProductComparisonService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord, listOf(img1, img2))).thenReturn(dummyRecord)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ProductComparisonViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(2, imageRepo.count)
        viewModel.analyzeComparison(listOf(img1, img2))

        assertEquals(0, imageRepo.count)
        assertEquals(ScanAnalysisStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals("泡麵比對", viewModel.uiState.value.record.title)
    }

    @Test
    fun testAnalyzeComparison_onFailure_preservesImagesInRepository() = runBlocking {
        val img1 = createDummyImageEntity("comp_1")
        val img2 = createDummyImageEntity("comp_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val mockAgent = mock(IProductComparisonAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1, img2))).thenReturn(
            flow {
                throw IOException("AI service unavailable")
            }
        )

        val mockService = mock(ProductComparisonService::class.java)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ProductComparisonViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(2, imageRepo.count)
        viewModel.analyzeComparison(listOf(img1, img2))

        assertEquals(2, imageRepo.count)
        assertEquals(ScanAnalysisStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("AI service unavailable") == true)
    }

    @Test
    fun testAnalyzeComparison_secondRun_immediatelyResetsStateToConnectingAndClearsPreviousRecord() = runBlocking {
        val img1 = createDummyImageEntity("comp_1")
        val img2 = createDummyImageEntity("comp_2")
        val img3 = createDummyImageEntity("comp_3")
        val img4 = createDummyImageEntity("comp_4")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2, img3, img4))

        val dummyRecord1 = ProductComparisonRecord.empty().copy(title = "第一次比對")
        val dummyRecord2 = ProductComparisonRecord.empty().copy(title = "第二次比對")

        val gate = CompletableDeferred<Unit>()

        val mockAgent = mock(IProductComparisonAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1, img2))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord1, isComplete = true))
            }
        )
        `when`(mockAgent.executeStreamSnapshot(listOf(img3, img4))).thenReturn(
            flow {
                gate.await()
                emit(StreamingSnapshot(data = dummyRecord2, isComplete = true))
            }
        )

        val mockService = mock(ProductComparisonService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord1, listOf(img1, img2))).thenReturn(dummyRecord1)
        `when`(mockService.saveCompletedRecord(dummyRecord2, listOf(img3, img4))).thenReturn(dummyRecord2)

        val testScope = CoroutineScope(Dispatchers.Default)
        val viewModel = ProductComparisonViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        viewModel.analyzeComparison(listOf(img1, img2))
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        val firstSessionId = viewModel.uiState.value.sessionId
        assertEquals("第一次比對", viewModel.uiState.value.record.title)

        viewModel.analyzeComparison(listOf(img3, img4))

        // 核心驗證：在 gate 未打開前，狀態必須「立刻同步」變為 CONNECTING，且舊資料必須被完全清空！
        assertEquals(ScanAnalysisStatus.CONNECTING, viewModel.uiState.value.status)
        assertNotEquals(firstSessionId, viewModel.uiState.value.sessionId)
        assertEquals("", viewModel.uiState.value.record.title)

        gate.complete(Unit)
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        assertEquals("第二次比對", viewModel.uiState.value.record.title)
    }
}
