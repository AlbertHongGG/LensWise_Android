package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 發票單一品項列 (ReceiptItemRow)。
 * 1:1 對齊 Flutter 的 receipt_item_row.dart：
 * - 左側：翻譯名稱（16sp Bold Black）+ 原始名稱（14sp Black87）。
 * - 右側：純數字金額（16sp Bold Black，品項行不帶貨幣符號）。
 * - 點擊觸發品項原地編輯彈窗。
 */
@Composable
fun ReceiptItemRow(
    item: ReceiptItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            if (item.translatedName.isNotBlank()) {
                Text(
                    text = item.translatedName,
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 22.sp
                )
            }
            if (item.originalName.isNotBlank()) {
                if (item.translatedName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = item.originalName,
                    fontFamily = OutfitFontFamily,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.87f),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "%.2f".format(item.price),
            fontFamily = OutfitFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.End
        )
    }
}
