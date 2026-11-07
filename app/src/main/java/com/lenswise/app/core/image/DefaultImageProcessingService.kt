package com.lenswise.app.core.image

import android.content.Context
import com.lenswise.app.core.camera.RawCameraCapture

/**
 * 預設影像處理規格化服務實作。
 * 委託 [ImageStandardizer] 執行 API 28+ 現代 ImageDecoder 硬體加速解碼與比例裁切。
 */
class DefaultImageProcessingService(
    private val appContext: Context
) : ImageProcessingService {

    override suspend fun processCameraCapture(capture: RawCameraCapture): Result<AppImageEntity> {
        return ImageStandardizer.standardizeCameraCapture(
            context = appContext,
            capture = capture
        )
    }

    override suspend fun processGalleryUri(uri: android.net.Uri): Result<AppImageEntity> {
        return ImageStandardizer.standardizeGalleryUri(
            context = appContext,
            uri = uri
        )
    }
}
