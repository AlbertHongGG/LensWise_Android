package com.lenswise.app.features.menu.presentation

import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.model.menu.MenuScannerItem
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.domain.IMenuScannerAgent
import com.lenswise.app.features.menu.service.MenuScannerService
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
        assertEquals(MenuScannerStatus.SUCCESS, viewModel.uiState.value.status)
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
        assertEquals(MenuScannerStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Menu OCR failure") == true)
    }
}
