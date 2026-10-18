package com.lenswise.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

/**
 * 企業級統一圖片規格化處理器。
 *
 * 核心保證：
 * 1. 防 OOM 二階段採樣：先讀 Bounds，再以 inSampleSize 載入。
 * 2. EXIF 姿態自動校正：讀取旋轉角（90/180/270）並透過 Matrix 自動翻轉。
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
            val rotationDegrees = getExifOrientation(context, uri)

            // 1. InJustDecodeBounds 獲取尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("無法開啟圖片串流: $uri")

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                error("圖片尺寸解析失敗")
            }

            // 2. 計算採樣率
            options.inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // 3. 安全解碼點陣圖
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("無法解碼圖片資料: $uri")

            processAndSave(context, sampledBitmap, rotationDegrees, source)
        }
    }

    suspend fun standardizeFromBytes(
        context: Context,
        bytes: ByteArray,
        source: ImageSource
    ): Result<AppImageEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val rotationDegrees = getExifOrientation(bytes)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                error("圖片二進位尺寸解析失敗")
            }

            options.inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val sampledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: error("無法解碼二進位圖片點陣圖")

            processAndSave(context, sampledBitmap, rotationDegrees, source)
        }
    }

    private fun processAndSave(
        context: Context,
        rawBitmap: Bitmap,
        rotationDegrees: Int,
        source: ImageSource
    ): AppImageEntity {
        // 4. 矩陣變換：旋轉 + 縮放（若長邊仍超過 MAX_DIMENSION）
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

        // 5. 生成輕量縮圖
        val thumbnailBitmap = Bitmap.createScaledBitmap(
            transformedBitmap,
            THUMBNAIL_SIZE,
            (THUMBNAIL_SIZE * (finalHeight.toFloat() / finalWidth.toFloat())).toInt().coerceAtLeast(1),
            true
        )

        // 6. 持久化寫入 App 專用暫存快取目錄
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
