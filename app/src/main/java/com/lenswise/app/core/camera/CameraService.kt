package com.lenswise.app.core.camera

import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

/**
 * 相機服務抽象合約。
 *
 * 遵循依賴反轉原則 (DIP) 與介面隔離原則 (ISP)：
 * 1. 隔離底層硬體實作細節 (如 CameraX)，讓領域層與展示層僅依賴此抽象介面。
 * 2. 徹底解除 ViewModel 對 Android Context 與具體 CameraManager 的依賴。
 * 3. 支援純 JVM 單元測試時注入 FakeCameraService。
 */
interface CameraService {

    /**
     * 綁定相機預覽至 UI 視口。
     *
     * @param lifecycleOwner 生命週期擁有者 (通常為 Composable 的 LocalLifecycleOwner)
     * @param surfaceProvider PreviewView 提供的表面渲染提供者
     * @param viewportAspectRatio 視口實際寬高比 (width / height)，用於幾何校準
     */
    suspend fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        viewportAspectRatio: Float = 0f
    ): Result<Unit>

    /**
     * 觸發拍照並擷取原始相機點陣位元組與拍攝當下視口幾何比。
     */
    suspend fun capturePhoto(): Result<RawCameraCapture>

    /**
     * 線性變焦控制 (0.0f ~ 1.0f)。
     */
    fun setLinearZoom(zoom: Float)

    /**
     * 對焦與測光點設置。
     */
    fun focusOnPoint(meteringPoint: MeteringPoint)

    /**
     * 暫時解綁相機 (例如開啟全螢幕相簿時休眠相機，釋放硬體運算資源)。
     */
    fun unbind()

    /**
     * 徹底釋放相機與非同步執行緒資源。
     */
    fun release()
}
