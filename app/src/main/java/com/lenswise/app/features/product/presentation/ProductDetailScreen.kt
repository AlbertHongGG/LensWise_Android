package com.lenswise.app.features.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.lenswise.app.core.image.SessionImageResolver
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.features.product.presentation.components.ImmersiveBackground
import com.lenswise.app.features.product.agent.ProductChatPrompts
import com.lenswise.app.features.product.presentation.components.HeroTitleOverlay
import com.lenswise.app.features.product.presentation.components.InteractiveDetailPanel
import com.lenswise.app.features.product.presentation.components.VerdictFloatingCard
import com.lenswise.app.presentation.components.chat.AiChatBottomSheet
import java.io.File

/**
 * 商品深度分析結果與詳情畫面 (ProductDetailScreen)。
 * 1:1 對齊 LensWise Flutter 版 ProductDetailPage 與圖一：
 * - 沉浸式頂部商品照片背景（支援本地實體檔案或記憶體照片，0 重組零閃爍）
 * - 無實體返回鍵，統一純手勢右滑返回 (CupertinoSwipeBackContainer)
 * - 42% 高度交疊之底層圓角卡片 (32.dp 圓角、雙層立體陰影)
 * - 右上方 InteractiveDetailPanel 浮空水平毛玻璃按鈕群 (圖一 1:1 復刻，支援 Haze 即時高斯模糊)
 * - 全功能專屬 AI 顧問對話抽屜。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    record: ProductLensRecord,
    viewModel: ProductLensViewModel? = null,
    isLiveSession: Boolean = false,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val liveState by viewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(ProductLensUiState(record = record)) }
    var localRecord by remember(record) { mutableStateOf(record) }

    val effectiveRecord = if (isLiveSession) {
        if (liveState.record.productName.isNotBlank() || liveState.record.summary.isNotBlank()) {
            liveState.record
        } else {
            localRecord
        }
    } else {
        localRecord
    }

    // 雙軌圖片來源解析器，以 File 原生類型供 Coil 毫秒級載入
    val capturedImages = liveState.capturedImages
    val imageResolver = remember(effectiveRecord.imagePaths, capturedImages) {
        SessionImageResolver(
            memoryImages = capturedImages,
            imagePaths = effectiveRecord.imagePaths
        )
    }

    val hazeState = remember { HazeState() }
    var showChatSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    DisposableEffect(isLiveSession) {
        onDispose {
            if (isLiveSession) {
                viewModel?.reset()
            }
        }
    }

    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)

    Scaffold(
        containerColor = bgColor,
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 頂部 48% 高度沉浸式商品多圖輪播展演區塊 (1:1 對齊 Flutter 版 ImmersiveBackground)
            val topImageHeight = screenHeight * 0.48f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topImageHeight)
                    .haze(state = hazeState)
            ) {
                ImmersiveBackground(
                    resolver = imageResolver,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. 底部 42% 高度開始交疊的資訊卡片 (32dp 頂部圓角、立體雙層陰影)
            val cardTopOffset = screenHeight * 0.42f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = cardTopOffset)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.12f)
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(bgColor)
                    .padding(top = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 48.dp)
                ) {
                    HeroTitleOverlay(
                        record = effectiveRecord,
                        isLiveSession = isLiveSession
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        VerdictFloatingCard(
                            summary = effectiveRecord.summary,
                            isLiveSession = isLiveSession
                        )
                    }
                }
            }

            // 3. 右上角浮動按鈕群 (InteractiveDetailPanel, 對齊圖一：水平 3 顆毛玻璃 FAB，無返回鍵)
            InteractiveDetailPanel(
                record = effectiveRecord,
                onOpenChat = { showChatSheet = true },
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 16.dp)
            )

            // 4. AI 深度對話顧問抽屜
            if (showChatSheet) {
                val chatAgent = viewModel?.createChatAgent()
                AiChatBottomSheet(
                    title = "商品顧問",
                    initialMessage = ProductChatPrompts.getInitialMessage(),
                    initialMessages = effectiveRecord.chatHistory,
                    systemContextPrompt = ProductChatPrompts.buildSystemContextPrompt(effectiveRecord),
                    onSendMessage = { request ->
                        chatAgent?.executeStream(request)
                            ?: kotlinx.coroutines.flow.flowOf("AI 顧問暫未就緒，請稍後重試。")
                    },
                    onMessagesUpdated = { updatedMessages ->
                        val updated = effectiveRecord.copy(chatHistory = updatedMessages)
                        localRecord = updated
                        viewModel?.updateRecord(updated)
                    },
                    onDismissRequest = { showChatSheet = false }
                )
            }
        }
    }
}

