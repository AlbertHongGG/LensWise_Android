package com.lenswise.app.presentation.navigation.model

/**
 * LensWise 全域導航路由模型。
 * 遵循不可變資料結構與強型別契約，保證導航狀態的確定性。
 */
sealed interface AppRoute {
    val key: String
    val isSwipeBackEnabled: Boolean get() = true

    /** 主相機掃描核心主畫面 (ScannerScreen) */
    data object Scanner : AppRoute {
        override val key: String = "route_scanner"
    }

    /** 設定畫面 (SettingsScreen) */
    data object Settings : AppRoute {
        override val key: String = "route_settings"
    }

    /** 歷史紀錄畫面 (HistoryScreen) */
    data class History(val initialTab: Int = 0) : AppRoute {
        override val key: String = "route_history_$initialTab"
    }

    /** 已捕獲/選取照片全螢幕檢視與刪除畫面 (CapturedImageViewerScreen) - 唯一禁用右滑返回介面 */
    data object CapturedViewer : AppRoute {
        override val key: String = "route_captured_viewer"
        override val isSwipeBackEnabled: Boolean = false
    }
}
