package com.lenswise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lenswise.app.presentation.features.history.HistoryScreen
import com.lenswise.app.presentation.features.scanner.ScannerScreen
import com.lenswise.app.presentation.features.scanner.ScannerViewModel
import com.lenswise.app.presentation.features.scanner.viewer.CapturedImageViewerScreen
import com.lenswise.app.presentation.features.settings.SettingsScreen
import com.lenswise.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.lenswise.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.lenswise.app.presentation.navigation.model.AppRoute
import com.lenswise.app.presentation.notification.GlobalNotificationOverlay
import com.lenswise.app.presentation.theme.LensWiseTheme

class MainActivity : ComponentActivity() {

    private val scannerViewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LensWise)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            LensWiseTheme(darkTheme = true) {
                GlobalNotificationOverlay {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            scannerViewModel = scannerViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    scannerViewModel: ScannerViewModel
) {
    var backStack by rememberSaveable { mutableStateOf<List<AppRoute>>(listOf(AppRoute.Scanner)) }

    CupertinoSwipeBackContainer(
        backStack = backStack,
        onPushRoute = { route -> backStack = backStack + route },
        onPopRoute = {
            if (backStack.size > 1) {
                backStack = backStack.dropLast(1)
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
                    onNavigateToViewer = { navigator.push(AppRoute.CapturedViewer) }
                )
            }
            is AppRoute.Settings -> {
                SettingsScreen()
            }
            is AppRoute.History -> {
                HistoryScreen(initialTab = route.initialTab)
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
