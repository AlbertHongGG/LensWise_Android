package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.components.StreamingSkeleton
import com.lenswise.app.presentation.theme.AppColors

/**
 * AI 決策與推薦總結抽屜 (AiVerdictBottomSheet)。
 * 1:1 對齊 LensWise Flutter 版 _showAiVerdictSheet：
 * - 兩段式高度：剛彈出時佔 60% (PartiallyExpanded)，使用者往上滑動時平滑展開至 85%
 * - 串流中繼骨架屏 (3 行 20dp 骨架)
 * - 結構化 Markdown 渲染 (## 核心結論、## 選購指南、## 關鍵取捨與避坑)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVerdictBottomSheet(
    summary: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // 標題列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "AI 推薦總結",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF111418),
                        letterSpacing = 0.5.sp
                    )
                )
            }

            if (summary.isBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 20.dp, isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 20.dp, isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    StreamingSkeleton(modifier = Modifier.width(200.dp), height = 20.dp, isDark = isDark)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    RenderStructuredVerdictMarkdown(
                        markdown = summary,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

/**
 * 簡易而嚴謹之結構化 Markdown 渲染器。
 */
@Composable
private fun RenderStructuredVerdictMarkdown(
    markdown: String,
    isDark: Boolean
) {
    val lines = markdown.lines()
    val textColor = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF2D3748)

    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.primary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else AppColors.primary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF1F2937)
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(AppColors.primary.copy(alpha = if (isDark) 0.12f else 0.06f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = line.removePrefix("> ").trim(),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF374151),
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletText = line.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AppColors.primary)
                        )
                        Text(
                            text = buildAnnotatedBoldString(bulletText, textColor),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = textColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> {
                    Text(
                        text = buildAnnotatedBoldString(line, textColor),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = textColor
                        ),
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * 簡易粗體 **text** 語意解析支援。
 */
private fun buildAnnotatedBoldString(text: String, normalColor: Color) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = normalColor
                )
            ) {
                append(part)
            }
        } else {
            withStyle(SpanStyle(color = normalColor)) {
                append(part)
            }
        }
    }
}
