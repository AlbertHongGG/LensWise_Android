package com.lenswise.app.features.scanner.gallery.fastscroll

import android.net.Uri
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class FastScrollTimelineTest {

    private fun createPhoto(id: Long, bucket: DateBucket): GalleryUiItem.Photo {
        val asset = MediaAsset(
            id = id,
            contentUri = mock(Uri::class.java),
            dateTakenMs = 1000L * id,
            bucket = bucket
        )
        return GalleryUiItem.Photo(asset = asset)
    }

    @Test
    fun `empty items returns invalid timeline and safe defaults`() {
        val timeline = FastScrollTimeline(emptyList())

        assertFalse(timeline.isValid)
        assertEquals(0, timeline.totalItems)
        assertEquals(0, timeline.getTargetIndex(0.5f))
        assertEquals(0f, timeline.getFractionForIndex(0))
        assertNull(timeline.getBucketForIndex(0))
        assertNull(timeline.getBucketForFraction(0.5f))
    }

    @Test
    fun `single header and photo timeline maps correctly`() {
        val b202611 = DateBucket(2026, 11)
        val items = listOf(
            GalleryUiItem.Header(bucket = b202611, totalCount = 1),
            createPhoto(1L, b202611)
        )
        val timeline = FastScrollTimeline(items)

        assertTrue(timeline.isValid)
        assertEquals(2, timeline.totalItems)

        // 0.0 maps to index 0 (Header)
        assertEquals(0, timeline.getTargetIndex(0.0f))
        assertEquals(b202611, timeline.getBucketForIndex(0))
        assertEquals(b202611, timeline.getBucketForFraction(0.0f))

        // 1.0 maps to index 1 (Photo)
        assertEquals(1, timeline.getTargetIndex(1.0f))
        assertEquals(b202611, timeline.getBucketForIndex(1))
        assertEquals(b202611, timeline.getBucketForFraction(1.0f))
    }

    @Test
    fun `multi-year multi-month timeline maps accurately with binary search`() {
        val b202611 = DateBucket(2026, 11)
        val b202610 = DateBucket(2026, 10)
        val b202405 = DateBucket(2024, 5)
        val b202001 = DateBucket(2020, 1)

        val items = mutableListOf<GalleryUiItem>()

        // 2026/11: 3 photos (indices 0..3)
        items.add(GalleryUiItem.Header(b202611, 3))
        items.add(createPhoto(101L, b202611))
        items.add(createPhoto(102L, b202611))
        items.add(createPhoto(103L, b202611))

        // 2026/10: 2 photos (indices 4..6)
        items.add(GalleryUiItem.Header(b202610, 2))
        items.add(createPhoto(201L, b202610))
        items.add(createPhoto(202L, b202610))

        // 2024/05: 4 photos (indices 7..11)
        items.add(GalleryUiItem.Header(b202405, 4))
        items.add(createPhoto(301L, b202405))
        items.add(createPhoto(302L, b202405))
        items.add(createPhoto(303L, b202405))
        items.add(createPhoto(304L, b202405))

        // 2020/01: 1 photo (indices 12..13)
        items.add(GalleryUiItem.Header(b202001, 1))
        items.add(createPhoto(401L, b202001))

        val timeline = FastScrollTimeline(items)
        assertEquals(14, timeline.totalItems)
        assertTrue(timeline.isValid)

        // Test boundary 0.0 -> First Item (2026/11)
        assertEquals(0, timeline.getTargetIndex(0.0f))
        assertEquals(b202611, timeline.getBucketForIndex(0))
        assertEquals(b202611, timeline.getBucketForFraction(0.0f))

        // Test inside 2026/10: index 5
        assertEquals(b202610, timeline.getBucketForIndex(5))

        // Test inside 2024/05: index 9
        assertEquals(b202405, timeline.getBucketForIndex(9))

        // Test boundary 1.0 -> Last Item (2020/01)
        assertEquals(13, timeline.getTargetIndex(1.0f))
        assertEquals(b202001, timeline.getBucketForIndex(13))
        assertEquals(b202001, timeline.getBucketForFraction(1.0f))
    }

    @Test
    fun `abnormal fractions are clamped defensively`() {
        val b202611 = DateBucket(2026, 11)
        val items = listOf(
            GalleryUiItem.Header(bucket = b202611, totalCount = 2),
            createPhoto(1L, b202611),
            createPhoto(2L, b202611)
        )
        val timeline = FastScrollTimeline(items)

        // Negative fraction clamps to 0
        assertEquals(0, timeline.getTargetIndex(-0.8f))
        assertEquals(b202611, timeline.getBucketForFraction(-0.8f))

        // Fraction > 1.0 clamps to totalItems - 1
        assertEquals(2, timeline.getTargetIndex(1.5f))
        assertEquals(b202611, timeline.getBucketForFraction(1.5f))

        // NaN clamps to 0
        assertEquals(0, timeline.getTargetIndex(Float.NaN))
        assertEquals(b202611, timeline.getBucketForFraction(Float.NaN))
    }
}
