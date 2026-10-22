package com.lenswise.app.features.product.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.domain.IProductLensAgent
import com.lenswise.app.features.product.service.ProductLensService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        assertEquals(ProductLensStatus.SUCCESS, viewModel.uiState.value.status)
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
        assertEquals(ProductLensStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Network timeout") == true)
    }
}
