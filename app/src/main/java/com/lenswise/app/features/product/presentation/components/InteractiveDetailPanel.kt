package com.lenswise.app.features.product.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.presentation.components.GlassActionButton
import com.lenswise.app.presentation.theme.AppColors

/**
 * 商品右上方浮動操作互動面板 (InteractiveDetailPanel)。
 * 1:1 對齊 LensWise Flutter 版與圖一：
 * 水平並排 3 顆極致毛玻璃按鈕（成分與過敏原、適合對象分析、AI 深度對話）。
 * 支援細粒度 derivedStateOf 響應式隔離與 Haze 即時高斯模糊。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveDetailPanel(
    recordProvider: () -> ProductLensRecord,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    var showIngredientsSheet by remember { mutableStateOf(false) }
    var showProsConsSheet by remember { mutableStateOf(false) }

    val hasIngredients by remember {
        derivedStateOf {
            val r = recordProvider()
            r.ingredients.isNotEmpty() || r.allergenAlerts.isNotEmpty()
        }
    }

    val hasProsCons by remember {
        derivedStateOf {
            val r = recordProvider()
            r.whenRecommended.isNotEmpty() || r.whenNotRecommended.isNotEmpty()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasIngredients) {
            GlassActionButton(
                icon = Icons.Outlined.Science,
                contentDescription = "成分與過敏原",
                onClick = { showIngredientsSheet = true },
                hazeState = hazeState
            )
        }

        if (hasProsCons) {
            GlassActionButton(
                icon = Icons.Outlined.PeopleOutline,
                contentDescription = "適合對象分析",
                onClick = { showProsConsSheet = true },
                hazeState = hazeState
            )
        }

        GlassActionButton(
            icon = Icons.Default.AutoAwesome,
            contentDescription = "AI 深度探討",
            onClick = onOpenChat,
            hazeState = hazeState
        )
    }

    if (showIngredientsSheet) {
        val currentRecord = recordProvider()
        ModalBottomSheet(
            onDismissRequest = { showIngredientsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            DetailSheetContent(
                title = "成分與過敏原",
                icon = Icons.Outlined.Science
            ) {
                IngredientsSheetBody(record = currentRecord)
            }
        }
    }

    if (showProsConsSheet) {
        val currentRecord = recordProvider()
        ModalBottomSheet(
            onDismissRequest = { showProsConsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            DetailSheetContent(
                title = "適合對象分析",
                icon = Icons.Outlined.PeopleOutline
            ) {
                ProsConsSheetBody(record = currentRecord)
            }
        }
    }
}

@Composable
private fun DetailSheetContent(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF111418),
                    letterSpacing = 0.5.sp
                )
            )
        }

        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientsSheetBody(record: ProductLensRecord) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        if (record.allergenAlerts.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                record.allergenAlerts.forEach { alert ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = alert,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (record.ingredients.isNotEmpty()) {
            Text(
                text = "主要成分",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF111418)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.ingredients.joinToString(", "),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF4A5568)
                )
            )
        } else {
            Text(
                text = "無詳細成分標示",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun ProsConsSheetBody(record: ProductLensRecord) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        ProsConsBlock(
            title = "推薦情境",
            icon = Icons.Outlined.CheckCircle,
            color = Color(0xFF10B981),
            items = record.whenRecommended
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProsConsBlock(
            title = "不推薦情境",
            icon = Icons.Outlined.Cancel,
            color = Color(0xFFEF4444),
            items = record.whenNotRecommended
        )
    }
}

@Composable
private fun ProsConsBlock(
    title: String,
    icon: ImageVector,
    color: Color,
    items: List<String>
) {
    val isDark = isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                text = "無特別註明",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            )
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 10.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = item,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF2D3748)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
