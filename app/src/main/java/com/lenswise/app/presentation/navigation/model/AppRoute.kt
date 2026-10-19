package com.lenswise.app.presentation.navigation.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * LensWise 全域導航路由模型。
 * 遵循不可變資料結構與強型別契約，保證導航狀態的確定性。
 * 繼承 Parcelable 確保 Android 系統生命週期切換 (onSaveInstanceState) 零崩潰。
 */
sealed interface AppRoute : Parcelable {
    val key: String
    val isSwipeBackEnabled: Boolean get() = true

    /** 主相機掃描核心主畫面 (ScannerScreen) */
    @Parcelize
    data object Scanner : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_scanner"
    }

    /** 設定畫面 (SettingsScreen) */
    @Parcelize
    data object Settings : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_settings"
    }

    /** 歷史紀錄畫面 (HistoryScreen) */
    @Parcelize
    data class History(val initialTab: Int = 0) : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_history_$initialTab"
    }

    /** 已捕獲/選取照片全螢幕檢視與刪除畫面 (CapturedImageViewerScreen) - 唯一禁用右滑返回介面 */
    @Parcelize
    data object CapturedViewer : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_captured_viewer"
        @IgnoredOnParcel
        override val isSwipeBackEnabled: Boolean = false
    }

    /** 系統通知日誌畫面 (SystemNotificationLogScreen) */
    @Parcelize
    data object NotificationLog : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_notification_log"
    }

    /** AI / API 請求日誌畫面 (AiLogViewerScreen) */
    @Parcelize
    data object ApiLog : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_api_log"
    }

    /** API 日誌詳細 JSON 檢視畫面 (ApiLogDetailScreen) */
    @Parcelize
    data class ApiLogDetail(val rawJson: String) : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_api_log_detail"
    }

    /** 收據 AI 萃取等待處理畫面 (ReceiptProcessingScreen) */
    @Parcelize
    data class ReceiptProcessing(
        val images: List<com.lenswise.app.core.image.AppImageEntity>
    ) : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_receipt_processing"
    }

    /** 收據結果與串流漸進展開畫面 (ReceiptDetailScreen) */
    @Parcelize
    data class ReceiptDetail(
        val record: com.lenswise.app.domain.model.receipt.ReceiptScannerRecord,
        val isLiveSession: Boolean = false
    ) : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_receipt_detail_${record.id}"
    }

    /** AI 代理調度中心畫面 (AiDispatchCenterScreen) */
    @Parcelize
    data object AiDispatchCenter : AppRoute {
        @IgnoredOnParcel
        override val key: String = "route_ai_dispatch_center"
    }
}
