package com.lenswise.app.core.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 硬體管理器。
 * 封裝預覽綁定、縮放手勢、點擊對焦與無閃光燈拍照。
 */
class CameraManager(
    private val context: Context
) {
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    private fun getExecutor(): java.util.concurrent.ExecutorService {
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        return cameraExecutor
    }

    val cameraControl: CameraControl? get() = camera?.cameraControl
    val cameraInfo: CameraInfo? get() = camera?.cameraInfo

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF) // 依需求無閃光燈
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                if (continuation.isActive) {
                    continuation.resume(true)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setLinearZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0f, 1f)
        cameraControl?.setLinearZoom(clamped)
    }

    fun focusOnPoint(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        cameraControl?.startFocusAndMetering(action)
    }

    suspend fun capturePhoto(): Result<ByteArray> = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resume(Result.failure(IllegalStateException("相機尚未就緒")))
            return@suspendCancellableCoroutine
        }

        capture.takePicture(
            getExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        if (continuation.isActive) {
                            continuation.resume(Result.success(bytes))
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

    fun release() {
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
        camera = null
        imageCapture = null
    }
}
