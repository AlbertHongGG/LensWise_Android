package com.lenswise.app.features.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.lenswise.app.core.camera.CameraManager
import com.lenswise.app.core.media.MediaAccessManager
import com.lenswise.app.features.scanner.components.CameraViewfinder
import com.lenswise.app.features.scanner.components.ScannerBottomControls
import com.lenswise.app.features.scanner.components.ScannerTopBar
import com.lenswise.app.features.scanner.gallery.CustomGalleryBottomSheet
import com.lenswise.app.features.scanner.gallery.GalleryPickerViewModel
import com.lenswise.app.presentation.notification.NotificationController
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToViewer: () -> Unit,
    onStartAnalysis: (com.lenswise.app.domain.scanner.ScannerMode, List<com.lenswise.app.core.image.AppImageEntity>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cameraManager = remember { CameraManager(context) }
    val hazeState = remember { HazeState() }
    var showCustomGallery by remember { mutableStateOf(false) }

    // 提前在背景預熱相簿狀態與首屏微縮圖 (0ms 開窗瞬顯)
    val galleryPickerViewModel: GalleryPickerViewModel = viewModel(factory = GalleryPickerViewModel.Factory)

    // 1. 相機權限請求器
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationController.showSuccess("相機授權成功")
        } else {
            NotificationController.showError("需要相機權限才能進行即時取景與拍攝")
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 2. 媒體相簿權限請求器 (統一委託 MediaAccessManager 跨版本管理)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        if (MediaAccessManager.isPermissionGranted(resultMap)) {
            showCustomGallery = true
        } else {
            NotificationController.showError("需要相簿讀取權限才能挑選照片")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. 全螢幕相機視口 (相簿開啟時自動休眠 unbind，100% 釋放 45% CPU/GPU 算力予相簿滑動)
        CameraViewfinder(
            cameraManager = cameraManager,
            isActive = !showCustomGallery,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        )

        // 2. 頂部列 (設定與歷史紀錄入口)
        ScannerTopBar(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToHistory = onNavigateToHistory,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. 底部綜合控制區塊 (發光分析按鈕、快門三鍵列、模式切換膠囊)
        ScannerBottomControls(
            uiState = uiState,
            hazeState = hazeState,
            onTakeShutter = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.takePicture(context, cameraManager)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPickGallery = {
                if (MediaAccessManager.hasPermission(context)) {
                    showCustomGallery = true
                } else {
                    mediaPermissionLauncher.launch(MediaAccessManager.getRequiredPermissions())
                }
            },
            onOpenViewer = onNavigateToViewer,
            onTriggerAnalysis = { viewModel.triggerAnalysis(onStartAnalysis) },
            onSelectMode = { viewModel.setMode(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 4. 高效能自製相簿選擇器底部彈窗 (零卡頓、日期分組、多選勾選遮罩)
        if (showCustomGallery) {
            CustomGalleryBottomSheet(
                onDismiss = { showCustomGallery = false },
                onConfirmSelection = { uris ->
                    viewModel.onImagesPicked(context, uris)
                },
                viewModel = galleryPickerViewModel
            )
        }
    }
}

