package com.lenswise.app.core.media

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class GalleryPreloadManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockImageLoader: ImageLoader

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockImageLoader = mock(ImageLoader::class.java)
    }

    @Test
    fun `preloadTopThumbnails should enqueue requests up to requested count`() {
        val manager = CoilGalleryPreloadManager(
            context = mockContext,
            imageLoader = mockImageLoader,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val assets = (1..10).map { id ->
            MediaAsset(id = id.toLong(), contentUri = mockUri, dateTakenMs = 1000L * id, bucket = bucket)
        }

        manager.preloadTopThumbnails(assets, count = 3)

        val captor = ArgumentCaptor.forClass(ImageRequest::class.java)
        val dummy = ImageRequest.Builder(mockContext).build()
        verify(mockImageLoader, times(3)).enqueue(captor.capture() ?: dummy)

        val captured = captor.allValues
        assertEquals(3, captured.size)
        assertEquals(assets[0], captured[0].data)
        assertEquals(assets[1], captured[1].data)
        assertEquals(assets[2], captured[2].data)
    }

    @Test
    fun `preloadTopThumbnails with empty assets should do nothing`() {
        val manager = CoilGalleryPreloadManager(
            context = mockContext,
            imageLoader = mockImageLoader,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        manager.preloadTopThumbnails(emptyList(), count = 5)

        val dummy = ImageRequest.Builder(mockContext).build()
        val captor = ArgumentCaptor.forClass(ImageRequest::class.java)
        verify(mockImageLoader, times(0)).enqueue(captor.capture() ?: dummy)
    }
}
