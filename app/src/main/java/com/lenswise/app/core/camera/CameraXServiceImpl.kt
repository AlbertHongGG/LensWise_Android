package com.lenswise.app.core.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * 基於 AndroidX CameraX 1.4+ 的現代化相機服務實作。
 *
 * 架構特點：
 * 1. 結構化並行：使用 [Dispatchers.IO.asExecutor] 作為拍攝回調執行器，免除傳統 raw Executor 手動管理與洩漏風險。
 * 2. 協程安全：[bindPreview] 與 [capturePhoto] 均基於 [suspendCancellableCoroutine] 實現，支援取消傳遞。
 * 3. 視口幾何同步：精確保存拍攝時的 PreviewView 幾何比例，提供後續 Single-Pass 居中裁切所見即所得。
 */
class CameraXServiceImpl(
    private val appContext: Context
) : CameraService {

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentViewportAspectRatio: Float = 0f

    private val captureExecutor = Dispatchers.IO.asExecutor()

    override suspend fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        viewportAspectRatio: Float
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        this.currentViewportAspectRatio = viewportAspectRatio
        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                this.cameraProvider = provider
                provider.unbindAll()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(surfaceProvider)
                    }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .build()
                this.imageCapture = capture

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                this.camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )

                if (continuation.isActive) {
                    continuation.resume(Result.success(Unit))
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    override suspend fun capturePhoto(): Result<RawCameraCapture> = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resume(Result.failure(IllegalStateException("相機尚未就緒或未綁定生命週期")))
            return@suspendCancellableCoroutine
        }

        val capturedRatio = currentViewportAspectRatio

        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.success(
                                    RawCameraCapture(
                                        jpegBytes = bytes,
                                        viewportAspectRatio = capturedRatio
                                    )
                                )
                            )
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(e))
                        }
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            }
        )
    }

    override fun setLinearZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0f, 1f)
        camera?.cameraControl?.setLinearZoom(clamped)
    }

    override fun focusOnPoint(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(2000, TimeUnit.MILLISECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun unbind() {
        ContextCompat.getMainExecutor(appContext).execute {
            runCatching {
                cameraProvider?.unbindAll()
            }
            camera = null
            imageCapture = null
        }
    }

    override fun release() {
        unbind()
        cameraProvider = null
    }
}
