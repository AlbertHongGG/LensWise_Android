package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.lenswise.app.presentation.components.sheet.LensWiseAdjustableDrawer
import com.lenswise.app.presentation.components.sheet.LensWiseSheetHandle
import com.lenswise.app.presentation.theme.AppColors

/**
 * AI 決策與推薦總結抽屜 (AiVerdictBottomSheet)。
 * 採用 LensWise 統一可調整抽屜架構 (Type B: LensWiseAdjustableDrawer)：
 * - 首次開啟預設佔 60% 高度 (defaultHeightRatio = 0.60f)
 * - 頂部拉桿與標題列大面積拖曳，支援拉到任意高度自由停放
 * - Body 內容完全解耦：滑動長文 Markdown 絕對不影響抽屜高度，徹底杜絕截斷與懸空 Bug
 * - 唯有向下拖拉至底部緩衝門檻 (< 0.20f) 才觸發平滑收合關閉
 * - 串流中繼骨架屏與結構化 Markdown 渲染維持 100% 現有設計
 */
@Composable
fun AiVerdictBottomSheet(
    summary: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    LensWiseAdjustableDrawer(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        defaultHeightRatio = 0.60f,
        maxHeightRatio = 0.90f,
        hasScrim = true,
        headerContent = { dragModifier ->
            // 把手與標題整塊作為拖曳感應區
            Column(
                modifier = dragModifier.fillMaxWidth()
            ) {
                LensWiseSheetHandle(isDark = isDark)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp, top = 4.dp)
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
            }
        },
        content = {
            // Body 槽位：Markdown 內容獨立滾動，與抽屜高度手勢完全解耦
            if (summary.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
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
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    RenderStructuredVerdictMarkdown(
                        markdown = summary,
                        isDark = isDark
                    )
                }
            }
        }
    )
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
