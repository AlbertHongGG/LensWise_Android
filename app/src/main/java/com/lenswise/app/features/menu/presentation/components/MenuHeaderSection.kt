package com.lenswise.app.features.menu.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.menu.MenuScannerRecord

/**
 * 菜單頂部襯線字體店名與品項統計區塊 (MenuHeaderSection)。
 * 1:1 對齊 LensWise Flutter 版：
 * - 襯線字體 (Serif) 28dp 大標題
 * - 40x2dp 極簡中央水平裝飾線
 * - 字母大間距品項數量副標。
 */
@Composable
fun MenuHeaderSection(
    record: MenuScannerRecord,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val storeName = record.storeName.ifBlank { "翻譯菜單" }
    val itemCount = record.items.size

    val textColor = if (isDark) Color.White else Color(0xFF1F2937)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = storeName,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = textColor,
                lineHeight = 34.sp,
                letterSpacing = 2.0.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$itemCount 個品項",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f),
                letterSpacing = 1.0.sp
            )
        )
    }
}
