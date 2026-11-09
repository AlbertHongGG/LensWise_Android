package com.lenswise.app.features.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors

/**
 * API 日誌詳細看板頂部 Header。
 * 1:1 復刻 ReceiptMgr 視覺風格：
 * - 顯示項目名稱 (Title) 與狀態碼徽章 (Status Code Badge)。
 * - 右側配置日誌複製按鈕 (Copy Button)。
 * - 下方緊湊展示耗時 (Duration) 與格式化時間戳 (Timestamp)。
 * - 無上一頁箭頭按鈕，完全由外層向右滑動手勢返回。
 */
@Composable
fun ApiLogDetailHeader(
    title: String,
    status: Int,
    isSuccess: Boolean,
    durationMs: Long,
    formattedTimestamp: String,
    onCopyLog: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val metaIconColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val metaTextColor = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.87f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 第一行：項目名稱 + 狀態碼徽章
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.width(8.dp))

                ApiLogStatusBadge(
                    status = status,
                    isSuccess = isSuccess,
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第二行：耗時 + 格式化時間戳
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = metaIconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$durationMs ms",
                    fontSize = 12.sp,
                    color = metaTextColor
                )

                if (formattedTimestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = metaIconColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTimestamp,
                        fontSize = 12.sp,
                        color = metaTextColor
                    )
                }
            }
        }

        IconButton(
            onClick = onCopyLog
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "複製完整日誌",
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else AppColors.primary
            )
        }
    }
}

/**
 * HTTP 狀態碼徽章組件。
 * 成功時 (2xx) 呈現綠色系，失敗時呈現紅色系。
 */
@Composable
fun ApiLogStatusBadge(
    status: Int,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val (statusBg, statusFg) = if (isSuccess) {
        Pair(
            Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.14f),
            if (isDark) Color(0xFF34D399) else Color(0xFF059669)
        )
    } else {
        Pair(
            Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.14f),
            Color(0xFFEF4444)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(statusBg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$status",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusFg
        )
    }
}
