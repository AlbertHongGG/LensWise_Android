package com.lenswise.app.core.image

import com.lenswise.app.core.camera.RawCameraCapture

/**
 * 影像處理規格化服務契約。
 *
 * 將 Android Context 封裝於服務建構子中，對外提供純粹領域驅動的規格化介面，
 * 徹底切斷 ViewModel 與 Presentation 層對 Android Framework 儲存與繪圖 Context 的依賴。
 */
interface ImageProcessingService {

    /**
     * 規格化相機拍攝原始成果。
     * 自動套用 PreviewView 全螢幕視口比例進行居中裁切，輸出輕量 JPEG 快取與小尺寸微縮圖。
     */
    suspend fun processCameraCapture(capture: RawCameraCapture): Result<AppImageEntity>

    /**
     * 規格化系統相簿選取之圖片 URI。
     * 自動解析 EXIF 旋轉、等比縮放長邊至 1920 像素，並生成輕量縮圖。
     */
    suspend fun processGalleryUri(uri: android.net.Uri): Result<AppImageEntity>
}
