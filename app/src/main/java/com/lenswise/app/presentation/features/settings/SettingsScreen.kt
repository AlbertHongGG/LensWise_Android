package com.lenswise.app.presentation.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 設定頁面（第一階段入口佔位畫面）。
 * 嚴格零上一頁按鈕：自左側螢幕邊緣向右滑動即可退回主畫面。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.backgroundDark)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "設定",
                color = Color.White,
                fontFamily = OutfitFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "設定介面規劃中。無須點擊返回鍵，直接自螢幕左邊緣向右滑動即可返回相機主畫面。",
                color = AppColors.textSecondaryDark,
                fontFamily = OutfitFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
