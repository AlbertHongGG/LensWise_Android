package com.lenswise.app.features.comparison.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.presentation.components.StreamingSkeleton

/**
 * 商品比對整合儀表板矩陣 (ComparisonUnifiedBoard)。
 * 1:1 對齊 LensWise Flutter 版：
 * - 嵌入於 Hero VS 區塊下方 24dp (頂部預留 40dp 呼吸空間)
 * - 串流中繼骨架屏 (無資料時 3 行 44dp 骨架、增量串流時尾端追加 44dp 骨架)
 * - 中央對比維度膠囊 (Aspect Pill) 與左右欄原子特徵對比矩陣。
 */
@Composable
fun ComparisonUnifiedBoard(
    record: ProductComparisonRecord,
    leftIndex: Int,
    rightIndex: Int,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val points = record.comparisonPoints

    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    if (points.isEmpty()) {
        if (!isStreaming) return

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(cardColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(top = 40.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 44.dp, isDark = isDark)
                Spacer(modifier = Modifier.height(16.dp))
                StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 44.dp, isDark = isDark)
                Spacer(modifier = Modifier.height(16.dp))
                StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 44.dp, isDark = isDark)
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.4f else 0.05f)
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(cardColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(top = 40.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                val leftValue = if (leftIndex < point.values.size) point.values[leftIndex] else "N/A"
                val rightValue = if (rightIndex < point.values.size) point.values[rightIndex] else "N/A"

                ComparisonMatrixRow(
                    aspect = point.aspect,
                    leftValue = leftValue,
                    rightValue = rightValue,
                    isDark = isDark
                )

                if (index < points.size - 1 || isStreaming) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    )
                }
            }

            if (isStreaming) {
                StreamingSkeleton(
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun ComparisonMatrixRow(
    aspect: String,
    leftValue: String,
    rightValue: String,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 中央對比維度膠囊
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) Color(0xFF2563EB).copy(alpha = 0.2f) else Color(0xFFEFF6FF)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFFBFDBFE),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = aspect,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 左右分割特徵數值
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = leftValue,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                ),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )

            Text(
                text = rightValue,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
