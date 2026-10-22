package com.lenswise.app.features.receipt.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lenswise.app.core.image.SessionImageResolver
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.receipt.agent.ReceiptChatPrompts
import com.lenswise.app.features.receipt.presentation.components.ReceiptImageSheet
import com.lenswise.app.features.receipt.presentation.components.ReceiptPaperCard
import com.lenswise.app.presentation.components.chat.AiChatBottomSheet
import java.io.File

/**
 * 收據萃取結果與串流漸進展開畫面 (ReceiptDetailScreen)。
 * 1:1 對齊 Flutter 的 receipt_result_page.dart 與實機截圖設計：
 * 1. 沉浸式無頂部導航列佈局，背景色為淡灰底 (#F5F5F5)。
 * 2. 發票卡片邊界留白 (horizontal = 16dp, vertical = 8dp)。
 * 3. 底部懸浮雙圓形暗色按鈕組（相片檢視 + AI 顧問）。
 * 4. 點擊相片開啟收據原圖輪播檢視抽屜 (ReceiptImageSheet)。
 * 5. 點擊 AI 顧問開啟對話抽屜 (ReceiptChatBottomSheet)。
 * 6. 原地修改發票內容雙向綁定至 ViewModel 與本地歷史倉儲。
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
    var showImageSheet by remember { mutableStateOf(false) }

    val liveState by viewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(ReceiptScannerUiState(receiptRecord = record)) }

    var localRecord by remember(record) { mutableStateOf(record) }

    val currentRecord = if (isLiveSession) {
        if (liveState.receiptRecord.storeName.isNotEmpty() || liveState.receiptRecord.items.isNotEmpty()) {
            liveState.receiptRecord
        } else {
            localRecord
        }
    } else {
        localRecord
    }

    val isStreaming = isLiveSession && liveState.status == ReceiptScannerStatus.STREAMING

    // 解析相片實體檔案（透過 SessionImageResolver 雙軌統一抽象）
    val imageFiles = remember(liveState.capturedImages, currentRecord.imagePaths) {
        SessionImageResolver(
            memoryImages = liveState.capturedImages,
            imagePaths = currentRecord.imagePaths
        ).getAllFiles()
    }

    // 攔截系統返回鍵
    BackHandler(enabled = true) {
        when {
            showImageSheet -> showImageSheet = false
            showChatSheet -> showChatSheet = false
            else -> onNavigateBack?.invoke()
        }
    }

    val backgroundColor = if (isDark) Color(0xFF0F111A) else Color(0xFFF5F5F5)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // 主要卡片滾動區 (1:1 對齊 Flutter 的 SingleChildScrollView)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReceiptPaperCard(
                record = currentRecord,
                isStreaming = isStreaming,
                onUpdate = { updated ->
                    localRecord = updated
                    viewModel?.updateAndPersistRecord(updated)
                }
            )

            // 預留底部 FAB 與系統導航列安全空間 (140.dp)
            Spacer(modifier = Modifier.height(140.dp))
        }

        // 底部懸浮雙圓形按鈕組 (1:1 對齊 Flutter 的 FloatingActionButton Row)
        // 嚴格配置 navigationBarsPadding 與 28.dp 留白，確保絕不誤觸 Android 系統手勢與導航按鍵
        val isDrawerOpen = showImageSheet || showChatSheet
        val fabAlpha by animateFloatAsState(
            targetValue = if (isDrawerOpen) 0f else 1f,
            animationSpec = tween(durationMillis = 200),
            label = "fab_alpha"
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(fabAlpha)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageFiles.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showImageSheet = true },
                        containerColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.87f),
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "查看發票照片",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                FloatingActionButton(
                    onClick = {
                        showChatSheet = true
                    },
                    containerColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.87f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "問 AI 顧問",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 相片檢視抽屜
        if (showImageSheet) {
            ReceiptImageSheet(
                imageFiles = imageFiles,
                onDismissRequest = { showImageSheet = false }
            )
        }

        // AI 顧問問答抽屜
        if (showChatSheet) {
            val chatAgent = viewModel?.createChatAgent()
            AiChatBottomSheet(
                title = "發票 · 消費顧問對話",
                initialMessage = ReceiptChatPrompts.getInitialMessage(),
                systemContextPrompt = ReceiptChatPrompts.buildSystemPrompt(currentRecord),
                onSendMessage = { request ->
                    chatAgent?.executeStream(request)
                        ?: kotlinx.coroutines.flow.flowOf("AI 顧問暫未就緒，請稍後重試。")
                },
                onDismissRequest = { showChatSheet = false }
            )
        }
    }
}

