package com.lenswise.app.core.media

import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.ZoneId
import java.time.ZonedDateTime

class GalleryPickerDomainTest {

    @Test
    fun `DateBucket should cache instances for the same year and month`() {
        val dt1 = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneId.systemDefault())
        val dt2 = ZonedDateTime.of(2026, 10, 20, 18, 30, 0, 0, ZoneId.systemDefault())

        val bucket1 = DateBucket.fromEpochMillis(dt1.toInstant().toEpochMilli())
        val bucket2 = DateBucket.fromEpochMillis(dt2.toInstant().toEpochMilli())

        assertEquals(2026, bucket1.year)
        assertEquals(10, bucket1.month)
        assertEquals("2026 年 10 月", bucket1.displayTitle)
        assertSame(bucket1, bucket2)
    }

    @Test
    fun `DateBucket should sort descending by year and month`() {
        val oct2026 = DateBucket(2026, 10)
        val sep2026 = DateBucket(2026, 9)
        val dec2025 = DateBucket(2025, 12)

        val list = listOf(sep2026, dec2025, oct2026).sorted()

        assertEquals(listOf(oct2026, sep2026, dec2025), list)
    }

    @Test
    fun `buildTimelineItems should group assets and assign correct spans and keys`() = runBlocking {
        val dummyContext = mock(android.content.Context::class.java)
        val repository = MediaRepository(dummyContext)

        val mockUri = mock(Uri::class.java)

        val octBucket = DateBucket(2026, 10)
        val sepBucket = DateBucket(2026, 9)

        val asset1 = MediaAsset(id = 1L, contentUri = mockUri, dateTakenMs = 1000L, bucket = octBucket)
        val asset2 = MediaAsset(id = 2L, contentUri = mockUri, dateTakenMs = 900L, bucket = octBucket)
        val asset3 = MediaAsset(id = 3L, contentUri = mockUri, dateTakenMs = 800L, bucket = sepBucket)

        val items = repository.buildTimelineItems(
            assets = listOf(asset1, asset2, asset3),
            selectedIds = setOf(2L)
        )

        assertEquals(5, items.size)

        // 1. October Header
        val header1 = items[0] as GalleryUiItem.Header
        assertEquals(octBucket, header1.bucket)
        assertEquals(2, header1.totalCount)
        assertEquals(3, header1.spanCount)
        assertEquals("header_2026_10", header1.stableKey)

        // 2. Photo 1 (not selected)
        val photo1 = items[1] as GalleryUiItem.Photo
        assertEquals(1L, photo1.asset.id)
        assertFalse(photo1.isSelected)
        assertEquals(1, photo1.spanCount)
        assertEquals("photo_1", photo1.stableKey)

        // 3. Photo 2 (selected)
        val photo2 = items[2] as GalleryUiItem.Photo
        assertEquals(2L, photo2.asset.id)
        assertTrue(photo2.isSelected)
        assertEquals(1, photo2.spanCount)
        assertEquals("photo_2", photo2.stableKey)

        // 4. September Header
        val header2 = items[3] as GalleryUiItem.Header
        assertEquals(sepBucket, header2.bucket)
        assertEquals(1, header2.totalCount)
        assertEquals(3, header2.spanCount)
        assertEquals("header_2026_9", header2.stableKey)

        // 5. Photo 3
        val photo3 = items[4] as GalleryUiItem.Photo
        assertEquals(3L, photo3.asset.id)
        assertFalse(photo3.isSelected)
    }
}
