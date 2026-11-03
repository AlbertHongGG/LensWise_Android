package com.lenswise.app.features.product.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.scanner.ScanAnalysisStatus
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.domain.IProductLensAgent
import com.lenswise.app.features.product.service.ProductLensService
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

class ProductLensViewModelTest {

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
    fun testAnalyzeProduct_onSuccess_clearsConsumedImagesFromRepository() = runBlocking {
        val img1 = createDummyImageEntity("product_1")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1))

        val dummyRecord = ProductLensRecord.empty().copy(productName = "日清兵衛", brand = "日清")

        val mockAgent = mock(IProductLensAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord, isComplete = true))
            }
        )

        val mockService = mock(ProductLensService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord, listOf(img1))).thenReturn(dummyRecord)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ProductLensViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(1, imageRepo.count)
        viewModel.analyzeProduct(listOf(img1))

        assertEquals(0, imageRepo.count)
        assertEquals(ScanAnalysisStatus.SUCCESS, viewModel.uiState.value.status)
        assertEquals("日清兵衛", viewModel.uiState.value.record.productName)
    }

    @Test
    fun testAnalyzeProduct_onFailure_preservesImagesInRepository() = runBlocking {
        val img1 = createDummyImageEntity("product_1")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1))

        val mockAgent = mock(IProductLensAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                throw IOException("Network timeout")
            }
        )

        val mockService = mock(ProductLensService::class.java)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = ProductLensViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        assertEquals(1, imageRepo.count)
        viewModel.analyzeProduct(listOf(img1))

        assertEquals(1, imageRepo.count)
        assertEquals(ScanAnalysisStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Network timeout") == true)
    }

    @Test
    fun testAnalyzeProduct_secondRun_immediatelyResetsStateToConnectingAndClearsPreviousRecord() = runBlocking {
        val img1 = createDummyImageEntity("product_1")
        val img2 = createDummyImageEntity("product_2")
        val imageRepo = DefaultCapturedImageRepository(listOf(img1, img2))

        val dummyRecord1 = ProductLensRecord.empty().copy(productName = "第一次分析商品")
        val dummyRecord2 = ProductLensRecord.empty().copy(productName = "第二次分析商品")

        val gate = CompletableDeferred<Unit>()

        val mockAgent = mock(IProductLensAgent::class.java)
        `when`(mockAgent.executeStreamSnapshot(listOf(img1))).thenReturn(
            flow {
                emit(StreamingSnapshot(data = dummyRecord1, isComplete = true))
            }
        )
        `when`(mockAgent.executeStreamSnapshot(listOf(img2))).thenReturn(
            flow {
                // 等待 gate 打開前不發出任何 snapshot，模擬網路連線等待中
                gate.await()
                emit(StreamingSnapshot(data = dummyRecord2, isComplete = true))
            }
        )

        val mockService = mock(ProductLensService::class.java)
        `when`(mockService.saveCompletedRecord(dummyRecord1, listOf(img1))).thenReturn(dummyRecord1)
        `when`(mockService.saveCompletedRecord(dummyRecord2, listOf(img2))).thenReturn(dummyRecord2)

        val testScope = CoroutineScope(Dispatchers.Default)
        val viewModel = ProductLensViewModel(
            agent = mockAgent,
            service = mockService,
            capturedImageRepository = imageRepo,
            scope = testScope
        )

        // 第一次分析完成
        viewModel.analyzeProduct(listOf(img1))
        // 等待第一次完成
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        val firstSessionId = viewModel.uiState.value.sessionId
        assertEquals("第一次分析商品", viewModel.uiState.value.record.productName)

        // 啟動第二次分析
        viewModel.analyzeProduct(listOf(img2))

        // 核心驗證：在 gate 未打開、串流尚未抵達前，狀態必須「立刻同步」變為 CONNECTING，且舊資料必須被完全清空！
        assertEquals(ScanAnalysisStatus.CONNECTING, viewModel.uiState.value.status)
        assertNotEquals(firstSessionId, viewModel.uiState.value.sessionId)
        assertEquals("", viewModel.uiState.value.record.productName)

        // 解除 gate 讓第二次串流完成
        gate.complete(Unit)
        while (viewModel.uiState.value.status != ScanAnalysisStatus.SUCCESS) {
            kotlinx.coroutines.delay(10)
        }
        assertEquals("第二次分析商品", viewModel.uiState.value.record.productName)
    }
}
