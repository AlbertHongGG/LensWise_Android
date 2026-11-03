package com.lenswise.app.presentation.components.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.lenswise.app.core.scanner.BaseScannerViewModel
import com.lenswise.app.core.scanner.ScanAnalysisStatus
import com.lenswise.app.domain.model.history.IHistoryRecord
import com.lenswise.app.presentation.components.ThinkingOrb
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 處理中畫面文案配置資料模型 (ScannerProcessingTextConfig)。
 */
data class ScannerProcessingTextConfig(
    val connectingTitle: String,
    val connectingSubtitle: String,
    val streamingTitle: String,
    val streamingSubtitle: String,
    val errorTitle: String = "解析失敗",
    val defaultErrorMessage: String,
    val retryButtonText: String = "重新嘗試",
    val backButtonText: String = "返回相機"
)

/**
 * 通用掃描分析等待處理畫面鷹架 (ScannerProcessingScaffold)。
 * 封裝 1:1 對齊 LensWise 設計的 ThinkingOrb 動畫、錯誤重試視圖與狀態跳轉監聽，
 * 徹底消滅各模組重複的樣板程式碼與非同步競態。
 */
@Composable
fun <TRecord : IHistoryRecord> ScannerProcessingScaffold(
    viewModel: BaseScannerViewModel<TRecord>,
    config: ScannerProcessingTextConfig,
    onTransitionToDetail: (TRecord) -> Unit,
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

    LaunchedEffect(state.status, state.sessionId) {
        if ((state.status == ScanAnalysisStatus.STREAMING || state.status == ScanAnalysisStatus.SUCCESS) && !hasNavigated) {
            hasNavigated = true
            onTransitionToDetail(state.record)
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
        if (state.status == ScanAnalysisStatus.ERROR && state.errorMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.15f) else Color(0xFFD32F2F).copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isDark) Color(0xFFFF5252).copy(alpha = 0.3f) else Color(0xFFD32F2F).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = config.errorTitle,
                        tint = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F),
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = config.errorTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.errorMessage ?: config.defaultErrorMessage,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    fontFamily = OutfitFontFamily,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = { viewModel.retryAnalysis() },
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
                        text = config.retryButtonText,
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
                        text = config.backButtonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            val isStreaming = state.status == ScanAnalysisStatus.STREAMING || state.status == ScanAnalysisStatus.SUCCESS
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
                    text = if (isStreaming) config.streamingTitle else config.connectingTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isStreaming) config.streamingSubtitle else config.connectingSubtitle,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontFamily = OutfitFontFamily,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}
