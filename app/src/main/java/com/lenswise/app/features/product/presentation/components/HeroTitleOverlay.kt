package com.lenswise.app.features.product.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.presentation.components.StreamingSkeleton
import com.lenswise.app.presentation.theme.AppColors

/**
 * 商品深度分析主標題覆蓋區塊 (HeroTitleOverlay)。
 * 1:1 對齊 LensWise Flutter 版：
 * 串流骨架屏 (110x24 膠囊、240x34 標題、150x18 副標)、
 * 品牌產地高質感膠囊 Tag、雙語品名與自適應文字縮放。
 */
@Composable
fun HeroTitleOverlay(
    record: ProductLensRecord,
    isLiveSession: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val productName = record.productName
    val originalName = record.originalName
    val brand = record.brand
    val origin = record.origin

    if (productName.isBlank()) {
        if (!isLiveSession) return
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            StreamingSkeleton(
                width = 110.dp,
                height = 24.dp,
                shape = RoundedCornerShape(20.dp),
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(14.dp))
            StreamingSkeleton(
                width = 240.dp,
                height = 34.dp,
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            StreamingSkeleton(
                width = 150.dp,
                height = 18.dp,
                isDark = isDark
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 品牌與產地 Tag
        if (brand.isNotBlank() || origin.isNotBlank()) {
            val tagText = buildString {
                append(brand.ifBlank { "未知品牌" })
                if (origin.isNotBlank()) {
                    append(" · ")
                    append(origin)
                }
            }.uppercase()

            Text(
                text = tagText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = AppColors.primary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.primary.copy(alpha = 0.95f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // 翻譯商品名稱 (Translated Product Name)
        Text(
            text = productName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF111418),
            letterSpacing = (-0.5).sp
        )

        // 原文商品名稱 (Original Name)
        if (originalName.isNotBlank() && originalName != productName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = originalName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = if (isDark) Color(0xFFB0B0B0) else Color(0xFF6B7280),
                letterSpacing = 0.2.sp
            )
        }
    }
}
