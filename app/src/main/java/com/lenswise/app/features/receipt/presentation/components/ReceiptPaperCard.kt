package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.presentation.components.StreamingSkeleton
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 擬真熱感應發票紙質感卡片 (ReceiptPaperCard)。
 *
 * 1:1 對齊 Flutter 的 receipt_paper_card.dart 與實機截圖排版：
 * 1. 骨架屏載入過渡：串流中尚未獲取的欄位全部由 StreamingSkeleton 呼吸微光塊承載，嚴禁文字佔位。
 * 2. 排版結構：
 *    - 商店名稱 (24sp Bold) / 日期 (14sp Gray)
 *    - 1dp 細分隔線
 *    - 表頭「品項 / 金額」(14sp Bold Black54)
 *    - 品項清單 (ReceiptItemRow：中文品名粗體 16sp + 日文原名 14sp + 右側純數字金額 16sp)
 *    - 1dp 細分隔線
 *    - 小計 (16sp)
 *    - 折扣列表 (鮮紅字 14sp，金額帶負號)
 *    - 稅金列表 (灰色 14sp)
 *    - 2dp 純黑粗分隔線
 *    - 總計 ($currency $totalAmount，24sp Black 900)
 * 3. 支援點擊商店名稱、品項列、總計金額喚起 ReceiptEditBottomSheet 進行原地編輯。
 */
@Composable
fun ReceiptPaperCard(
    record: ReceiptScannerRecord,
    isStreaming: Boolean,
    onUpdate: (ReceiptScannerRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingTarget by remember { mutableStateOf<ReceiptEditTarget?>(null) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 商店名稱
            if (isStreaming && record.storeName.isEmpty()) {
                StreamingSkeleton(
                    width = 140.dp,
                    height = 28.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    text = record.storeName.ifBlank { "未知商店" },
                    fontFamily = OutfitFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { editingTarget = ReceiptEditTarget.StoreName(record.storeName) }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 收據日期
            if (isStreaming && (record.receiptDate == null || record.receiptDate.isEmpty())) {
                StreamingSkeleton(width = 100.dp, height = 16.dp)
            } else {
                Text(
                    text = record.receiptDate ?: "",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // 分隔線
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color.Black.copy(alpha = 0.08f)
            )

            // 表頭欄位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "品項",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.54f)
                )
                Text(
                    text = "金額",
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.54f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 品項清單
            if (record.items.isEmpty()) {
                if (isStreaming) {
                    Column {
                        StreamingSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            height = 26.dp
                        )
                        StreamingSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            height = 26.dp
                        )
                        StreamingSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            height = 26.dp
                        )
                    }
                }
            } else {
                Column {
                    record.items.forEachIndexed { index, item ->
                        ReceiptItemRow(
                            item = item,
                            onClick = { editingTarget = ReceiptEditTarget.Item(index, item) }
                        )
                    }
                    if (isStreaming) {
                        StreamingSkeleton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            height = 26.dp
                        )
                    }
                }
            }

            // 分隔線
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color.Black.copy(alpha = 0.08f)
            )

            // 小計
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "小計",
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.87f)
                )
                if (isStreaming && record.subtotal <= 0.0) {
                    StreamingSkeleton(width = 60.dp, height = 18.dp)
                } else {
                    Text(
                        text = "%.2f".format(record.subtotal),
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        color = Color.Black.copy(alpha = 0.87f)
                    )
                }
            }

            // 折扣列表
            if (record.discounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                record.discounts.forEach { discount ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = discount.name,
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val formattedDiscount = if (discount.amount > 0) "-%.2f".format(discount.amount) else "%.2f".format(discount.amount)
                        Text(
                            text = formattedDiscount,
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 稅金列表
            if (record.taxes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                record.taxes.forEach { tax ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = tax.name,
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            color = Color.Black.copy(alpha = 0.54f),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "%.2f".format(tax.amount),
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            color = Color.Black.copy(alpha = 0.54f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 總計上方粗黑線 (2dp thickness)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 2.dp,
                color = Color.Black
            )

            // 總計金額
            if (isStreaming && record.totalAmount <= 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "總計",
                        fontFamily = OutfitFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    StreamingSkeleton(width = 90.dp, height = 28.dp)
                }
            } else {
                val currencyPrefix = if (!record.currency.isNullOrBlank()) "${record.currency} " else ""
                Text(
                    text = "$currencyPrefix${"%.2f".format(record.totalAmount)}",
                    fontFamily = OutfitFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            editingTarget = ReceiptEditTarget.TotalAmount(record.totalAmount)
                        }
                    )
                )
            }
        }
    }

    // 原地編輯工作表
    val activeTarget = editingTarget
    if (activeTarget != null) {
        ReceiptEditBottomSheet(
            target = activeTarget,
            onDismissRequest = { editingTarget = null },
            onSaveStoreName = { newName ->
                onUpdate(record.copy(storeName = newName))
            },
            onSaveItem = { index, newItem ->
                val newItems = record.items.toMutableList()
                if (index in newItems.indices) {
                    newItems[index] = newItem
                    onUpdate(record.copy(items = newItems))
                }
            },
            onSaveTotalAmount = { newTotal ->
                onUpdate(record.copy(totalAmount = newTotal))
            }
        )
    }
}
