package com.lenswise.app.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageGeometryCalculatorTest {

    @Test
    fun `gallery mode with ImageCropPolicy None retains full image without cropping`() {
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 4000,
            rawHeight = 3000,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.None,
            maxDimension = 1920
        )

        // 應保留完整原圖區域
        assertEquals(0, plan.sourceCrop.x)
        assertEquals(0, plan.sourceCrop.y)
        assertEquals(4000, plan.sourceCrop.width)
        assertEquals(3000, plan.sourceCrop.height)
        assertEquals(0, plan.rotationDegrees)

        // 4000 等比縮放至長邊 1920 -> 1920 x 1440
        assertEquals(1920, plan.outputWidth)
        assertEquals(1440, plan.outputHeight)
        assertEquals(1920f / 4000f, plan.scaleFactor, 0.001f)
        assertFalse(plan.isIdentity)
    }

    @Test
    fun `gallery mode under max dimension with 0 rotation is identity`() {
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 800,
            rawHeight = 600,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.None,
            maxDimension = 1920
        )

        assertEquals(0, plan.sourceCrop.x)
        assertEquals(0, plan.sourceCrop.y)
        assertEquals(800, plan.sourceCrop.width)
        assertEquals(600, plan.sourceCrop.height)
        assertEquals(800, plan.outputWidth)
        assertEquals(600, plan.outputHeight)
        assertEquals(1.0f, plan.scaleFactor, 0.001f)
        assertTrue(plan.isIdentity)
    }

    @Test
    fun `camera mode 4-3 upright with full screen 0-45 aspect ratio crops sides centered`() {
        // 直向已歸正的照片：寬 3000, 高 4000 (比例 0.75)
        // 目標視口比例：1080 / 2400 = 0.45
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 3000,
            rawHeight = 4000,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(1080f / 2400f),
            maxDimension = 1920
        )

        // 裁切目標寬度 = 4000 * 0.45 = 1800
        // X 起始點 = (3000 - 1800) / 2 = 600
        assertEquals(600, plan.sourceCrop.x)
        assertEquals(0, plan.sourceCrop.y)
        assertEquals(1800, plan.sourceCrop.width)
        assertEquals(4000, plan.sourceCrop.height)

        // 縮放係數 = 1920 / 4000 = 0.48
        assertEquals(1920f / 4000f, plan.scaleFactor, 0.001f)
        // 輸出尺寸 = 1800 * 0.48 = 864, 4000 * 0.48 = 1920
        assertEquals(864, plan.outputWidth)
        assertEquals(1920, plan.outputHeight)
        // 最終輸出寬高比應精準吻合 0.45
        assertEquals(864f / 1920f, 1080f / 2400f, 0.001f)
    }

    @Test
    fun `camera mode with 90 degree EXIF rotation maps crop to unrotated buffer correctly`() {
        // 相機感光元件原始橫向 Buffer：寬 4000, 高 3000，EXIF 標註旋轉 90 度順時針
        // 視覺方向為：寬 3000, 高 4000
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 4000,
            rawHeight = 3000,
            rotationDegrees = 90,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(1080f / 2400f),
            maxDimension = 1920
        )

        // 視覺上寬 3000 需裁成 1800 (對應 rawHeight 3000 裁成 1800，兩端各去 600)
        // 視覺上高 4000 全部保留 (對應 rawWidth 4000 全部保留)
        assertEquals(0, plan.sourceCrop.x)
        assertEquals(600, plan.sourceCrop.y)
        assertEquals(4000, plan.sourceCrop.width)
        assertEquals(1800, plan.sourceCrop.height)
        assertEquals(90, plan.rotationDegrees)

        // 旋轉並縮放後的輸出尺寸為 864 x 1920
        assertEquals(864, plan.outputWidth)
        assertEquals(1920, plan.outputHeight)
        assertEquals(864f / 1920f, 1080f / 2400f, 0.001f)
    }

    @Test
    fun `camera mode with 270 degree EXIF rotation maps crop correctly`() {
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 4000,
            rawHeight = 3000,
            rotationDegrees = 270,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(1080f / 2400f),
            maxDimension = 1920
        )

        assertEquals(0, plan.sourceCrop.x)
        assertEquals(600, plan.sourceCrop.y)
        assertEquals(4000, plan.sourceCrop.width)
        assertEquals(1800, plan.sourceCrop.height)
        assertEquals(270, plan.rotationDegrees)
        assertEquals(864, plan.outputWidth)
        assertEquals(1920, plan.outputHeight)
    }

    @Test
    fun `narrower visual image crops top and bottom when target aspect ratio is wider`() {
        // 視覺畫面：寬 1000, 高 2000 (比例 0.5)
        // 目標視口比例：1.0 (正方形)
        val plan = ImageGeometryCalculator.calculatePlan(
            rawWidth = 1000,
            rawHeight = 2000,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(1.0f),
            maxDimension = 1920
        )

        // 寬 1000 全部保留，高 2000 裁切為 1000，Y 起始點 = (2000 - 1000) / 2 = 500
        assertEquals(0, plan.sourceCrop.x)
        assertEquals(500, plan.sourceCrop.y)
        assertEquals(1000, plan.sourceCrop.width)
        assertEquals(1000, plan.sourceCrop.height)
        assertEquals(1000, plan.outputWidth)
        assertEquals(1000, plan.outputHeight)
    }

    @Test
    fun `invalid or zero dimensions return safe fallback plan without throwing`() {
        val planZero = ImageGeometryCalculator.calculatePlan(
            rawWidth = 0,
            rawHeight = 0,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(0.5f)
        )
        assertTrue(planZero.isIdentity)
        assertEquals(1, planZero.outputWidth)
        assertEquals(1, planZero.outputHeight)

        val planNegativeRatio = ImageGeometryCalculator.calculatePlan(
            rawWidth = 1000,
            rawHeight = 1000,
            rotationDegrees = 0,
            cropPolicy = ImageCropPolicy.CenterFillAspectRatio(-1f)
        )
        // 負比例自動回退為不裁切
        assertEquals(1000, planNegativeRatio.sourceCrop.width)
        assertEquals(1000, planNegativeRatio.sourceCrop.height)
    }
}
