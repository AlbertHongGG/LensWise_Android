package com.lenswise.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.features.log.components.JsonTreeViewer
import com.lenswise.app.presentation.theme.AppColors
import org.json.JSONObject

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

@Composable
fun ApiLogDetailScreen(
    rawJson: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    val parsed = remember(rawJson) {
        try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    val agentName = parsed.optString("agentName", "API Request")
    val durationMs = if (parsed.has("durationMs")) "${parsed.optLong("durationMs")} ms" else "N/A"
    val timestamp = parsed.optString("timestamp", "")
    val requestObj = parsed.opt("request")
    val responseObj = parsed.opt("response")

    val metaIconColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val metaTextColor = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.87f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = if (isDark) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = agentName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = metaIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Duration: $durationMs",
                        fontSize = 13.sp,
                        color = metaTextColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = metaIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Time: $timestamp",
                        fontSize = 13.sp,
                        color = metaTextColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (requestObj != null && requestObj != JSONObject.NULL) {
                    JsonTreeViewer(
                        data = requestObj,
                        rootName = "request",
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (responseObj != null && responseObj != JSONObject.NULL) {
                    JsonTreeViewer(
                        data = responseObj,
                        rootName = "response",
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
