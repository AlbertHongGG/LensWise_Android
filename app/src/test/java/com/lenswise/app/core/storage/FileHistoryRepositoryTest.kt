package com.lenswise.app.core.storage

import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileHistoryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var repository: ReceiptHistoryRepository

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("files_dir")
        repository = ReceiptHistoryRepository(baseDir)
    }

    private fun createDummyImageEntity(id: String, content: String = "dummy_content"): AppImageEntity {
        val dummyFile = tempFolder.newFile("src_$id.jpg").apply {
            writeText(content)
        }
        return AppImageEntity(
            id = id,
            file = dummyFile,
            width = 1080,
            height = 1920,
            source = ImageSource.CAMERA
        )
    }

    @Test
    fun testSaveImages_savesRelativePaths_andWritesPhysicalFiles() = runBlocking {
        val img1 = createDummyImageEntity("img_1", "bytes_photo_1")
        val img2 = createDummyImageEntity("img_2", "bytes_photo_2")

        val relativePaths = repository.saveImages("receipt_101", listOf(img1, img2))

        assertEquals(2, relativePaths.size)
        assertEquals("history/receipt_images/receipt_101_0.jpg", relativePaths[0])
        assertEquals("history/receipt_images/receipt_101_1.jpg", relativePaths[1])

        val physicalFile1 = File(baseDir, relativePaths[0])
        val physicalFile2 = File(baseDir, relativePaths[1])

        assertTrue(physicalFile1.exists())
        assertTrue(physicalFile2.exists())
        assertEquals("bytes_photo_1", physicalFile1.readText())
        assertEquals("bytes_photo_2", physicalFile2.readText())
    }

    @Test
    fun testDeleteRecord_removesBothJsonAndPhysicalPhotos_preventingOrphanFiles() = runBlocking {
        val img = createDummyImageEntity("photo_orphan_test", "content_orphan")
        val relativePaths = repository.saveImages("rec_to_delete", listOf(img))

        val record = ReceiptScannerRecord(
            id = "rec_to_delete",
            storeName = "全聯福利中心",
            totalAmount = 250.0,
            imagePaths = relativePaths
        )
        repository.save(record)

        val jsonFile = File(repository.recordsDir, "rec_to_delete.json")
        val photoFile = File(baseDir, relativePaths[0])

        assertTrue(jsonFile.exists())
        assertTrue(photoFile.exists())
        assertEquals(1, repository.getAll().size)

        // 執行刪除
        repository.delete("rec_to_delete")

        // 驗證 JSON 與實體照片皆被連帶刪除，杜絕孤兒檔案！
        assertFalse(jsonFile.exists())
        assertFalse(photoFile.exists())
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun testClearAll_removesAllJsonAndImages() = runBlocking {
        val img1 = createDummyImageEntity("p1")
        val img2 = createDummyImageEntity("p2")

        val paths1 = repository.saveImages("rec_1", listOf(img1))
        val paths2 = repository.saveImages("rec_2", listOf(img2))

        repository.save(ReceiptScannerRecord(id = "rec_1", storeName = "S1", imagePaths = paths1))
        repository.save(ReceiptScannerRecord(id = "rec_2", storeName = "S2", imagePaths = paths2))

        assertEquals(2, repository.getAll().size)
        assertEquals(2, repository.imagesDir.listFiles()?.size)

        repository.clearAll()

        assertEquals(0, repository.getAll().size)
        assertEquals(0, repository.recordsDir.listFiles()?.size ?: 0)
        assertEquals(0, repository.imagesDir.listFiles()?.size ?: 0)
    }
}
