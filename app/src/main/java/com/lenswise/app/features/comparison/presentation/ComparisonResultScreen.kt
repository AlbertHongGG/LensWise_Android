package com.lenswise.app.features.comparison.presentation

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.ViewCarousel
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.features.comparison.agent.ComparisonChatPrompts
import com.lenswise.app.features.comparison.presentation.components.AiVerdictBottomSheet
import com.lenswise.app.features.comparison.presentation.components.ComparisonContendersDrawer
import com.lenswise.app.features.comparison.presentation.components.ComparisonHeroSection
import com.lenswise.app.features.comparison.presentation.components.ComparisonRosterBar
import com.lenswise.app.features.comparison.presentation.components.ComparisonUnifiedBoard
import com.lenswise.app.features.comparison.presentation.components.ReplaceContenderBottomSheet
import com.lenswise.app.presentation.components.GlassActionButton
import com.lenswise.app.presentation.components.chat.AiChatBottomSheet
import java.io.File

/**
 * 商品比對結果與矩陣看板畫面 (ComparisonResultScreen)。
 * 1:1 絕對對齊 LensWise Flutter 版 ComparisonResultPage 與圖二：
 * - 徹底移除實體返回按鈕，統一遵循純手勢右滑返回 (CupertinoSwipeBackContainer)
 * - 頂部 Hero VS 雙雄展演看板 (220dp 高度、4dp 微間距、52dp 圓形光學居中 VS 徽章)
 * - 底層嵌入式統合矩陣 (Unified Board, 頂部縮進 196dp 貼合 Hero 後方，呈現 24dp 深度層次)
 * - 底部 Roster Bar 懸浮圓膠囊 (圖二修正：40dp 圓角膠囊、外層無裁切、18dp 完整白邊箭頭角標)
 * - 右上角 3 顆縱向排列毛玻璃 FAB（AI 對話、推薦總結、產品圖鑑）
 * - 產品圖鑑抽屜 (幾何高度精準對齊：剛好露出 2 個選項，上滑展開 85%)
 * - 替換對手彈窗 (wrapContentHeight，並列【替換左側】與【替換右側】2 個選項)
 * - 推薦總結抽屜 (兩段式高度：初始 60%，上滑展開 85%)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonResultScreen(
    record: ProductComparisonRecord,
    viewModel: ProductComparisonViewModel? = null,
    isLiveSession: Boolean = false,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val state = viewModel?.uiState?.collectAsState()?.value
    val isStreaming = isLiveSession && (state?.status == ProductComparisonStatus.STREAMING)

    val effectiveRecord = if (isLiveSession && state != null && state.record.comparisonPoints.isNotEmpty()) {
        state.record
    } else {
        record
    }

    var leftIndex by remember { mutableIntStateOf(0) }
    var rightIndex by remember { mutableIntStateOf(1) }
    var selectedSwapCandidate by remember { mutableIntStateOf(-1) }
    var showVerdictSheet by remember { mutableStateOf(false) }
    var showContendersSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    val totalProductCount = when {
        effectiveRecord.imagePaths.isNotEmpty() -> effectiveRecord.imagePaths.size
        state?.capturedImages?.isNotEmpty() == true -> state.capturedImages.size
        else -> effectiveRecord.products.size
    }

    // 固化圖片來源解析器，串流期間保持穩定
    val getImageModel: (Int) -> Any? = remember(effectiveRecord.id) {
        val paths = effectiveRecord.imagePaths
        val captured = state?.capturedImages ?: viewModel?.uiState?.value?.capturedImages
        val resolver: (Int) -> Any? = { idx ->
            when {
                idx < paths.size -> File(paths[idx])
                captured != null && idx < captured.size -> captured[idx].byteArray
                else -> null
            }
        }
        resolver
    }

    val hazeState = remember { HazeState() }

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
            val scrollState = rememberScrollState()

            // 1. 視口背景捕獲層 (Viewport Framebuffer Layer)
            // 固定等於螢幕尺寸，記錄眼睛所見的實體畫面；
            // 透過 drawWithContent 觀察 scrollState.value，確保在滑動時每一幀即時重新記錄。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        scrollState.value
                        drawContent()
                    }
                    .haze(state = hazeState)
            ) {
                // 主要滑動內容區 (Hero + Board Z-Order 精密堆疊)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 120.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 底層：嵌入式統合儀表板 (Unified Board, 頂部縮進 196dp 貼合 Hero 後方，呈現 24dp 深度層次)
                        Box(modifier = Modifier.padding(top = 196.dp)) {
                            ComparisonUnifiedBoard(
                                record = effectiveRecord,
                                leftIndex = leftIndex,
                                rightIndex = rightIndex,
                                isStreaming = isStreaming
                            )
                        }

                        // 頂層：Hero VS 對決區塊 (穩穩壓在 Board 上方 24dp)
                        val leftProduct = if (leftIndex < effectiveRecord.products.size) effectiveRecord.products[leftIndex] else null
                        val rightProduct = if (rightIndex < effectiveRecord.products.size) effectiveRecord.products[rightIndex] else null

                        ComparisonHeroSection(
                            leftImageModel = getImageModel(leftIndex),
                            rightImageModel = getImageModel(rightIndex),
                            leftTranslatedName = leftProduct?.translatedName.orEmpty(),
                            leftOriginalName = leftProduct?.originalName.orEmpty(),
                            rightTranslatedName = rightProduct?.translatedName.orEmpty(),
                            rightOriginalName = rightProduct?.originalName.orEmpty()
                        )
                    }
                }
            }

            // 2. 右上角浮動按鈕區 (3 顆縱向排列：AI 對話、推薦總結、產品圖鑑)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassActionButton(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "AI 深度探討",
                    onClick = { showChatSheet = true },
                    hazeState = hazeState,
                    scrollState = scrollState
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassActionButton(
                    icon = Icons.Outlined.Summarize,
                    contentDescription = "AI 推薦總結",
                    onClick = { showVerdictSheet = true },
                    hazeState = hazeState,
                    scrollState = scrollState
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassActionButton(
                    icon = Icons.Outlined.ViewCarousel,
                    contentDescription = "產品圖鑑",
                    onClick = { showContendersSheet = true },
                    hazeState = hazeState,
                    scrollState = scrollState
                )
            }

            // 3. 底部席次輪巡頭像切換膠囊 (圖二 1:1 復刻：40dp 圓膠囊、未裁切外凸 18dp 白邊角標)
            ComparisonRosterBar(
                itemCount = totalProductCount,
                leftIndex = leftIndex,
                rightIndex = rightIndex,
                getImageModel = getImageModel,
                onProductTapped = { candidateIdx ->
                    if (candidateIdx != leftIndex && candidateIdx != rightIndex) {
                        selectedSwapCandidate = candidateIdx
                    }
                },
                hazeState = hazeState,
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 4. 產品圖鑑抽屜 (高度精準設計：剛好露出 2 個選項，上滑可展開至 85%)
            if (showContendersSheet) {
                ComparisonContendersDrawer(
                    record = effectiveRecord,
                    leftIndex = leftIndex,
                    rightIndex = rightIndex,
                    getImageModel = getImageModel,
                    onProductTapped = { candidateIdx ->
                        if (candidateIdx != leftIndex && candidateIdx != rightIndex) {
                            showContendersSheet = false
                            selectedSwapCandidate = candidateIdx
                        }
                    },
                    onDismissRequest = { showContendersSheet = false }
                )
            }

            // 5. 替換對手雙選項彈窗 (自適應緊湊高度，剛好顯示【替換左側】與【替換右側】2 個選項)
            if (selectedSwapCandidate >= 0) {
                ReplaceContenderBottomSheet(
                    targetIndex = selectedSwapCandidate,
                    targetImageModel = getImageModel(selectedSwapCandidate),
                    leftImageModel = getImageModel(leftIndex),
                    rightImageModel = getImageModel(rightIndex),
                    onSwapLeft = {
                        leftIndex = selectedSwapCandidate
                        selectedSwapCandidate = -1
                    },
                    onSwapRight = {
                        rightIndex = selectedSwapCandidate
                        selectedSwapCandidate = -1
                    },
                    onDismissRequest = { selectedSwapCandidate = -1 }
                )
            }

            // 6. AI 推薦總結 Markdown 抽屜 (兩段式高度：初始 60%，上滑展開至 85%)
            if (showVerdictSheet) {
                AiVerdictBottomSheet(
                    summary = effectiveRecord.summary,
                    onDismissRequest = { showVerdictSheet = false }
                )
            }

            // 7. AI 深度對話顧問抽屜
            if (showChatSheet) {
                val chatAgent = viewModel?.createChatAgent()
                AiChatBottomSheet(
                    title = "${effectiveRecord.title.ifBlank { "商品比對" }} · 顧問對話",
                    initialMessage = ComparisonChatPrompts.getInitialMessage(),
                    systemContextPrompt = ComparisonChatPrompts.buildSystemPrompt(effectiveRecord),
                    onSendMessage = { request ->
                        chatAgent?.executeStream(request)
                            ?: kotlinx.coroutines.flow.flowOf("AI 比對顧問暫未就緒，請稍後重試。")
                    },
                    onDismissRequest = { showChatSheet = false }
                )
            }
        }
    }
}

