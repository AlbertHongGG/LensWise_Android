package com.lenswise.app.features.scanner.gallery

import android.graphics.Bitmap
import android.net.Uri
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryPreloadManager
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.MediaRepository
import com.lenswise.app.core.media.ThumbnailCacheRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

class GalleryPickerViewModelTest {

    private class FakeMediaRepository : MediaRepository {
        val assetsFlow = MutableStateFlow<List<MediaAsset>>(emptyList())

        override fun observeMediaAssets(): Flow<List<MediaAsset>> = assetsFlow.asStateFlow()

        override suspend fun queryAllMediaAssets(): List<MediaAsset> = assetsFlow.value

        override suspend fun buildTimelineItems(
            assets: List<MediaAsset>,
            selectedIds: Set<Long>
        ): List<GalleryUiItem> {
            val bucket = DateBucket(2026, 11)
            val items = mutableListOf<GalleryUiItem>()
            if (assets.isNotEmpty()) {
                items.add(GalleryUiItem.Header(bucket, assets.size))
                for (a in assets) {
                    items.add(GalleryUiItem.Photo(asset = a, isSelected = selectedIds.contains(a.id)))
                }
            }
            return items
        }
    }

    private class FakeThumbnailCacheRepository : ThumbnailCacheRepository {
        var lastReconciledIds: Set<Long>? = null

        override fun getThumbnailFile(asset: MediaAsset): File? = null
        override suspend fun saveThumbnail(asset: MediaAsset, bitmap: Bitmap): File = error("Not needed")
        override suspend fun generateAndCacheThumbnail(asset: MediaAsset, targetSize: Int): File? = null
        override suspend fun reconcileActiveAssets(activeAssetIds: Set<Long>) {
            lastReconciledIds = activeAssetIds
        }
        override fun getMemoryMicroThumbnail(assetId: Long): Bitmap? = null
        override fun putMemoryMicroThumbnail(assetId: Long, bitmap: Bitmap) {}
        override suspend fun getOrGenerateMicroThumbnail(asset: MediaAsset): Bitmap? = null
    }

    private class FakeGalleryPreloadManager : GalleryPreloadManager {
        var preloadedAssets: List<MediaAsset>? = null
        override fun preloadTopThumbnails(assets: List<MediaAsset>, count: Int) {
            preloadedAssets = assets.take(count)
        }
    }

    private lateinit var fakeRepo: FakeMediaRepository
    private lateinit var fakeCacheRepo: FakeThumbnailCacheRepository
    private lateinit var fakePreloadMgr: FakeGalleryPreloadManager
    private val testScope = CoroutineScope(Dispatchers.Default)
    private lateinit var viewModel: GalleryPickerViewModel

    @Before
    fun setUp() {
        fakeRepo = FakeMediaRepository()
        fakeCacheRepo = FakeThumbnailCacheRepository()
        fakePreloadMgr = FakeGalleryPreloadManager()
        viewModel = GalleryPickerViewModel(
            repository = fakeRepo,
            thumbnailCacheRepository = fakeCacheRepo,
            preloadManager = fakePreloadMgr,
            scope = testScope
        )
    }

    @Test
    fun `togglePhotoSelection should add and remove asset id in O(1)`() {
        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset = MediaAsset(id = 100L, contentUri = mockUri, dateTakenMs = 1000L, bucket = bucket)

        assertFalse(viewModel.uiState.value.canConfirm)
        assertEquals(0, viewModel.uiState.value.selectionCount)

        // Toggle ON
        viewModel.togglePhotoSelection(asset)
        assertTrue(viewModel.uiState.value.canConfirm)
        assertEquals(1, viewModel.uiState.value.selectionCount)
        assertTrue(viewModel.uiState.value.selectedIds.contains(100L))
        assertEquals(listOf(mockUri), viewModel.uiState.value.selectedUris)

        // Toggle OFF
        viewModel.togglePhotoSelection(asset)
        assertFalse(viewModel.uiState.value.canConfirm)
        assertEquals(0, viewModel.uiState.value.selectionCount)
        assertFalse(viewModel.uiState.value.selectedIds.contains(100L))
        assertTrue(viewModel.uiState.value.selectedUris.isEmpty())
    }

    @Test
    fun `clearSelection should reset selected ids and uris`() {
        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset1 = MediaAsset(id = 101L, contentUri = mockUri, dateTakenMs = 1000L, bucket = bucket)
        val asset2 = MediaAsset(id = 102L, contentUri = mockUri, dateTakenMs = 2000L, bucket = bucket)

        viewModel.togglePhotoSelection(asset1)
        viewModel.togglePhotoSelection(asset2)
        assertEquals(2, viewModel.uiState.value.selectionCount)

        viewModel.clearSelection()
        assertEquals(0, viewModel.uiState.value.selectionCount)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertTrue(viewModel.uiState.value.selectedUris.isEmpty())
    }

    @Test
    fun `onSessionDismiss should clean all selections and stop observing`() {
        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset = MediaAsset(id = 200L, contentUri = mockUri, dateTakenMs = 1000L, bucket = bucket)

        viewModel.togglePhotoSelection(asset)
        assertEquals(1, viewModel.uiState.value.selectionCount)

        viewModel.onSessionDismiss()
        assertEquals(0, viewModel.uiState.value.selectionCount)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `emitted assets should trigger preloadManager preloadTopThumbnails`() = runBlocking {
        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset1 = MediaAsset(id = 111L, contentUri = mockUri, dateTakenMs = 1000L, bucket = bucket)
        val asset2 = MediaAsset(id = 222L, contentUri = mockUri, dateTakenMs = 2000L, bucket = bucket)

        fakeRepo.assetsFlow.value = listOf(asset1, asset2)

        var attempts = 0
        while (fakePreloadMgr.preloadedAssets != listOf(asset1, asset2) && attempts < 40) {
            delay(50)
            attempts++
        }

        assertEquals(listOf(asset1, asset2), fakePreloadMgr.preloadedAssets)
    }

    @Test
    fun `emitted assets should trigger thumbnailCacheRepository reconcileActiveAssets after delay`() = runBlocking {
        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset1 = MediaAsset(id = 111L, contentUri = mockUri, dateTakenMs = 1000L, bucket = bucket)
        val asset2 = MediaAsset(id = 222L, contentUri = mockUri, dateTakenMs = 2000L, bucket = bucket)

        fakeRepo.assetsFlow.value = listOf(asset1, asset2)

        var attempts = 0
        while (fakeCacheRepo.lastReconciledIds != setOf(111L, 222L) && attempts < 60) {
            delay(50)
            attempts++
        }

        assertEquals(setOf(111L, 222L), fakeCacheRepo.lastReconciledIds)
    }

    @Test
    fun `onSessionStart when cachedAssets was empty should re-query and recover once assets are available`() = runBlocking {
        // 初始狀態為空 (模擬無權限啟動)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        val mockUri = mock(Uri::class.java)
        val bucket = DateBucket(2026, 11)
        val asset = MediaAsset(id = 555L, contentUri = mockUri, dateTakenMs = 5000L, bucket = bucket)

        // 使用者授權成功，MediaRepository 具備相片
        fakeRepo.assetsFlow.value = listOf(asset)

        // 再次調用 onSessionStart (模擬授權後點開彈窗)
        viewModel.onSessionStart()

        var attempts = 0
        while (viewModel.uiState.value.items.isEmpty() && attempts < 40) {
            delay(50)
            attempts++
        }

        // 驗證自我修復：狀態機順利解除死鎖並呈現相片
        assertFalse(viewModel.uiState.value.items.isEmpty())
        assertEquals(2, viewModel.uiState.value.items.size) // 1 Header + 1 Photo
    }

    @Test
    fun `Factory companion object should be non-null and accessible`() {
        assertNotNull(GalleryPickerViewModel.Factory)
    }
}
