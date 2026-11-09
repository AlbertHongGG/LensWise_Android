package com.lenswise.app.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.lenswise.app.core.logging.ApiLogManager
import com.lenswise.app.features.log.components.ApiLogDetailHeader
import com.lenswise.app.features.log.components.JsonTreeViewer
import com.lenswise.app.features.log.model.ApiLogDetailUiState
import com.lenswise.app.presentation.notification.NotificationController
import com.lenswise.app.presentation.theme.AppColors

/**
 * API 日誌詳細資料畫面 (ApiLogDetailScreen)。
 *
 * 遵循 Android 架構標準與 Clean Architecture：
 * - 依據 logId 按需單次載入日誌 JSON，絕不在導航路由中跨介面傳遞巨量文字，杜絕 TransactionTooLargeException。
 * - 頂部看板：項目名稱標題、狀態碼徽章 (Status Code Badge)、複製整份 Log 按鈕、耗時與格式化時間戳。
 * - 移除上一頁箭頭按鈕：全站統一透過外層 CupertinoSwipeBackContainer 向右滑動返回。
 * - 內容區呈現 request 與 response 互動式摺疊樹狀結構 (JsonTreeViewer)。
 */
@Composable
fun ApiLogDetailScreen(
    logId: String,
    apiLogManager: ApiLogManager,
    modifier: Modifier = Modifier
) {
    val rawJson = remember(logId) {
        apiLogManager.getLogRawJson(logId) ?: "{}"
    }

    ApiLogDetailContent(
        rawJson = rawJson,
        modifier = modifier
    )
}

/**
 * 無狀態純 UI 內容呈現容器 (可直接供 Compose Previews 與獨立測試)。
 */
@Composable
fun ApiLogDetailContent(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current

    val uiState = remember(rawJson) {
        ApiLogDetailUiState.fromRawJson(rawJson)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部看板 (右滑返回，無上一頁按鈕)
            ApiLogDetailHeader(
                title = uiState.title,
                status = uiState.status,
                isSuccess = uiState.isSuccess,
                durationMs = uiState.durationMs,
                formattedTimestamp = uiState.formattedTimestamp,
                onCopyLog = {
                    clipboardManager.setText(AnnotatedString(uiState.rawJson))
                    NotificationController.showSuccess("已複製日誌 JSON")
                },
                isDark = isDark
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.requestData != null) {
                    JsonTreeViewer(
                        data = uiState.requestData,
                        rootName = "request",
                        isDark = isDark
                    )
                }

                if (uiState.responseData != null) {
                    JsonTreeViewer(
                        data = uiState.responseData,
                        rootName = "response",
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
