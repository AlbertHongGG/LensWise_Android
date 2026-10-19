package com.lenswise.app.core.image

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
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
@Parcelize
data class AppImageEntity(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    @IgnoredOnParcel
    val thumbnailBitmap: Bitmap? = null,
    val width: Int,
    val height: Int,
    val source: ImageSource,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    val filePath: String get() = file.absolutePath
    val uriString: String get() = file.toURI().toString()
    val byteArray: ByteArray get() = file.readBytes()
}
