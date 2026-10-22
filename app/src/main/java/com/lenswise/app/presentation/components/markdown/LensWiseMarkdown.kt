package com.lenswise.app.presentation.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 模組化高品質 Markdown 渲染器 (LensWiseMarkdown)。
 * 1:1 對齊 LensWise Flutter 版 _buildPremiumMarkdownStyleSheet 規範：
 * - 支援階層式標題 (# H1, ## H2, ### H3)
 * - 支援引用區塊 (> Blockquote)，左側品牌色裝飾豎條與淺底反白
 * - 支援無序清單 (- / *) 與有序清單 (1. 2. 3.)
 * - 支援程式碼區塊 (```code```) 與行內碼 (`code`)
 * - 支援粗體 (**text**) 與斜體 (*text*) 巢狀語意解析
 * - 完美適配 Light Mode (淺灰/純白) 與 Dark Mode
 */
@Composable
fun LensWiseMarkdown(
    content: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val normalTextColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1F2937)
    val mutedTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val codeBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F5F9)
    val codeBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    val lines = content.lines()
    var inCodeBlock = false
    val codeBlockBuffer = StringBuilder()

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, rawLine ->
            val trimmed = rawLine.trim()

            // 處理代碼塊開關 ```
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // 結束代碼塊
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBgColor)
                            .border(1.dp, codeBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = codeBlockBuffer.toString().trimEnd(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFFE5C07B) else Color(0xFF6B21A8),
                            lineHeight = 18.sp
                        )
                    }
                    codeBlockBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(rawLine).append("\n")
                return@forEachIndexed
            }

            // 一般 Markdown 元素解析
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        fontFamily = OutfitFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        fontFamily = OutfitFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else AppColors.primary,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        fontFamily = OutfitFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF111827),
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 3.dp)
                    )
                }

                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(AppColors.primary.copy(alpha = if (isDark) 0.12f else 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AppColors.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("> ").trim(), normalTextColor, isDark),
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Normal,
                            color = normalTextColor,
                            lineHeight = 20.sp
                        )
                    }
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletText = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 10.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AppColors.primary)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText, normalTextColor, isDark),
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = normalTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val prefix = trimmed.substringBefore(".")
                    val body = trimmed.substringAfter(".").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$prefix.",
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(body, normalTextColor, isDark),
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = normalTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed, normalTextColor, isDark),
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = normalTextColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 解析行內語意 Markdown (**粗體**, *斜體*, `行內碼`)。
 */
private fun parseInlineMarkdown(
    text: String,
    normalColor: Color,
    isDark: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`)")
        val matches = regex.findAll(text)

        matches.forEach { match ->
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }

            val matchedValue = match.value
            when {
                matchedValue.startsWith("**") && matchedValue.endsWith("**") -> {
                    val inner = matchedValue.removeSurrounding("**")
                    val start = length
                    append(inner)
                    addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = normalColor
                        ),
                        start,
                        length
                    )
                }

                matchedValue.startsWith("`") && matchedValue.endsWith("`") -> {
                    val inner = matchedValue.removeSurrounding("`")
                    val start = length
                    append(inner)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFFE5C07B) else Color(0xFF7C3AED),
                            background = if (isDark) Color.White.copy(0.10f) else Color.Black.copy(0.05f)
                        ),
                        start,
                        length
                    )
                }

                matchedValue.startsWith("*") && matchedValue.endsWith("*") -> {
                    val inner = matchedValue.removeSurrounding("*")
                    val start = length
                    append(inner)
                    addStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = normalColor
                        ),
                        start,
                        length
                    )
                }

                else -> {
                    append(matchedValue)
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
