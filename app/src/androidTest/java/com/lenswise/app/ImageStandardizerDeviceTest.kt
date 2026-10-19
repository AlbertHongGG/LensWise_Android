package com.lenswise.app

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.image.ImageStandardizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageStandardizerDeviceTest {

    private val context: android.content.Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun grantPermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pkg = context.packageName
        if (Build.VERSION.SDK_INT >= 34) {
            automation.grantRuntimePermission(pkg, Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(pkg, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            automation.grantRuntimePermission(pkg, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        automation.grantRuntimePermission(pkg, Manifest.permission.CAMERA)
    }

    @Test
    fun testStandardizeRealMediaStoreUriSuccess() = runBlocking {
        val uri = Uri.parse("content://media/external/images/media/1000010041")

        println("=== REAL TEST EXECUTION START ===")
        val result = ImageStandardizer.standardizeFromUri(context, uri, ImageSource.GALLERY)

        assertTrue("Standardization should succeed! Error: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val entity = result.getOrThrow()
        println("SUCCESS: entity width=${entity.width}, height=${entity.height}, file=${entity.file.absolutePath}, fileSize=${entity.file.length()}")
        assertTrue("Output file must exist", entity.file.exists())
        assertTrue("Output file must be non-empty", entity.file.length() > 0)
        assertTrue("Thumbnail must not be recycled", entity.thumbnailBitmap?.isRecycled == false)
        println("=== REAL TEST EXECUTION END ===")
    }
}
