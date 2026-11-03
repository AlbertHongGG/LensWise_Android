package com.lenswise.app.core.media

import android.content.Context
import android.net.Uri
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class ThumbnailCacheRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        `when`(mockContext.cacheDir).thenReturn(tempFolder.root)
    }

    @Test
    fun `getThumbnailFile should return null if file does not exist or is empty`() {
        val repo = DiskThumbnailCacheRepository(
            context = mockContext,
            cacheDirName = "test_thumbs",
            boundedDispatcher = Dispatchers.Default
        )
        val mockUri = mock(Uri::class.java)
        val asset = MediaAsset(id = 1L, contentUri = mockUri, dateTakenMs = 12345L, bucket = DateBucket(2026, 11))

        assertNull(repo.getThumbnailFile(asset))

        // 建立 0 byte 空檔案
        val cacheDir = File(tempFolder.root, "test_thumbs")
        cacheDir.mkdirs()
        val emptyFile = File(cacheDir, "1_12345.jpg")
        emptyFile.createNewFile()

        assertNull(repo.getThumbnailFile(asset))

        // 寫入資料後應能取得
        emptyFile.writeBytes(byteArrayOf(1, 2, 3))
        val result = repo.getThumbnailFile(asset)
        assertNotNull(result)
        assertEquals(emptyFile.absolutePath, result?.absolutePath)
    }

    @Test
    fun `generateAndCacheThumbnail should return existing file if already present`() = runBlocking {
        val repo = DiskThumbnailCacheRepository(
            context = mockContext,
            cacheDirName = "test_thumbs",
            boundedDispatcher = Dispatchers.Default
        )
        val mockUri = mock(Uri::class.java)
        val asset = MediaAsset(id = 88L, contentUri = mockUri, dateTakenMs = 77777L, bucket = DateBucket(2026, 11))

        val cacheDir = File(tempFolder.root, "test_thumbs")
        cacheDir.mkdirs()
        val file = File(cacheDir, "88_77777.jpg").apply { writeBytes(byteArrayOf(9, 9, 9)) }

        val result = repo.generateAndCacheThumbnail(asset)
        assertNotNull(result)
        assertEquals(file.absolutePath, result?.absolutePath)
    }

    @Test
    fun `reconcileActiveAssets should immediately delete orphaned and tmp cache files`() = runBlocking {
        val repo = DiskThumbnailCacheRepository(
            context = mockContext,
            cacheDirName = "test_thumbs",
            boundedDispatcher = Dispatchers.Default
        )

        val cacheDir = File(tempFolder.root, "test_thumbs")
        cacheDir.mkdirs()

        // 準備檔案：
        // 1. active_file: id = 100 (仍在相簿內)
        val activeFile = File(cacheDir, "100_5000.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        // 2. orphan_file: id = 200 (已被使用者從手機刪除)
        val orphanFile = File(cacheDir, "200_6000.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        // 3. tmp_file: 殘留暫存檔
        val tmpFile = File(cacheDir, "300_7000.jpg.tmp").apply { writeBytes(byteArrayOf(7, 8, 9)) }

        assertTrue(activeFile.exists())
        assertTrue(orphanFile.exists())
        assertTrue(tmpFile.exists())

        // 執行活性對照：目前相簿僅存 id = 100
        repo.reconcileActiveAssets(activeAssetIds = setOf(100L))

        // 驗證：有效檔案保留，孤兒與暫存檔徹底銷毀
        assertTrue(activeFile.exists())
        assertFalse(orphanFile.exists())
        assertFalse(tmpFile.exists())
    }

    @Test
    fun `reconcileActiveAssets with empty activeAssetIds should safely abort and never delete cache files`() = runBlocking {
        val repo = DiskThumbnailCacheRepository(
            context = mockContext,
            cacheDirName = "test_thumbs",
            boundedDispatcher = Dispatchers.Default
        )

        val cacheDir = File(tempFolder.root, "test_thumbs")
        cacheDir.mkdirs()

        val activeFile = File(cacheDir, "100_5000.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val orphanFile = File(cacheDir, "200_6000.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        // 當傳入空集合 (如無權限或查詢異常狀態時)
        repo.reconcileActiveAssets(activeAssetIds = emptySet())

        // 驗證防禦性閥門：所有檔案原封不動保留，絕不誤清空
        assertTrue("Files should be preserved when activeAssetIds is empty", activeFile.exists())
        assertTrue("Files should be preserved when activeAssetIds is empty", orphanFile.exists())
    }

    @Test
    fun `reconcileActiveAssets should prune older files when total size exceeds maxCacheSizeBytes`() = runBlocking {
        val repo = DiskThumbnailCacheRepository(
            context = mockContext,
            cacheDirName = "test_thumbs",
            maxCacheSizeBytes = 500L,
            boundedDispatcher = Dispatchers.Default
        )

        val cacheDir = File(tempFolder.root, "test_thumbs")
        cacheDir.mkdirs()

        // 建立 3 個檔案，每個 300 bytes (總共 900 bytes > 500 bytes)
        val file1 = File(cacheDir, "101_10.jpg").apply {
            writeBytes(ByteArray(300))
            setLastModified(1000L) // 最舊
        }
        val file2 = File(cacheDir, "102_20.jpg").apply {
            writeBytes(ByteArray(300))
            setLastModified(2000L) // 次舊
        }
        val file3 = File(cacheDir, "103_30.jpg").apply {
            writeBytes(ByteArray(300))
            setLastModified(3000L) // 最新
        }

        // 3 個檔案都在相簿內，但超過 500 bytes 限制
        repo.reconcileActiveAssets(activeAssetIds = setOf(101L, 102L, 103L))

        // 驗證 LRU 淘汰：file1 (最舊) 應被刪除，總容量降至 600 -> 仍大於 500，file2 也被刪除，剩下 file3 (300 <= 500)
        assertFalse("Oldest file should be evicted", file1.exists())
        assertFalse("Second oldest file should be evicted", file2.exists())
        assertTrue("Newest file should be kept", file3.exists())
    }

    @Test
    fun `MediaAssetKeyer should generate unique deterministic cache key`() {
        val keyer = MediaAssetKeyer()
        val mockUri = mock(Uri::class.java)
        val asset = MediaAsset(id = 999L, contentUri = mockUri, dateTakenMs = 888888L, bucket = DateBucket(2026, 11))
        val options = Options(context = mockContext)

        val key = keyer.key(asset, options)
        assertEquals("asset_999_888888", key)
    }
}
