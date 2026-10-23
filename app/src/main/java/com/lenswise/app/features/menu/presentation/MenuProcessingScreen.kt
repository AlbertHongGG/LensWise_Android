package com.lenswise.app.features.menu.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.presentation.components.ThinkingOrb
import com.lenswise.app.presentation.theme.AppColors

/**
 * 菜單掃描與辨識等待處理畫面 (MenuProcessingScreen)。
 * 1:1 對齊 LensWise Flutter 版：
 * - ThinkingOrb 旋轉核心
 * - 雙階段動態提示文案（菜單解析中 -> 菜單辨識完成載入中）
 * - 亮色對齊 PremiumErrorView 錯誤處理。
 */
@Composable
fun MenuProcessingScreen(
    images: List<AppImageEntity>,
    viewModel: MenuScannerViewModel,
    onTransitionToResult: (MenuScannerRecord) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val state by viewModel.uiState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.reset()
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        val currentStatus = viewModel.uiState.value.status
        if (currentStatus != MenuScannerStatus.CONNECTING && currentStatus != MenuScannerStatus.STREAMING) {
            viewModel.reset()
            viewModel.analyzeMenu(images)
        }
    }

    LaunchedEffect(state.status) {
        if ((state.status == MenuScannerStatus.STREAMING || state.status == MenuScannerStatus.SUCCESS) && !hasNavigated) {
            hasNavigated = true
            onTransitionToResult(state.record)
        }
    }

    val backgroundColor = if (isDark) AppColors.backgroundDark else AppColors.backgroundLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.status == MenuScannerStatus.ERROR && state.errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = if (isDark) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEE2E2),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "辨識失敗",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "解析失敗",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = state.errorMessage ?: "無法完成菜單文字辨識與翻譯，請確認網路連線或 API 金鑰設定。",
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        viewModel.reset()
                        viewModel.analyzeMenu(images)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "重新嘗試",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "返回重新拍攝",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            val isStreaming = state.status == MenuScannerStatus.STREAMING || state.status == MenuScannerStatus.SUCCESS
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ThinkingOrb(
                    size = 100.dp,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isStreaming) "菜單辨識完成，載入中..." else "AI 菜單解析中...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isStreaming) "即將為您即時展開菜單品項" else "正在為您辨識並翻譯菜單文字\n這可能需要幾秒鐘的時間",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}
