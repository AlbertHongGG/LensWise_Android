package com.lenswise.app.presentation.features.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenswise.app.core.camera.CameraManager
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.core.image.ImageStandardizer
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun setMode(mode: ScannerMode) {
        if (_uiState.value.isLocked || _uiState.value.currentMode == mode) return
        _uiState.update { it.copy(currentMode = mode) }
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

    fun triggerAnalysis() {
        val state = _uiState.value
        if (!state.canTriggerAnalysis) {
            if (state.currentMode == ScannerMode.COMPARISON) {
                NotificationController.showWarning("比對模式至少需要選取或拍攝 2 張照片")
            } else {
                NotificationController.showWarning("請至少選取或拍攝 1 張照片")
            }
            return
        }

        NotificationController.showInfo("即將啟動「${state.currentMode.actionTitle}」分析流程 (${state.imageCount} 張照片)")
    }
}
