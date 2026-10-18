package com.lenswise.app.core.image

import android.graphics.Bitmap
import java.io.File
import java.util.UUID

enum class ImageSource {
    CAMERA,
    GALLERY
}

/**
 * 規格化圖片領域實體模型。
 * 保證圖片路徑有效、EXIF 姿態已歸正、尺寸已降採樣，徹底隔離 OOM。
 */
data class AppImageEntity(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val thumbnailBitmap: Bitmap? = null,
    val width: Int,
    val height: Int,
    val source: ImageSource,
    val timestamp: Long = System.currentTimeMillis()
) {
    val filePath: String get() = file.absolutePath
    val uriString: String get() = file.toURI().toString()
}
