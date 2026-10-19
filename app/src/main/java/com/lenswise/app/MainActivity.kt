package com.lenswise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lenswise.app.domain.scanner.ScannerMode
import com.lenswise.app.features.receipt.presentation.ReceiptDetailScreen
import com.lenswise.app.features.receipt.presentation.ReceiptProcessingScreen
import com.lenswise.app.features.receipt.presentation.ReceiptScannerViewModel
import com.lenswise.app.presentation.features.history.HistoryScreen
import com.lenswise.app.presentation.features.log.AiLogViewerScreen
import com.lenswise.app.presentation.features.log.ApiLogDetailScreen
import com.lenswise.app.presentation.features.log.SystemNotificationLogScreen
import com.lenswise.app.presentation.features.scanner.ScannerScreen
import com.lenswise.app.presentation.features.scanner.ScannerViewModel
import com.lenswise.app.presentation.features.scanner.viewer.CapturedImageViewerScreen
import com.lenswise.app.presentation.features.settings.SettingsScreen
import com.lenswise.app.presentation.features.settings.SettingsViewModel
import com.lenswise.app.presentation.features.splash.SplashScreen
import com.lenswise.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.lenswise.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.lenswise.app.presentation.navigation.model.AppRoute
import com.lenswise.app.presentation.notification.GlobalNotificationOverlay
import com.lenswise.app.presentation.notification.NotificationController
import com.lenswise.app.presentation.theme.LensWiseTheme

class MainActivity : ComponentActivity() {

    private val scannerViewModel: ScannerViewModel by viewModels { ScannerViewModel.Factory }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }
    private val receiptScannerViewModel: ReceiptScannerViewModel by viewModels { ReceiptScannerViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LensWise)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            val settingsUiState by settingsViewModel.uiState.collectAsState()

            LensWiseTheme(darkTheme = settingsUiState.isDarkMode) {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith
                            fadeOut(animationSpec = tween(400))
                    },
                    label = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onAnimationFinished = { showSplash = false }
                        )
                    } else {
                        GlobalNotificationOverlay {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AppNavigation(
                                    scannerViewModel = scannerViewModel,
                                    settingsViewModel = settingsViewModel,
                                    receiptScannerViewModel = receiptScannerViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val AppRouteListSaver: Saver<List<AppRoute>, ArrayList<AppRoute>> = Saver(
    save = { list -> ArrayList(list) },
    restore = { list -> list.ifEmpty { listOf(AppRoute.Scanner) } }
)

@Composable
fun AppNavigation(
    scannerViewModel: ScannerViewModel,
    settingsViewModel: SettingsViewModel,
    receiptScannerViewModel: ReceiptScannerViewModel
) {
    var backStack by rememberSaveable(stateSaver = AppRouteListSaver) {
        mutableStateOf(listOf(AppRoute.Scanner))
    }

    val context = LocalContext.current
    val app = context.applicationContext as LensWiseApplication

    CupertinoSwipeBackContainer(
        backStack = backStack,
        onPushRoute = { route -> backStack = backStack + route },
        onPopRoute = {
            if (backStack.size > 1) {
                backStack = backStack.dropLast(1)
            }
        },
        onReplaceRoute = { route ->
            backStack = if (backStack.isNotEmpty()) {
                backStack.dropLast(1) + route
            } else {
                listOf(route)
            }
        }
    ) { route ->
        val navigator = LocalCupertinoNavigator.current

        when (route) {
            is AppRoute.Scanner -> {
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateToSettings = { navigator.push(AppRoute.Settings) },
                    onNavigateToHistory = { navigator.push(AppRoute.History()) },
                    onNavigateToViewer = { navigator.push(AppRoute.CapturedViewer) },
                    onStartAnalysis = { mode, images ->
                        when (mode) {
                            ScannerMode.RECEIPT -> {
                                navigator.push(AppRoute.ReceiptProcessing(images))
                            }
                            else -> {
                                NotificationController.showInfo("「${mode.title}」分析模組即將上線")
                            }
                        }
                    }
                )
            }
            is AppRoute.ReceiptProcessing -> {
                ReceiptProcessingScreen(
                    images = route.images,
                    viewModel = receiptScannerViewModel,
                    onTransitionToDetail = { record ->
                        navigator.replace(AppRoute.ReceiptDetail(record, isLiveSession = true))
                    },
                    onNavigateBack = { navigator.pop() }
                )
            }
            is AppRoute.ReceiptDetail -> {
                ReceiptDetailScreen(
                    record = route.record,
                    isLiveSession = route.isLiveSession,
                    viewModel = receiptScannerViewModel
                )
            }
            is AppRoute.Settings -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToNotificationLogs = { navigator.push(AppRoute.NotificationLog) },
                    onNavigateToAiLogs = { navigator.push(AppRoute.ApiLog) },
                    onConfigureAiEngine = { navigator.push(AppRoute.AiDispatchCenter) }
                )
            }
            is AppRoute.AiDispatchCenter -> {
                com.lenswise.app.presentation.features.settings.AiDispatchCenterScreen(
                    onNavigateBack = { navigator.pop() }
                )
            }
            is AppRoute.NotificationLog -> {
                SystemNotificationLogScreen(
                    logManager = app.notificationLogManager
                )
            }
            is AppRoute.ApiLog -> {
                AiLogViewerScreen(
                    apiLogManager = app.apiLogManager,
                    onNavigateToDetail = { rawJson -> navigator.push(AppRoute.ApiLogDetail(rawJson)) }
                )
            }
            is AppRoute.ApiLogDetail -> {
                ApiLogDetailScreen(
                    rawJson = route.rawJson,
                    onNavigateBack = { navigator.pop() }
                )
            }
            is AppRoute.History -> {
                HistoryScreen(
                    initialTab = route.initialTab,
                    onNavigateToReceiptDetail = { record ->
                        navigator.push(AppRoute.ReceiptDetail(record = record, isLiveSession = false))
                    }
                )
            }
            is AppRoute.CapturedViewer -> {
                CapturedImageViewerScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = { navigator.pop() }
                )
            }
        }
    }
}
