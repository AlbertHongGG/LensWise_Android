package com.lenswise.app.features.menu.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.menu.MenuScannerItem
import com.lenswise.app.presentation.theme.AppColors

/**
 * 菜單單一品項卡片 (MenuListItemTile)。
 * 1:1 對齊 LensWise Flutter 版：
 * - 左右對齊品項譯名、原文與價格標註
 * - 經典引言風格區塊 (The Quote Block)：左側 3dp 原廠主題提示邊線與斜體說明
 * - 極微弱定海神針分隔線。
 */
@Composable
fun MenuListItemTile(
    item: MenuScannerItem,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1F2937)
    val hasDescription = !item.description.isNullOrBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.translatedName,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.2).sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.originalName,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            val priceText = if (item.price > 0.0) "%.0f".format(item.price) else "-"
            Text(
                text = priceText,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.primary
                )
            )
        }

        // 引言區塊說明 (The Quote Block)
        if (hasDescription) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(AppColors.primary.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.description.orEmpty(),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )
        }
    }
}
