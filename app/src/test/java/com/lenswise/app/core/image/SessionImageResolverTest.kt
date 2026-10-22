package com.lenswise.app.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionImageResolverTest {

    @Test
    fun `empty resolver returns safe default values`() {
        val resolver = SessionImageResolver()

        assertFalse(resolver.hasImages)
        assertFalse(resolver.isMemorySource)
        assertEquals(0, resolver.count)
        assertNull(resolver.resolveFile(0))
        assertNull(resolver.resolveFile(-1))
        assertNull(resolver.resolveFile(10))
        assertTrue(resolver.getAllFiles().isEmpty())
        assertNull(resolver.getDiskPath(0))
    }

    @Test
    fun `memory source prioritizes app image entities`() {
        val tempFile1 = File.createTempFile("test1", ".jpg")
        val tempFile2 = File.createTempFile("test2", ".jpg")
        tempFile1.deleteOnExit()
        tempFile2.deleteOnExit()

        val entities = listOf(
            AppImageEntity(file = tempFile1, width = 100, height = 100, source = ImageSource.CAMERA),
            AppImageEntity(file = tempFile2, width = 100, height = 100, source = ImageSource.CAMERA)
        )

        val resolver = SessionImageResolver(
            memoryImages = entities,
            imagePaths = listOf("/disk/old.jpg")
        )

        assertTrue(resolver.isMemorySource)
        assertTrue(resolver.hasImages)
        assertEquals(2, resolver.count)

        assertEquals(tempFile1, resolver.resolveFile(0))
        assertEquals(tempFile2, resolver.resolveFile(1))
        // Clamping check: out of bound index locks to boundary
        assertEquals(tempFile1, resolver.resolveFile(-5))
        assertEquals(tempFile2, resolver.resolveFile(10))

        val all = resolver.getAllFiles()
        assertEquals(2, all.size)
        assertEquals(tempFile1, all[0])
        assertEquals(tempFile2, all[1])
    }

    @Test
    fun `disk source resolves image paths`() {
        val path1 = "C:/test/image1.jpg"
        val path2 = "C:/test/image2.jpg"

        val resolver = SessionImageResolver(
            memoryImages = null,
            imagePaths = listOf(path1, path2)
        )

        assertFalse(resolver.isMemorySource)
        assertTrue(resolver.hasImages)
        assertEquals(2, resolver.count)

        assertEquals(File(path1), resolver.resolveFile(0))
        assertEquals(File(path2), resolver.resolveFile(1))
        // Clamping check
        assertEquals(File(path1), resolver.resolveFile(-1))
        assertEquals(File(path2), resolver.resolveFile(5))

        assertEquals(path1, resolver.getDiskPath(0))
        assertEquals(path2, resolver.getDiskPath(1))
    }
}
