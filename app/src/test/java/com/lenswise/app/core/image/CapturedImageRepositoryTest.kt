package com.lenswise.app.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CapturedImageRepositoryTest {

    private fun createDummyEntity(id: String = java.util.UUID.randomUUID().toString()): AppImageEntity {
        return AppImageEntity(
            id = id,
            file = File("dummy_$id.jpg"),
            width = 1920,
            height = 1080,
            source = ImageSource.CAMERA
        )
    }

    @Test
    fun testAddImages_updatesStateFlow() {
        val repo = DefaultCapturedImageRepository()
        assertEquals(0, repo.count)

        val img1 = createDummyEntity()
        val img2 = createDummyEntity()
        repo.addImages(listOf(img1, img2))

        assertEquals(2, repo.count)
        assertEquals(listOf(img1, img2), repo.images.value)
    }

    @Test
    fun testRemoveImageByIndex() {
        val img1 = createDummyEntity("1")
        val img2 = createDummyEntity("2")
        val repo = DefaultCapturedImageRepository(listOf(img1, img2))

        repo.removeImage(0)
        assertEquals(1, repo.count)
        assertEquals("2", repo.images.value[0].id)

        // Out of bound does nothing
        repo.removeImage(99)
        assertEquals(1, repo.count)
    }

    @Test
    fun testRemoveImageById() {
        val img1 = createDummyEntity("A")
        val img2 = createDummyEntity("B")
        val repo = DefaultCapturedImageRepository(listOf(img1, img2))

        repo.removeImageById("A")
        assertEquals(1, repo.count)
        assertEquals("B", repo.images.value[0].id)
    }

    @Test
    fun testClear_emptiesAll() {
        val repo = DefaultCapturedImageRepository(listOf(createDummyEntity(), createDummyEntity()))
        assertEquals(2, repo.count)

        repo.clear()
        assertEquals(0, repo.count)
        assertTrue(repo.images.value.isEmpty())
    }

    @Test
    fun testClearConsumed_removesOnlyConsumedImagesSafely() {
        val img1 = createDummyEntity("task_1")
        val img2 = createDummyEntity("task_2")
        val repo = DefaultCapturedImageRepository(listOf(img1, img2))

        // Suppose img3 was added concurrently while task 1 and 2 were being analyzed
        val img3 = createDummyEntity("concurrent_new")
        repo.addImages(listOf(img3))
        assertEquals(3, repo.count)

        // Clear only the consumed images
        repo.clearConsumed(listOf(img1, img2))

        assertEquals(1, repo.count)
        assertEquals("concurrent_new", repo.images.value[0].id)
    }
}
