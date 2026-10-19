package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 擬真熱感應發票紙質感卡片 (ReceiptPaperCard)。
 * 支援品項隨串流漸進式滑入與金額動態響應。
 */
@Composable
fun ReceiptPaperCard(
    record: ReceiptScannerRecord,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 商店名稱
            val storeTitle = if (record.storeName.isNotBlank()) {
                record.storeName
            } else if (isStreaming) {
                "辨識商店中..."
            } else {
                "未知商店"
            }

            Text(
                text = storeTitle,
                fontFamily = OutfitFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 日期時間
            val displayDate = record.receiptDate ?: if (isStreaming) "取得日期中..." else "無日期資訊"
            Text(
                text = displayDate,
                fontFamily = OutfitFontFamily,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 標題列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "品項",
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f)
                )
                Text(
                    text = "金額",
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 品項清單 (逐筆展開)
            if (record.items.isEmpty()) {
                if (isStreaming) {
                    Text(
                        text = "正在為您即時解析品項...",
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                record.items.forEachIndexed { index, item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        ReceiptItemRow(item = item, currency = record.currency ?: "$")
                    }
                }
            }

            if (isStreaming && record.items.isNotEmpty()) {
                Text(
                    text = "• • • 正在載入後續品項",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = AppColors.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 小計
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "小計", fontFamily = OutfitFontFamily, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.8f))
                Text(
                    text = "${record.currency ?: "$"} ${"%.2f".format(record.subtotal)}",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            // 折扣
            if (record.discounts.isNotEmpty()) {
                record.discounts.forEach { discount ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = discount.name, fontFamily = OutfitFontFamily, fontSize = 13.sp, color = Color(0xFFD32F2F))
                        Text(
                            text = "${record.currency ?: "$"} ${"%.2f".format(discount.amount)}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // 稅金
            if (record.taxes.isNotEmpty()) {
                record.taxes.forEach { tax ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = tax.name, fontFamily = OutfitFontFamily, fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = "${record.currency ?: "$"} ${"%.2f".format(tax.amount)}",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.Black, thickness = 2.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 總計
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "總計",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "${record.currency ?: "$"} ${"%.2f".format(record.totalAmount)}",
                    fontFamily = OutfitFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ReceiptItemRow(item: ReceiptItem, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.translatedName.ifBlank { item.originalName },
                fontFamily = OutfitFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            if (item.originalName.isNotBlank() && item.originalName != item.translatedName) {
                Text(
                    text = item.originalName,
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = "$currency ${"%.2f".format(item.price)}",
            fontFamily = OutfitFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            textAlign = TextAlign.End
        )
    }
}
