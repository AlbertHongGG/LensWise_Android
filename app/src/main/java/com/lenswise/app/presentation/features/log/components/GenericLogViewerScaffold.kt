package com.lenswise.app.presentation.features.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.log.DeletableLog
import com.lenswise.app.presentation.components.SwipeToConfirmDrawer
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T : DeletableLog> GenericLogViewerScaffold(
    items: List<T>,
    onDeleteSingle: suspend (T) -> Unit,
    onClearAll: suspend () -> Unit,
    clearDrawerTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    itemContent: @Composable (item: T, isDragging: Boolean, modifier: Modifier) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val dragDropState = rememberDragDropTrashState<T>()
    var showClearDrawer by remember { mutableStateOf(false) }

    if (showClearDrawer) {
        SwipeToConfirmDrawer(
            title = clearDrawerTitle,
            onConfirm = {
                scope.launch {
                    onClearAll()
                    showClearDrawer = false
                }
            },
            onDismiss = { showClearDrawer = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        DragDropTrashContainer(
            state = dragDropState,
            onDropOnTrash = { item ->
                scope.launch {
                    onDeleteSingle(item)
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
            ) {
                header()

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
                            style = AppTypography.itemValue.copy(
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    }
                } else {
                    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = navBottom + 112.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = items,
                            key = { it.id }
                        ) { item ->
                            val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == item.id
                            itemContent(
                                item,
                                isBeingDragged,
                                Modifier.draggableToTrash(
                                    item = item,
                                    state = dragDropState,
                                    feedback = {
                                        itemContent(item, false, Modifier)
                                    }
                                )
                            )
                        }
                    }
                }
            }

            GlassmorphicTrashControl(
                onClick = { showClearDrawer = true },
                hazeState = hazeState,
                isHovering = dragDropState.isHoveringTrash,
                bottomSafetyPadding = 28.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { coordinates ->
                        dragDropState.trashBoundsInWindow = coordinates.boundsInWindow()
                    }
            )
        }
    }
}
