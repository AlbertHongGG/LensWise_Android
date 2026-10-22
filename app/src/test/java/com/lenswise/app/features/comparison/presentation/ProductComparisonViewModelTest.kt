package com.lenswise.app.features.comparison.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.domain.IProductComparisonAgent
import com.lenswise.app.features.comparison.service.ProductComparisonService
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
        assertEquals(ProductComparisonStatus.SUCCESS, viewModel.uiState.value.status)
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
        assertEquals(ProductComparisonStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("AI service unavailable") == true)
    }
}
