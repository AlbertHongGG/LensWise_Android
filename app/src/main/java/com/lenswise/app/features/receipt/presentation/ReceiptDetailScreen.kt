package com.lenswise.app.features.receipt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.ai.agents.receipt.ReceiptChatAgent
import com.lenswise.app.features.receipt.presentation.components.ReceiptChatBottomSheet
import com.lenswise.app.features.receipt.presentation.components.ReceiptPaperCard
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 收據萃取結果與串流漸進展開畫面 (ReceiptDetailScreen)。
 * 1:1 對齊 Flutter 原生設計：
 * 採用暖灰亮色背景 (#F5F5F5) 襯托純白立體擬真發票紙卡片，並提供懸浮 AI 顧問入口。
 */
@Composable
fun ReceiptDetailScreen(
    record: ReceiptScannerRecord,
    isLiveSession: Boolean = false,
    viewModel: ReceiptScannerViewModel? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    var showChatSheet by remember { mutableStateOf(false) }

    val liveState by viewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(ReceiptScannerUiState(receiptRecord = record)) }

    val currentRecord = if (isLiveSession && liveState.receiptRecord.storeName.isNotEmpty()) {
        liveState.receiptRecord
    } else {
        record
    }

    val isStreaming = isLiveSession && liveState.status == ReceiptScannerStatus.STREAMING
    var activeChatAgent by remember { mutableStateOf<ReceiptChatAgent?>(null) }

    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 頂部返回導航列 (透明背景)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = if (isDark) Color.White else Color.Black.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Text(
                    text = "收據明細",
                    style = AppTypography.cardTitle.copy(
                        fontSize = 17.sp,
                        color = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // 主要發票紙卡片滑動區域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReceiptPaperCard(
                    record = currentRecord,
                    isStreaming = isStreaming
                )

                // 預留底部 FAB 空間
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // 底部懸浮 AI 顧問入口
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = {
                    val agent = viewModel?.createChatAgent()
                    if (agent != null) {
                        activeChatAgent = agent
                        showChatSheet = true
                    }
                },
                containerColor = AppColors.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI 顧問",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "問 AI 顧問",
                        style = AppTypography.tileTitle.copy(
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        val currentChatAgent = activeChatAgent
        if (showChatSheet && currentChatAgent != null) {
            ReceiptChatBottomSheet(
                record = currentRecord,
                chatAgent = currentChatAgent,
                onDismissRequest = { showChatSheet = false }
            )
        }
    }
}
