package com.lenswise.app.core.image

import kotlin.math.roundToInt

/**
 * 矩形裁剪區域（以原始點陣圖 Buffer 座標系為基準）。
 */
data class CropRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * 點陣圖幾何轉換計畫。
 * 封裝單次 Bitmap.createBitmap 所需的全部轉換參數。
 */
data class ImageTransformationPlan(
    val sourceCrop: CropRect,
    val rotationDegrees: Int,
    val scaleFactor: Float,
    val outputWidth: Int,
    val outputHeight: Int,
    val isIdentity: Boolean
)

/**
 * 純 Kotlin 影像幾何運算核心。
 *
 * 核心保證：
 * 1. 零 Android Framework UI 依賴，100% 支援標準 JVM 單元測試。
 * 2. 精確實現 PreviewView.ScaleType.FILL_CENTER 之等效居中裁切。
 * 3. 支援 EXIF 旋轉逆投影座標映射 (0°, 90°, 180°, 270°)。
 * 4. 單次轉換計畫推導，杜絕中繼暫存 Bitmap 分配。
 */
object ImageGeometryCalculator {

    fun calculatePlan(
        rawWidth: Int,
        rawHeight: Int,
        rotationDegrees: Int = 0,
        cropPolicy: ImageCropPolicy = ImageCropPolicy.None,
        maxDimension: Int = 1920
    ): ImageTransformationPlan {
        if (rawWidth <= 0 || rawHeight <= 0) {
            return ImageTransformationPlan(
                sourceCrop = CropRect(0, 0, 1, 1),
                rotationDegrees = 0,
                scaleFactor = 1.0f,
                outputWidth = 1,
                outputHeight = 1,
                isIdentity = true
            )
        }

        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val isOrientationSwapped = normalizedRotation == 90 || normalizedRotation == 270

        // 視覺方向下的寬與高
        val visualWidth = if (isOrientationSwapped) rawHeight else rawWidth
        val visualHeight = if (isOrientationSwapped) rawWidth else rawHeight
        val visualRatio = visualWidth.toFloat() / visualHeight.toFloat()

        // 依據裁切策略計算視覺方向下的裁切尺寸
        val (vCropW, vCropH) = when (cropPolicy) {
            is ImageCropPolicy.CenterFillAspectRatio -> {
                val targetRatio = cropPolicy.targetAspectRatio
                if (targetRatio > 0f && !targetRatio.isNaN() && !targetRatio.isInfinite()) {
                    when {
                        visualRatio > targetRatio -> {
                            // 視覺畫面比視口更寬：裁切左右兩側
                            val targetWidth = (visualHeight * targetRatio).roundToInt().coerceIn(1, visualWidth)
                            targetWidth to visualHeight
                        }
                        visualRatio < targetRatio -> {
                            // 視覺畫面比視口更窄：裁切上下兩側
                            val targetHeight = (visualWidth / targetRatio).roundToInt().coerceIn(1, visualHeight)
                            visualWidth to targetHeight
                        }
                        else -> {
                            visualWidth to visualHeight
                        }
                    }
                } else {
                    visualWidth to visualHeight
                }
            }
            ImageCropPolicy.None -> {
                visualWidth to visualHeight
            }
        }

        // 將視覺裁切尺寸映射回原始 Buffer 座標系
        val srcW = if (isOrientationSwapped) vCropH else vCropW
        val srcH = if (isOrientationSwapped) vCropW else vCropH

        val srcX = (rawWidth - srcW) / 2
        val srcY = (rawHeight - srcH) / 2

        val clampedSrcX = srcX.coerceIn(0, rawWidth - 1)
        val clampedSrcY = srcY.coerceIn(0, rawHeight - 1)
        val clampedSrcW = srcW.coerceIn(1, rawWidth - clampedSrcX)
        val clampedSrcH = srcH.coerceIn(1, rawHeight - clampedSrcY)

        val sourceCrop = CropRect(
            x = clampedSrcX,
            y = clampedSrcY,
            width = clampedSrcW,
            height = clampedSrcH
        )

        // 裁切後的實際視覺寬高
        val postCropVisualW = if (isOrientationSwapped) clampedSrcH else clampedSrcW
        val postCropVisualH = if (isOrientationSwapped) clampedSrcW else clampedSrcH

        val maxVisualDim = maxOf(postCropVisualW, postCropVisualH)
        val scaleFactor = if (maxVisualDim > maxDimension && maxDimension > 0) {
            maxDimension.toFloat() / maxVisualDim.toFloat()
        } else {
            1.0f
        }

        val outputWidth = (postCropVisualW * scaleFactor).roundToInt().coerceAtLeast(1)
        val outputHeight = (postCropVisualH * scaleFactor).roundToInt().coerceAtLeast(1)

        val isIdentity = sourceCrop.x == 0 &&
                sourceCrop.y == 0 &&
                sourceCrop.width == rawWidth &&
                sourceCrop.height == rawHeight &&
                normalizedRotation == 0 &&
                scaleFactor == 1.0f

        return ImageTransformationPlan(
            sourceCrop = sourceCrop,
            rotationDegrees = normalizedRotation,
            scaleFactor = scaleFactor,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            isIdentity = isIdentity
        )
    }
}
