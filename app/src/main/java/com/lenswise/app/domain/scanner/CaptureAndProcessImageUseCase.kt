package com.lenswise.app.domain.scanner

import com.lenswise.app.core.camera.CameraService
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.CapturedImageRepository
import com.lenswise.app.core.image.ImageProcessingService

/**
 * 拍照並規格化儲存用例 (CaptureAndProcessImageUseCase)。
 *
 * 核心保證：
 * 1. 單一職責：協調 [CameraService] 擷取、[ImageProcessingService] 規格化與 [CapturedImageRepository] 保存。
 * 2. 乾淨架構：不涉及 UI State 或 Android Framework 生命週期，純業務串接。
 * 3. 測試隔離：全建構子注入，100% 可在 JVM 透過 Fake 進行單元測試。
 */
class CaptureAndProcessImageUseCase(
    private val cameraService: CameraService,
    private val imageProcessingService: ImageProcessingService,
    private val capturedImageRepository: CapturedImageRepository
) {

    suspend operator fun invoke(): Result<AppImageEntity> {
        val captureResult = cameraService.capturePhoto()
        val capture = captureResult.getOrElse { return Result.failure(it) }

        val processResult = imageProcessingService.processCameraCapture(capture)
        val entity = processResult.getOrElse { return Result.failure(it) }

        capturedImageRepository.addImages(listOf(entity))
        return Result.success(entity)
    }
}
