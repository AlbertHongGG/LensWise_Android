package com.lenswise.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.max

/**
 * 企業級統一圖片規格化處理器。
 *
 * 核心保證：
 * 1. API 28+ 全面採用現代 ImageDecoder：硬體加速解碼、自動處理 EXIF 旋轉、消除串流關閉與採樣錯誤。
 * 2. API 26-27 保留 FileDescriptor 雙階段安全解碼作為極端相容性保護。
 * 3. 尺寸限制：最大長邊限制為 1920 像素，等比縮放，杜絕記憶體暴漲。
 * 4. 輸出輕量 JPEG (品質 85%) 磁碟快取，並生成 160x160 輕量縮圖供 UI Badge 與預覽使用。
 */
object ImageStandardizer {

    private const val MAX_DIMENSION = 1920
    private const val THUMBNAIL_SIZE = 160
    private const val JPEG_QUALITY = 85

    suspend fun standardizeFromUri(
        context: Context,
        uri: Uri,
        source: ImageSource
    ): Result<AppImageEntity> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ (API 28+) 官方推薦標準管道：ImageDecoder 自動處理 EXIF 與硬體降採樣
                val decoderSource = ImageDecoder.createSource(context.contentResolver, uri)
                val decodedBitmap = ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val origWidth = info.size.width
                    val origHeight = info.size.height
                    val sampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
                    if (sampleSize > 1) {
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
                // ImageDecoder 已自動校正 EXIF 旋轉，旋轉角度帶入 0
                processAndSave(context, decodedBitmap, rotationDegrees = 0, source = source)
            } else {
                // API 26-27 舊版相容安全降級管道：使用可隨意 seek 的 FileDescriptor
                val rotationDegrees = getExifOrientation(context, uri)

                val sampledBitmap = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val fd = pfd.fileDescriptor
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFileDescriptor(fd, null, boundsOptions)

                    val origWidth = boundsOptions.outWidth
                    val origHeight = boundsOptions.outHeight
                    if (origWidth <= 0 || origHeight <= 0) {
                        error("圖片尺寸解析失敗: $uri")
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeFileDescriptor(fd, null, decodeOptions)
                } ?: error("無法透過 FileDescriptor 解碼圖片: $uri")

                processAndSave(context, sampledBitmap, rotationDegrees, source)
            }
        }
    }

    suspend fun standardizeFromBytes(
        context: Context,
        bytes: ByteArray,
        source: ImageSource
    ): Result<AppImageEntity> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val byteBuffer = ByteBuffer.wrap(bytes)
                val decoderSource = ImageDecoder.createSource(byteBuffer)
                val decodedBitmap = ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val origWidth = info.size.width
                    val origHeight = info.size.height
                    val sampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
                    if (sampleSize > 1) {
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
                processAndSave(context, decodedBitmap, rotationDegrees = 0, source = source)
            } else {
                val rotationDegrees = getExifOrientation(bytes)

                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                val origWidth = boundsOptions.outWidth
                val origHeight = boundsOptions.outHeight
                if (origWidth <= 0 || origHeight <= 0) {
                    error("圖片二進位尺寸解析失敗")
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val sampledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: error("無法解碼二進位圖片點陣圖")

                processAndSave(context, sampledBitmap, rotationDegrees, source)
            }
        }
    }

    private fun processAndSave(
        context: Context,
        rawBitmap: Bitmap,
        rotationDegrees: Int,
        source: ImageSource
    ): AppImageEntity {
        // 矩陣變換：旋轉 + 縮放（若長邊仍超過 MAX_DIMENSION）
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }

        val rawMax = max(rawBitmap.width, rawBitmap.height)
        if (rawMax > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / rawMax.toFloat()
            matrix.postScale(scale, scale)
        }

        val transformedBitmap = if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
            if (transformed != rawBitmap) {
                rawBitmap.recycle()
            }
            transformed
        } else {
            rawBitmap
        }

        val finalWidth = transformedBitmap.width
        val finalHeight = transformedBitmap.height

        // 生成 160x160 輕量縮圖
        val thumbnailBitmap = Bitmap.createScaledBitmap(
            transformedBitmap,
            THUMBNAIL_SIZE,
            (THUMBNAIL_SIZE * (finalHeight.toFloat() / finalWidth.toFloat())).toInt().coerceAtLeast(1),
            true
        )

        // 持久化寫入 App 專用暫存快取目錄
        val cacheDir = File(context.cacheDir, "standardized_images").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")

        FileOutputStream(targetFile).use { fos ->
            transformedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }

        transformedBitmap.recycle()

        return AppImageEntity(
            file = targetFile,
            thumbnailBitmap = thumbnailBitmap,
            width = finalWidth,
            height = finalHeight,
            source = source
        )
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        var currentMax = max(width, height)
        while (currentMax / 2 >= maxDim) {
            inSampleSize *= 2
            currentMax /= 2
        }
        return inSampleSize
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun getExifOrientation(bytes: ByteArray): Int {
        return runCatching {
            ByteArrayInputStream(bytes).use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        }.getOrDefault(0)
    }
}
