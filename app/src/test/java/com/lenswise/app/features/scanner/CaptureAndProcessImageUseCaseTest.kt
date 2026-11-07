package com.lenswise.app.features.scanner

import com.lenswise.app.core.camera.CameraService
import com.lenswise.app.core.camera.RawCameraCapture
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.DefaultCapturedImageRepository
import com.lenswise.app.core.image.ImageProcessingService
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.scanner.CaptureAndProcessImageUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CaptureAndProcessImageUseCaseTest {

    private fun createDummyEntity(name: String = "photo.jpg"): AppImageEntity {
        return AppImageEntity(
            file = File(name),
            width = 1920,
            height = 1080,
            source = ImageSource.CAMERA
        )
    }

    private class FakeCameraService(
        var captureResult: Result<RawCameraCapture> = Result.success(
            RawCameraCapture(
                jpegBytes = byteArrayOf(0x01, 0x02, 0x03),
                viewportAspectRatio = 0.5625f
            )
        )
    ) : CameraService {
        override suspend fun bindPreview(
            lifecycleOwner: androidx.lifecycle.LifecycleOwner,
            surfaceProvider: androidx.camera.core.Preview.SurfaceProvider,
            viewportAspectRatio: Float
        ): Result<Unit> = Result.success(Unit)

        override suspend fun capturePhoto(): Result<RawCameraCapture> = captureResult
        override fun setLinearZoom(zoom: Float) {}
        override fun focusOnPoint(meteringPoint: androidx.camera.core.MeteringPoint) {}
        override fun unbind() {}
        override fun release() {}
    }

    private class FakeImageProcessingService(
        var processResult: Result<AppImageEntity>
    ) : ImageProcessingService {
        override suspend fun processCameraCapture(capture: RawCameraCapture): Result<AppImageEntity> = processResult
        override suspend fun processGalleryUri(uri: android.net.Uri): Result<AppImageEntity> = processResult
    }

    @Test
    fun testCaptureAndProcessSuccessFlow() = runBlocking {
        val dummyEntity = createDummyEntity()
        val fakeCamera = FakeCameraService()
        val fakeProcessor = FakeImageProcessingService(Result.success(dummyEntity))
        val repository = DefaultCapturedImageRepository()

        val useCase = CaptureAndProcessImageUseCase(
            cameraService = fakeCamera,
            imageProcessingService = fakeProcessor,
            capturedImageRepository = repository
        )

        val result = useCase()
        assertTrue("用例執行應成功", result.isSuccess)
        assertEquals(dummyEntity, result.getOrNull())
        assertEquals("暫存池中應包含 1 張照片", 1, repository.images.value.size)
        assertEquals(dummyEntity, repository.images.value.first())
    }

    @Test
    fun testCaptureFailurePropagatesAndDoesNotAddToRepository() = runBlocking {
        val fakeCamera = FakeCameraService(Result.failure(IllegalStateException("相機硬體異常")))
        val fakeProcessor = FakeImageProcessingService(Result.success(createDummyEntity()))
        val repository = DefaultCapturedImageRepository()

        val useCase = CaptureAndProcessImageUseCase(
            cameraService = fakeCamera,
            imageProcessingService = fakeProcessor,
            capturedImageRepository = repository
        )

        val result = useCase()
        assertTrue("拍照失敗時用例應回傳 failure", result.isFailure)
        assertEquals("相機硬體異常", result.exceptionOrNull()?.message)
        assertEquals("暫存池應為空", 0, repository.images.value.size)
    }

    @Test
    fun testProcessingFailurePropagatesAndDoesNotAddToRepository() = runBlocking {
        val fakeCamera = FakeCameraService()
        val fakeProcessor = FakeImageProcessingService(Result.failure(IllegalArgumentException("圖片規格化失敗")))
        val repository = DefaultCapturedImageRepository()

        val useCase = CaptureAndProcessImageUseCase(
            cameraService = fakeCamera,
            imageProcessingService = fakeProcessor,
            capturedImageRepository = repository
        )

        val result = useCase()
        assertTrue("規格化失敗時用例應回傳 failure", result.isFailure)
        assertEquals("圖片規格化失敗", result.exceptionOrNull()?.message)
        assertEquals("暫存池應為空", 0, repository.images.value.size)
    }
}
