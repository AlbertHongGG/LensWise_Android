package com.lenswise.app.features.menu.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.features.menu.agent.MenuChatPrompts
import com.lenswise.app.features.menu.presentation.components.MenuHeaderSection
import com.lenswise.app.features.menu.presentation.components.MenuListSection
import com.lenswise.app.presentation.components.chat.AiChatBottomSheet

/**
 * 菜單掃描結果與品項清單畫面 (MenuResultScreen)。
 * 1:1 對齊 LensWise Flutter 版 MenuResultPage：
 * - 沉浸式滾動大白/暗灰質樸菜單卡片 (32.dp 圓角、陰影深度)
 * - 襯線字體餐廳名稱 (MenuHeaderSection)
 * - 引言風格品項與串流中繼骨架屏 (MenuListSection)
 * - 底部圓角方塊按鈕 (Squircle Button) 與全功能 AI 美食點餐顧問抽屜。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuResultScreen(
    record: MenuScannerRecord,
    viewModel: MenuScannerViewModel? = null,
    isLiveSession: Boolean = false,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val state = viewModel?.uiState?.collectAsState()?.value
    val isStreaming = isLiveSession && (state?.status == MenuScannerStatus.STREAMING)

    val effectiveRecord = if (isLiveSession && state != null && state.record.items.isNotEmpty()) {
        state.record
    } else {
        record
    }

    var showChatSheet by remember { mutableStateOf(false) }

    DisposableEffect(isLiveSession) {
        onDispose {
            if (isLiveSession) {
                viewModel?.reset()
            }
        }
    }

    val bgColor = if (isDark) Color.Black else Color(0xFFF8F9FA)
    val cardColor = if (isDark) Color(0xFF1C1C1E) else Color.White

    Scaffold(
        containerColor = bgColor,
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()

            // 1. 全板塊滑動大卡片
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(cardColor)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        MenuHeaderSection(record = effectiveRecord)
                        MenuListSection(
                            record = effectiveRecord,
                            isStreaming = isStreaming
                        )
                    }
                }
            }

            // 2. 底部懸浮 Squircle 按鈕 (56x56 圓角方塊按鈕)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                val buttonBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFF1F2937)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = buttonBg.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(buttonBg)
                        .clickable { showChatSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI 美食顧問對話",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. AI 深度對話顧問抽屜
            if (showChatSheet) {
                val chatAgent = viewModel?.createChatAgent()
                AiChatBottomSheet(
                    title = "${effectiveRecord.storeName.ifBlank { "菜單" }} · 美食顧問",
                    initialMessage = MenuChatPrompts.getInitialMessage(),
                    systemContextPrompt = MenuChatPrompts.buildSystemPrompt(effectiveRecord),
                    onSendMessage = { request ->
                        chatAgent?.executeStream(request)
                            ?: kotlinx.coroutines.flow.flowOf("AI 菜單顧問暫未就緒，請稍後重試。")
                    },
                    onDismissRequest = { showChatSheet = false }
                )
            }
        }
    }
}

