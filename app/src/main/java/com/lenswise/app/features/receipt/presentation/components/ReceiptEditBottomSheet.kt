package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.presentation.components.sheet.LensWiseFixedSheet
import com.lenswise.app.presentation.components.sheet.LensWiseSheetHandle
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

sealed interface ReceiptEditTarget {
    data class StoreName(val current: String) : ReceiptEditTarget
    data class Item(val index: Int, val item: ReceiptItem) : ReceiptEditTarget
    data class TotalAmount(val current: Double) : ReceiptEditTarget
}

/**
 * 發票沉浸式原地編輯底部工作表 (ReceiptEditBottomSheet)。
 * 採用 LensWise 統一固定高度面板架構 (Type A: LensWiseFixedSheet)：
 * 1. 緊湊自適應高度 wrapContentHeight。
 * 2. 下拉把手具跟手位移，放手未超額彈回，超額則平滑關閉。
 * 3. 1:1 對齊 Flutter 的 _showImmersiveBottomSheet 樣式與編輯行為。
 */
@Composable
fun ReceiptEditBottomSheet(
    target: ReceiptEditTarget,
    onDismissRequest: () -> Unit,
    onSaveStoreName: (String) -> Unit,
    onSaveItem: (Int, ReceiptItem) -> Unit,
    onSaveTotalAmount: (Double) -> Unit
) {
    LensWiseFixedSheet(
        onDismissRequest = onDismissRequest,
        isWrapContent = true,
        hasScrim = true,
        containerColor = Color.White,
        headerContent = { dragModifier ->
            Box(modifier = dragModifier.fillMaxWidth()) {
                LensWiseSheetHandle(isDark = false)
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .imePadding()
            ) {
                when (target) {
                    is ReceiptEditTarget.StoreName -> {
                        var text by remember { mutableStateOf(target.current) }
                        Text(
                            text = "編輯商店名稱",
                            fontFamily = OutfitFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.04f),
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onSaveStoreName(text.trim())
                                    onDismissRequest()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                        ) {
                            Text(
                                text = "儲存變更",
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    is ReceiptEditTarget.Item -> {
                        var originalName by remember { mutableStateOf(target.item.originalName) }
                        var translatedName by remember { mutableStateOf(target.item.translatedName) }
                        var priceText by remember { mutableStateOf(target.item.price.toString()) }

                        Text(
                            text = "編輯品項",
                            fontFamily = OutfitFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = originalName,
                            onValueChange = { originalName = it },
                            label = { Text("原始品名") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = translatedName,
                            onValueChange = { translatedName = it },
                            label = { Text("翻譯品名") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("金額") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val priceVal = priceText.toDoubleOrNull() ?: target.item.price
                                onSaveItem(
                                    target.index,
                                    target.item.copy(
                                        originalName = originalName.trim(),
                                        translatedName = translatedName.trim(),
                                        price = priceVal
                                    )
                                )
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                        ) {
                            Text(
                                text = "儲存變更",
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    is ReceiptEditTarget.TotalAmount -> {
                        var amountText by remember { mutableStateOf("%.2f".format(target.current)) }

                        Text(
                            text = "編輯總計",
                            fontFamily = OutfitFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.04f),
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val parsed = amountText.toDoubleOrNull()
                                if (parsed != null) {
                                    onSaveTotalAmount(parsed)
                                    onDismissRequest()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                        ) {
                            Text(
                                text = "儲存變更",
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    )
}
