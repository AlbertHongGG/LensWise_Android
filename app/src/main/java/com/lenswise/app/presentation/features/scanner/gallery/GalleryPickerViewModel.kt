package com.lenswise.app.presentation.features.scanner.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lenswise.app.core.media.GalleryUiItem
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.MediaRepository
import com.lenswise.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 現代化高效能相簿選擇器視圖模型 (GalleryPickerViewModel)。
 *
 * 核心保證：
 * 1. 單一真理來源 (Single Source of Truth)：集中管理全相簿元數據、選取清單與時間軸狀態。
 * 2. 毫秒級極速索引與一維展平項目流，杜絕重複計算與巢狀 Recomposition。
 * 3. 精確選取單元格更新：點選照片時僅拷貝變更單元格，其餘項目參考完全不變，Compose 100% 跳過重組。
 */
class GalleryPickerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private var cachedAssets: List<MediaAsset> = emptyList()
    private val selectedUriMap = linkedMapOf<Long, Uri>()

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadGallery()
    }

    fun setMaxSelection(max: Int) {
        if (_uiState.value.maxSelection != max) {
            _uiState.update { it.copy(maxSelection = max) }
        }
    }

    fun loadGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val assets = repository.queryAllMediaAssets()
                cachedAssets = assets
                if (assets.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmpty = true,
                            items = emptyList()
                        )
                    }
                } else {
                    val items = repository.buildTimelineItems(assets, selectedUriMap.keys)
                    _uiState.update {
                        it.copy(
                            items = items,
                            isLoading = false,
                            isEmpty = false,
                            selectedIds = selectedUriMap.keys.toSet(),
                            selectedUris = selectedUriMap.values.toList()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "讀取相簿失敗"
                    )
                }
            }
        }
    }

    fun togglePhotoSelection(asset: MediaAsset) {
        val currentState = _uiState.value
        val isAlreadySelected = selectedUriMap.containsKey(asset.id)

        if (isAlreadySelected) {
            selectedUriMap.remove(asset.id)
        } else {
            if (selectedUriMap.size >= currentState.maxSelection) {
                NotificationController.showWarning("最多只能選擇 ${currentState.maxSelection} 張照片")
                return
            }
            selectedUriMap[asset.id] = asset.contentUri
        }

        val isNowSelected = !isAlreadySelected
        val updatedSelectedIds = selectedUriMap.keys.toSet()
        val updatedSelectedUris = selectedUriMap.values.toList()

        // 僅更新被點擊的單元格，保持其他 99.9% 項目的對象參考不變以觸發 Compose 跳過機制
        val updatedItems = currentState.items.map { item ->
            if (item is GalleryUiItem.Photo && item.asset.id == asset.id) {
                item.copy(isSelected = isNowSelected)
            } else {
                item
            }
        }

        _uiState.update {
            it.copy(
                items = updatedItems,
                selectedIds = updatedSelectedIds,
                selectedUris = updatedSelectedUris
            )
        }
    }

    fun clearSelection() {
        if (selectedUriMap.isEmpty()) return
        val previousSelectedIds = selectedUriMap.keys.toSet()
        selectedUriMap.clear()

        val updatedItems = _uiState.value.items.map { item ->
            if (item is GalleryUiItem.Photo && previousSelectedIds.contains(item.asset.id)) {
                item.copy(isSelected = false)
            } else {
                item
            }
        }

        _uiState.update {
            it.copy(
                items = updatedItems,
                selectedIds = emptySet(),
                selectedUris = emptyList()
            )
        }
    }
}
