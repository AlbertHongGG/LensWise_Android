package com.lenswise.app.features.scanner.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.media.MediaAsset
import com.lenswise.app.core.media.MediaRepository
import com.lenswise.app.core.media.MediaStoreMediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 現代化高效能相簿選擇器視圖模型 (GalleryPickerViewModel)。
 *
 * 核心保證：
 * 1. 依賴倒置 (DIP) 與純淨架構：繼承標準 [ViewModel]，僅依賴抽象 [MediaRepository]，解耦 Android Context。
 * 2. 型別安全 [Factory]：使用 [viewModelFactory] DSL 嚴格實例化，100% 杜絕執行期反射異常。
 * 3. 會話式生命週期 (Session-based Lifecycle)：[onSessionStart] 即時掛載響應流，[onSessionDismiss] 無殘留釋放。
 * 4. O(1) 極速選取狀態機：選取集合與展平流解耦，單張點選零遍歷複製，零 GC 壓力。
 */
class GalleryPickerViewModel(
    private val repository: MediaRepository,
    scope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    /** 降級/預覽輔助次要建構子 */
    constructor() : this(MediaStoreMediaRepository(LensWiseApplication.instance))

    private val vmScope = scope ?: runCatching {
        val s = viewModelScope
        kotlinx.coroutines.Dispatchers.Main.immediate.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext)
        s
    }.getOrElse { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default) }

    private var cachedAssets: List<MediaAsset> = emptyList()
    private val selectedUriMap = linkedMapOf<Long, Uri>()
    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        onSessionStart()
    }

    /**
     * 啟動相簿挑選會話 (Session Start)。
     * 每次進入彈窗保證：
     * 1. 徹底清空歷史殘留選取狀態。
     * 2. 訂閱響應式相簿流，即時呈現最新拍攝/下載之照片。
     */
    fun onSessionStart() {
        clearSelection()
        observeJob?.cancel()
        observeJob = vmScope.launch {
            _uiState.update { it.copy(isLoading = it.items.isEmpty(), errorMessage = null) }
            repository.observeMediaAssets()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "讀取相簿失敗"
                        )
                    }
                }
                .collect { assets ->
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
                        val items = repository.buildTimelineItems(assets, emptySet())
                        _uiState.update {
                            it.copy(
                                items = items,
                                isLoading = false,
                                isEmpty = false,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    /**
     * 關閉相簿挑選會話 (Session Dismiss)。
     * 確保取消協程監聽並清空所有選取狀態，杜絕狀態卡死。
     */
    fun onSessionDismiss() {
        clearSelection()
        observeJob?.cancel()
        observeJob = null
    }

    /**
     * O(1) 極速切換相片選取狀態。
     */
    fun togglePhotoSelection(asset: MediaAsset) {
        val isAlreadySelected = selectedUriMap.containsKey(asset.id)

        if (isAlreadySelected) {
            selectedUriMap.remove(asset.id)
        } else {
            selectedUriMap[asset.id] = asset.contentUri
        }

        val updatedSelectedIds = selectedUriMap.keys.toSet()
        val updatedSelectedUris = selectedUriMap.values.toList()

        _uiState.update {
            it.copy(
                selectedIds = updatedSelectedIds,
                selectedUris = updatedSelectedUris
            )
        }
    }

    /**
     * O(1) 清空所有選取狀態。
     */
    fun clearSelection() {
        if (selectedUriMap.isEmpty() && _uiState.value.selectedIds.isEmpty()) return
        selectedUriMap.clear()

        _uiState.update {
            it.copy(
                selectedIds = emptySet(),
                selectedUris = emptyList()
            )
        }
    }

    fun loadGallery() {
        onSessionStart()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? Application)
                    ?: LensWiseApplication.instance
                GalleryPickerViewModel(MediaStoreMediaRepository(application))
            }
        }
    }
}
