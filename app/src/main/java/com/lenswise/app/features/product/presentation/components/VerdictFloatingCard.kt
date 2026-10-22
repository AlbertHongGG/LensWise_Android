package com.lenswise.app.features.product.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.components.StreamingSkeleton
import com.lenswise.app.presentation.theme.AppColors

/**
 * 商品核心特色概述懸浮卡片 (VerdictFloatingCard)。
 * 1:1 對齊 LensWise Flutter 版：
 * 串流骨架屏 (32x32 圓圖示 + 80x18 標題 + 全寬/220dp 雙行骨架屏)、
 * 高光晶鑽邊框 (Primary 0.3 alpha) 與專業行距字級排版。
 */
@Composable
fun VerdictFloatingCard(
    summary: String,
    isLiveSession: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    if (summary.isBlank()) {
        if (!isLiveSession) return

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
                )
                .border(
                    width = 1.dp,
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreamingSkeleton(
                        width = 32.dp,
                        height = 32.dp,
                        shape = CircleShape,
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StreamingSkeleton(
                        width = 80.dp,
                        height = 18.dp,
                        isDark = isDark
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                StreamingSkeleton(
                    modifier = Modifier.fillMaxWidth(),
                    height = 14.dp,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                StreamingSkeleton(
                    width = 220.dp,
                    height = 14.dp,
                    isDark = isDark
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AppColors.primary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
            )
            .border(
                width = 1.dp,
                color = AppColors.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "特色概述",
                        tint = AppColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "商品簡介",
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        letterSpacing = 0.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summary,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF334155),
                    lineHeight = 24.sp,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}
