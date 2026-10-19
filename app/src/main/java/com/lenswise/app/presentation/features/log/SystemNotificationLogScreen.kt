package com.lenswise.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.logging.NotificationLogManager
import com.lenswise.app.domain.model.log.NotificationLogEntry
import com.lenswise.app.presentation.components.PremiumConfigHeader
import com.lenswise.app.presentation.features.log.components.GenericLogViewerScaffold
import com.lenswise.app.presentation.notification.NotificationType

/**
 * 系統通知日誌介面 (SystemNotificationLogScreen)。
 * 100% 依據 GeminiFlowApp 規範復刻排版、尺寸、色階與下拉式滑動確認刪除抽屜。
 */
@Composable
fun SystemNotificationLogScreen(
    logManager: NotificationLogManager,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val logs by logManager.logsFlow.collectAsState()

    GenericLogViewerScaffold(
        items = logs,
        onDeleteSingle = { logManager.clearLog(it.file) },
        onClearAll = { logManager.clearAllLogs() },
        clearDrawerTitle = "CLEAR SYSTEM LOGS",
        emptyMessage = "目前沒有系統通知日誌。",
        modifier = modifier,
        header = {
            PremiumConfigHeader(
                title = "系統通知日誌",
                subtitle = "SYSTEM NOTIFICATION LOG"
            )
        },
        itemContent = { entry, isDragging, itemModifier ->
            NotificationLogCardContent(
                entry = entry,
                isDark = isDark,
                modifier = itemModifier.alpha(if (isDragging) 0.3f else 1.0f)
            )
        }
    )
}

@Composable
private fun NotificationLogCardContent(
    entry: NotificationLogEntry,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val (typeColor, typeIcon) = when (entry.type) {
        NotificationType.SUCCESS -> Pair(Color(0xFF10B981), Icons.Outlined.CheckCircleOutline)
        NotificationType.ERROR -> Pair(Color(0xFFEF4444), Icons.Outlined.ErrorOutline)
        NotificationType.WARNING -> Pair(Color(0xFFF59E0B), Icons.Outlined.WarningAmber)
        NotificationType.INFO -> Pair(Color(0xFF3B82F6), Icons.Outlined.Info)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = entry.type.name,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.type.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = entry.timestampStr,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = entry.message,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )
            }
        }
    }
}
