package com.lenswise.app.features.scanner.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lenswise.app.features.scanner.ScannerViewModel
import com.lenswise.app.features.scanner.viewer.components.ViewerThumbnailFilmstrip
import com.lenswise.app.features.scanner.viewer.components.ViewerTopBar
import com.lenswise.app.features.scanner.viewer.components.ZoomableAsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 徹底重構之高質感照片檢視器 (CapturedImageViewerScreen)。
 *
 * 核心保證：
 * 1. 唯一禁用右滑返回介面：所有橫向滑動事件完整留給大圖翻頁與縮圖列表。
 * 2. 底部自由水平滑動縮圖膠捲列 (Filmstrip Carousel)：支援飛速滑動與點擊跳轉，空間恆定無推擠。
 * 3. 雙向即時連動：滑動大圖時縮圖列自動捲動置中；點擊縮圖大圖流暢切換。
 * 4. 支援雙指縮放 (Pinch-to-zoom)、雙擊放大與單擊切換沉浸純淨模式。
 * 5. 權責切分純粹：專注於照片預覽與管理，無多餘分析按鈕干擾。
 */
@Composable
fun CapturedImageViewerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val images = uiState.capturedImages

    // 若全部照片被刪除，自動優雅退回相機畫面
    LaunchedEffect(images.isEmpty()) {
        if (images.isEmpty()) {
            onNavigateBack()
        }
    }

    if (images.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = (images.size - 1).coerceAtLeast(0),
        pageCount = { images.size }
    )
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    var isControlsVisible by remember { mutableStateOf(true) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    // 當大圖切換時，自動將底部縮圖列平滑捲動至當前選中項並水平置中
    LaunchedEffect(pagerState.currentPage) {
        if (images.isNotEmpty() && pagerState.currentPage in images.indices) {
            val centerOffsetPx = with(density) {
                -((screenWidth / 2) - 26.dp).toPx().roundToInt()
            }
            lazyListState.animateScrollToItem(pagerState.currentPage, scrollOffset = centerOffsetPx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 全螢幕照片輪播大圖視口 (支援雙指/雙擊縮放，縮放時鎖定 Pager，1.0x 時順暢換頁)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val entity = images.getOrNull(pageIndex)
            if (entity != null) {
                ZoomableAsyncImage(
                    model = entity.file,
                    contentDescription = "照片大圖 ${pageIndex + 1}",
                    isSelected = pageIndex == pagerState.currentPage,
                    onIsZoomedChanged = { isZoomed ->
                        if (pageIndex == pagerState.currentPage) {
                            isCurrentPageZoomed = isZoomed
                        }
                    },
                    onToggleControls = {
                        isControlsVisible = !isControlsVisible
                    }
                )
            }
        }

        // 2. 頂部沉浸毛玻璃操作列 (返回相機、頁碼膠囊、單張刪除)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopBar(
                currentIndex = pagerState.currentPage,
                totalCount = images.size,
                onNavigateBack = onNavigateBack,
                onDeleteCurrent = {
                    viewModel.removeImage(pagerState.currentPage)
                }
            )
        }

        // 3. 底部水平滑動縮圖膠捲列 (支援飛速左右滑動瀏覽與點擊直達跳轉)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ViewerThumbnailFilmstrip(
                images = images,
                selectedIndex = pagerState.currentPage,
                lazyListState = lazyListState,
                onSelectImage = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }
}

