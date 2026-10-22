package com.lenswise.app.features.comparison.presentation.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lenswise.app.presentation.theme.AppColors

/**
 * 替換對手雙選項彈出視窗 (ReplaceContenderBottomSheet)。
 * 1:1 對齊 LensWise Flutter 版 _onAvatarTapped：
 * - 緊湊高度 wrapContentHeight
 * - 頂部目標候選人 64x64 圓形頭像與「請選擇要替換的對手」標題
 * - 並列顯示【替換左側】與【替換右側】2 個選項卡片 (120dp 高度、暗化預覽圖、雙向箭頭)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceContenderBottomSheet(
    targetIndex: Int,
    targetImageModel: Any?,
    leftImageModel: Any?,
    rightImageModel: Any?,
    onSwapLeft: () -> Unit,
    onSwapRight: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 目標替換候選人頭像 (64x64 圓形、2dp Primary 邊框與光暈)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = AppColors.primary.copy(alpha = 0.35f)
                    )
                    .clip(CircleShape)
                    .border(width = 2.dp, color = AppColors.primary, shape = CircleShape)
            ) {
                AsyncImage(
                    model = targetImageModel,
                    contentDescription = "候選商品",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 標題
            Text(
                text = "請選擇要替換的對手",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF111418),
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. 水平雙選項卡片 (剛好顯示出兩個選項：替換左側 & 替換右側)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReplaceOptionButton(
                    label = "替換左側",
                    icon = Icons.Default.KeyboardDoubleArrowLeft,
                    currentImageModel = leftImageModel,
                    onClick = onSwapLeft,
                    modifier = Modifier.weight(1f)
                )

                ReplaceOptionButton(
                    label = "替換右側",
                    icon = Icons.Default.KeyboardDoubleArrowRight,
                    currentImageModel = rightImageModel,
                    onClick = onSwapRight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReplaceOptionButton(
    label: String,
    icon: ImageVector,
    currentImageModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                color = AppColors.primary.copy(alpha = 0.30f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 底圖與暗化遮罩
        AsyncImage(
            model = currentImageModel,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
        )

        // 居中內容：圖示與文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}
