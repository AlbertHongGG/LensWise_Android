package com.lenswise.app.presentation.navigation.contract

import androidx.compose.runtime.staticCompositionLocalOf
import com.lenswise.app.presentation.navigation.model.AppRoute

interface CupertinoNavigator {
    fun push(route: AppRoute)
    fun pop()
}

val LocalCupertinoNavigator = staticCompositionLocalOf<CupertinoNavigator> {
    error("No CupertinoNavigator provided in composition tree")
}
