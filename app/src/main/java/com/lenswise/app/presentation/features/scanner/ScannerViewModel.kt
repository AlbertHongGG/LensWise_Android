package com.lenswise.app.presentation.features.scanner

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.camera.CameraManager
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.image.ImageStandardizer
import com.lenswise.app.core.storage.ScannerPreferencesDataSource
import com.lenswise.app.core.storage.ScannerPreferencesRepository
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 掃描器核心視圖模型 (ScannerViewModel)。
 *
 * 遵循現代 Android 架構指南：
 * 1. 繼承標準 [ViewModel]，徹底解耦 Android Framework Context 與反射依賴。
 * 2. 透過 [ScannerPreferencesDataSource] 契約介面實現偏好持久化注入，支援 100% JVM 單元測試隔離。
 * 3. 提供標準 [Factory] (基於 [viewModelFactory] DSL)，編譯期保證型別安全實例化。
 */
class ScannerViewModel(
    private val preferencesDataSource: ScannerPreferencesDataSource
) : ViewModel() {

    constructor() : this(createDefaultPreferencesDataSource())

    private val _uiState = MutableStateFlow(
        ScannerUiState(currentMode = preferencesDataSource.getSavedMode())
    )
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun setMode(mode: ScannerMode) {
        if (_uiState.value.isLocked || _uiState.value.currentMode == mode) return
        _uiState.update { it.copy(currentMode = mode) }
        preferencesDataSource.saveMode(mode)
    }

    fun takePicture(context: Context, cameraManager: CameraManager) {
        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true) }

            val captureResult = cameraManager.capturePhoto()
            captureResult.onSuccess { bytes ->
                val standardizeResult = ImageStandardizer.standardizeFromBytes(
                    context = context,
                    bytes = bytes,
                    source = ImageSource.CAMERA
                )

                standardizeResult.onSuccess { entity ->
                    _uiState.update { state ->
                        state.copy(
                            capturedImages = state.capturedImages + entity,
                            isCapturing = false
                        )
                    }
                    NotificationController.showSuccess("照片拍攝成功 (共 ${_uiState.value.imageCount} 張)")
                }.onFailure { error ->
                    _uiState.update { it.copy(isCapturing = false) }
                    NotificationController.showError("圖片規格化失敗: ${error.localizedMessage}")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isCapturing = false) }
                NotificationController.showError("拍攝照片失敗: ${error.localizedMessage}")
            }
        }
    }

    fun onImagesPicked(context: Context, uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isLocked) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPickingGallery = true) }

            val standardizedList = mutableListOf<AppImageEntity>()
            var failCount = 0

            for (uri in uris) {
                val result = ImageStandardizer.standardizeFromUri(
                    context = context,
                    uri = uri,
                    source = ImageSource.GALLERY
                )
                result.onSuccess { standardizedList.add(it) }
                    .onFailure { error ->
                        android.util.Log.e("ScannerViewModel", "相簿圖片規格化失敗: $uri", error)
                        failCount++
                    }
            }

            if (standardizedList.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        capturedImages = state.capturedImages + standardizedList,
                        isPickingGallery = false
                    )
                }
                NotificationController.showSuccess("已新增 ${standardizedList.size} 張照片 (共 ${_uiState.value.imageCount} 張)")
            } else {
                _uiState.update { it.copy(isPickingGallery = false) }
            }

            if (failCount > 0) {
                NotificationController.showWarning("$failCount 張照片載入規格化失敗")
            }
        }
    }

    fun removeImage(index: Int) {
        val currentImages = _uiState.value.capturedImages
        if (index in currentImages.indices) {
            val updated = currentImages.toMutableList().apply { removeAt(index) }
            _uiState.update { it.copy(capturedImages = updated) }
            NotificationController.showInfo("已刪除照片 (剩餘 ${updated.size} 張)")
        }
    }

    fun clearImages() {
        _uiState.update { it.copy(capturedImages = emptyList()) }
    }

    fun triggerAnalysis(onProceed: ((ScannerMode, List<AppImageEntity>) -> Unit)? = null) {
        val state = _uiState.value
        if (!state.canTriggerAnalysis) {
            if (state.currentMode == ScannerMode.COMPARISON) {
                NotificationController.showWarning("比對模式至少需要選取或拍攝 2 張照片")
            } else {
                NotificationController.showWarning("請至少選取或拍攝 1 張照片")
            }
            return
        }

        if (onProceed != null) {
            onProceed(state.currentMode, state.capturedImages)
        } else {
            NotificationController.showInfo("即將啟動「${state.currentMode.actionTitle}」分析流程 (${state.imageCount} 張照片)")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? Application)
                    ?: LensWiseApplication.instance
                val preferencesRepository = ScannerPreferencesRepository(application)
                ScannerViewModel(preferencesRepository)
            }
        }
    }
}

private object DefaultInMemoryPreferencesDataSource : ScannerPreferencesDataSource {
    private var mode = ScannerMode.PRODUCT
    override fun getSavedMode(): ScannerMode = mode
    override fun saveMode(mode: ScannerMode) {
        this.mode = mode
    }
}

private fun createDefaultPreferencesDataSource(): ScannerPreferencesDataSource {
    return try {
        ScannerPreferencesRepository(LensWiseApplication.instance)
    } catch (_: Throwable) {
        DefaultInMemoryPreferencesDataSource
    }
}


