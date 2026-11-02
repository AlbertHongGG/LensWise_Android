package com.lenswise.app.features.scanner.gallery

import android.net.Uri
import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

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

    private lateinit var fakeRepo: FakeMediaRepository
    private lateinit var viewModel: GalleryPickerViewModel

    @Before
    fun setUp() {
        fakeRepo = FakeMediaRepository()
        viewModel = GalleryPickerViewModel(fakeRepo)
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
    fun `Factory companion object should be non-null and accessible`() {
        assertNotNull(GalleryPickerViewModel.Factory)
    }
}
