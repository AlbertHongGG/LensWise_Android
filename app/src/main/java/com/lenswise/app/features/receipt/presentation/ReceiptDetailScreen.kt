package com.lenswise.app.features.receipt.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lenswise.app.LensWiseApplication
import com.lenswise.app.core.ai.provider.AiProviderFactory
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.ai.agents.receipt.ReceiptChatAgent
import com.lenswise.app.features.receipt.presentation.components.ReceiptChatBottomSheet
import com.lenswise.app.features.receipt.presentation.components.ReceiptPaperCard
import com.lenswise.app.presentation.theme.AppColors

/**
 * 收據萃取結果與串流漸進展開畫面 (ReceiptDetailScreen)。
 * 呈現擬真收據紙質感卡片，並提供懸浮專屬 AI 顧問諮詢入口。
 */
@Composable
fun ReceiptDetailScreen(
    record: ReceiptScannerRecord,
    isLiveSession: Boolean = false,
    viewModel: ReceiptScannerViewModel? = null,
    modifier: Modifier = Modifier
) {
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

    val context = LocalContext.current
    val app = context.applicationContext as LensWiseApplication
    val chatAgent = remember {
        ReceiptChatAgent(
            provider = AiProviderFactory.createDefaultProvider(),
            logger = app.apiLogManager
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReceiptPaperCard(
                record = currentRecord,
                isStreaming = isStreaming
            )

            // 預留底部 FAB 空間
            Spacer(modifier = Modifier.height(100.dp))
        }

        // 底部懸浮操作列
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { showChatSheet = true },
                containerColor = AppColors.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI 顧問")
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(text = "問 AI 顧問", color = Color.White)
                }
            }
        }

        if (showChatSheet) {
            ReceiptChatBottomSheet(
                record = currentRecord,
                chatAgent = chatAgent,
                onDismissRequest = { showChatSheet = false }
            )
        }
    }
}
