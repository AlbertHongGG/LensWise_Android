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
        assertEquals(0, timeline.sectionCount)
        assertEquals(0, timeline.getSectionIndex(0.5f))
        assertEquals(0, timeline.getHeaderItemIndexForSection(0))
        assertEquals(0f, timeline.getFractionForVisibleItem(0))
        assertNull(timeline.getBucketForSection(0))
        assertNull(timeline.getBucketForFraction(0.5f))
        assertNull(timeline.getBucketForVisibleItem(0))
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
        assertEquals(1, timeline.sectionCount)

        // Single section tests
        assertEquals(0, timeline.getSectionIndex(0.0f))
        assertEquals(0, timeline.getSectionIndex(1.0f))
        assertEquals(0, timeline.getHeaderItemIndexForSection(0))
        assertEquals(b202611, timeline.getBucketForSection(0))
        assertEquals(b202611, timeline.getBucketForFraction(0.5f))
        assertEquals(0f, timeline.getFractionForVisibleItem(1))
    }

    @Test
    fun `multi-year multi-month timeline snaps accurately by month section`() {
        val b202611 = DateBucket(2026, 11)
        val b202610 = DateBucket(2026, 10)
        val b202405 = DateBucket(2024, 5)
        val b202001 = DateBucket(2020, 1)

        val items = mutableListOf<GalleryUiItem>()

        // Section 0: 2026/11 at index 0 (3 photos: 0..3)
        items.add(GalleryUiItem.Header(b202611, 3))
        items.add(createPhoto(101L, b202611))
        items.add(createPhoto(102L, b202611))
        items.add(createPhoto(103L, b202611))

        // Section 1: 2026/10 at index 4 (2 photos: 4..6)
        items.add(GalleryUiItem.Header(b202610, 2))
        items.add(createPhoto(201L, b202610))
        items.add(createPhoto(202L, b202610))

        // Section 2: 2024/05 at index 7 (4 photos: 7..11)
        items.add(GalleryUiItem.Header(b202405, 4))
        items.add(createPhoto(301L, b202405))
        items.add(createPhoto(302L, b202405))
        items.add(createPhoto(303L, b202405))
        items.add(createPhoto(304L, b202405))

        // Section 3: 2020/01 at index 12 (1 photo: 12..13)
        items.add(GalleryUiItem.Header(b202001, 1))
        items.add(createPhoto(401L, b202001))

        val timeline = FastScrollTimeline(items)
        assertEquals(14, timeline.totalItems)
        assertEquals(4, timeline.sectionCount)
        assertTrue(timeline.isValid)

        // Section 0 -> 2026/11 (Header at 0)
        assertEquals(0, timeline.getSectionIndex(0.0f))
        assertEquals(0, timeline.getHeaderItemIndexForSection(0))
        assertEquals(b202611, timeline.getBucketForSection(0))
        assertEquals(b202611, timeline.getBucketForFraction(0.0f))

        // Section 1 -> 2026/10 (Header at 4)
        // Fraction 1/3 ~ 0.33 maps to section 1
        assertEquals(1, timeline.getSectionIndex(0.33f))
        assertEquals(4, timeline.getHeaderItemIndexForSection(1))
        assertEquals(b202610, timeline.getBucketForSection(1))

        // Section 2 -> 2024/05 (Header at 7)
        // Fraction 2/3 ~ 0.67 maps to section 2
        assertEquals(2, timeline.getSectionIndex(0.67f))
        assertEquals(7, timeline.getHeaderItemIndexForSection(2))
        assertEquals(b202405, timeline.getBucketForSection(2))

        // Section 3 -> 2020/01 (Header at 12)
        // Fraction 1.0 maps to section 3
        assertEquals(3, timeline.getSectionIndex(1.0f))
        assertEquals(12, timeline.getHeaderItemIndexForSection(3))
        assertEquals(b202001, timeline.getBucketForSection(3))
        assertEquals(b202001, timeline.getBucketForFraction(1.0f))

        // Check reverse mapping for visible item
        assertEquals(0, timeline.getSectionIndexForVisibleItem(2)) // item 2 is in 2026/11
        assertEquals(b202611, timeline.getBucketForVisibleItem(2))

        assertEquals(1, timeline.getSectionIndexForVisibleItem(5)) // item 5 is in 2026/10
        assertEquals(b202610, timeline.getBucketForVisibleItem(5))

        assertEquals(2, timeline.getSectionIndexForVisibleItem(9)) // item 9 is in 2024/05
        assertEquals(b202405, timeline.getBucketForVisibleItem(9))

        assertEquals(3, timeline.getSectionIndexForVisibleItem(13)) // item 13 is in 2020/01
        assertEquals(b202001, timeline.getBucketForVisibleItem(13))
    }

    @Test
    fun `abnormal fractions are clamped defensively`() {
        val b202611 = DateBucket(2026, 11)
        val b202610 = DateBucket(2026, 10)
        val items = listOf(
            GalleryUiItem.Header(bucket = b202611, totalCount = 1),
            createPhoto(1L, b202611),
            GalleryUiItem.Header(bucket = b202610, totalCount = 1),
            createPhoto(2L, b202610)
        )
        val timeline = FastScrollTimeline(items)

        // Negative fraction clamps to section 0
        assertEquals(0, timeline.getSectionIndex(-0.8f))
        assertEquals(b202611, timeline.getBucketForFraction(-0.8f))

        // Fraction > 1.0 clamps to last section
        assertEquals(1, timeline.getSectionIndex(1.5f))
        assertEquals(b202610, timeline.getBucketForFraction(1.5f))

        // NaN clamps to section 0
        assertEquals(0, timeline.getSectionIndex(Float.NaN))
        assertEquals(b202611, timeline.getBucketForFraction(Float.NaN))
    }
}
