package com.lenswise.app.core.image

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [CapturedImageRepository] 預設線程安全記憶體實作。
 */
class DefaultCapturedImageRepository(
    initialImages: List<AppImageEntity> = emptyList()
) : CapturedImageRepository {

    private val _images = MutableStateFlow(initialImages)
    override val images: StateFlow<List<AppImageEntity>> = _images.asStateFlow()

    override fun addImages(newImages: List<AppImageEntity>) {
        if (newImages.isEmpty()) return
        _images.update { current -> current + newImages }
    }

    override fun removeImage(index: Int) {
        _images.update { current ->
            if (index in current.indices) {
                current.toMutableList().apply { removeAt(index) }
            } else {
                current
            }
        }
    }

    override fun removeImageById(id: String) {
        _images.update { current ->
            current.filterNot { it.id == id }
        }
    }

    override fun clear() {
        _images.update { emptyList() }
    }

    override fun clearConsumed(consumedImages: List<AppImageEntity>) {
        if (consumedImages.isEmpty()) return
        val consumedIds = consumedImages.map { it.id }.toSet()
        _images.update { current ->
            current.filterNot { it.id in consumedIds }
        }
    }
}
