package com.lenswise.app.core.media

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayOutputStream
import java.io.File

class L1MicroPackStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var packFile: File
    private lateinit var store: L1MicroPackStore

    @Before
    fun setUp() {
        packFile = File(tempFolder.root, "test_pack.bin")
        store = L1MicroPackStore(packFile)
    }

    @Test
    fun `readAllToMemory on non-existent file returns empty map`() {
        val result = store.readAllToMemory()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `appendRecord writes record and raw bytes correctly`() {
        // 模擬一張 30x30 點陣圖的壓縮輸出
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.compress(any(), anyInt(), any())).thenAnswer { invocation ->
            val out = invocation.arguments[2] as java.io.OutputStream
            out.write(byteArrayOf(1, 2, 3, 4, 5))
            true
        }

        val success = store.appendRecord(assetId = 1001L, dateTakenMs = 1700000000L, bitmap = mockBitmap)
        assertTrue(success)
        assertTrue(packFile.exists())
        assertTrue("Pack file must contain at least 8 bytes header + record", packFile.length() > 8)
    }

    @Test
    fun `appendRecords batch writes multiple records`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.compress(any(), anyInt(), any())).thenAnswer { invocation ->
            val out = invocation.arguments[2] as java.io.OutputStream
            out.write(byteArrayOf(10, 20, 30))
            true
        }

        val records = listOf(
            Triple(1L, 100L, mockBitmap),
            Triple(2L, 200L, mockBitmap),
            Triple(3L, 300L, mockBitmap)
        )

        val written = store.appendRecords(records)
        assertEquals(3, written)
        assertTrue(packFile.length() > 8)
    }

    @Test
    fun `compact preserves only active asset records`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.compress(any(), anyInt(), any())).thenAnswer { invocation ->
            val out = invocation.arguments[2] as java.io.OutputStream
            out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            true
        }

        store.appendRecord(1L, 100L, mockBitmap)
        store.appendRecord(2L, 200L, mockBitmap)
        store.appendRecord(3L, 300L, mockBitmap)

        val initialLength = packFile.length()

        // 僅保留 ID = 2L
        store.compact(setOf(2L))

        assertTrue(packFile.exists())
        assertTrue("Compacted file should be smaller than initial", packFile.length() < initialLength)
    }
}
