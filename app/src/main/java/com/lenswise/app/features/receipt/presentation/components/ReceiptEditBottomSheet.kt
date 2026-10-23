package com.lenswise.app.features.receipt.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.presentation.components.sheet.LensWiseFixedSheet
import com.lenswise.app.presentation.components.sheet.LensWiseSheetHandle
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * 原地編輯目標類型 (ReceiptEditTarget)。
 */
sealed interface ReceiptEditTarget {
    data class StoreName(val current: String) : ReceiptEditTarget
    data class Item(val index: Int, val item: ReceiptItem) : ReceiptEditTarget
    data class TotalAmount(val current: Double, val currency: String = "") : ReceiptEditTarget
}

/**
 * 發票沉浸式原地編輯底部工作表 (ReceiptEditBottomSheet)。
 * 1:1 對齊 Flutter 的 _showImmersiveBottomSheet 與 receipt_item_row.dart 編輯抽屜：
 * 1. 採用 LensWise 統一固定高度面板架構 (Type A: LensWiseFixedSheet, isWrapContent = true, hasScrim = true)。
 * 2. 頂層獨立視口承載，杜絕滾動容器約束衝突。
 * 3. 鍵盤避讓 (imePadding) 確保輸入框在軟鍵盤彈出時自動向上推擠。
 * 4. Header 設計 1:1 對齊 Flutter：
 *    - 左側 48dp Spacer 平衡
 *    - 中央居中 13sp 粗體大寫、字距 2.0sp 灰色標題
 *    - 右側 28dp 主色圓形打勾按鈕 (點擊即儲存，絕無底部多餘按鈕)
 * 5. 顯式設定 focusedTextColor / unfocusedTextColor，徹底解決暗黑主題下白底白字隱形缺陷。
 */
@Composable
fun ReceiptEditBottomSheet(
    target: ReceiptEditTarget,
    onDismissRequest: () -> Unit,
    onSaveStoreName: (String) -> Unit,
    onSaveItem: (Int, ReceiptItem) -> Unit,
    onSaveTotalAmount: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val inputBgColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    LensWiseFixedSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        isWrapContent = true,
        hasScrim = true,
        containerColor = containerColor,
        headerContent = { dragModifier ->
            Box(
                modifier = dragModifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                LensWiseSheetHandle(isDark = isDark)
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
            ) {
                when (target) {
                    is ReceiptEditTarget.StoreName -> {
                        var text by remember(target) { mutableStateOf(target.current) }

                        fun doSave() {
                            if (text.isNotBlank()) {
                                onSaveStoreName(text.trim())
                                onDismissRequest()
                            }
                        }

                        // Header with Centered Title & Done Checkmark (1:1 對齊 Flutter)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(48.dp))
                            Text(
                                text = "編輯商店名稱".uppercase(),
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { doSave() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "確認儲存",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { doSave() }),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                disabledContainerColor = inputBgColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AppColors.primary
                            )
                        )
                    }

                    is ReceiptEditTarget.Item -> {
                        var originalName by remember(target) { mutableStateOf(target.item.originalName) }
                        var translatedName by remember(target) { mutableStateOf(target.item.translatedName) }
                        var priceText by remember(target) {
                            mutableStateOf(if (target.item.price > 0) "%.2f".format(target.item.price) else "")
                        }

                        fun doSave() {
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
                        }

                        // Header with Centered Title & Done Checkmark (1:1 對齊 Flutter receipt_item_row.dart)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(48.dp))
                            Text(
                                text = "編輯品項".uppercase(),
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { doSave() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "確認儲存",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = originalName,
                            onValueChange = { originalName = it },
                            label = { Text("原始品名", fontFamily = OutfitFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                color = textColor
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                                focusedLabelColor = AppColors.primary,
                                unfocusedLabelColor = secondaryTextColor,
                                cursorColor = AppColors.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = translatedName,
                            onValueChange = { translatedName = it },
                            label = { Text("翻譯品名", fontFamily = OutfitFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                color = textColor
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                                focusedLabelColor = AppColors.primary,
                                unfocusedLabelColor = secondaryTextColor,
                                cursorColor = AppColors.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("金額", fontFamily = OutfitFontFamily) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { doSave() }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontSize = 16.sp,
                                color = textColor
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                                focusedLabelColor = AppColors.primary,
                                unfocusedLabelColor = secondaryTextColor,
                                cursorColor = AppColors.primary
                            )
                        )
                    }

                    is ReceiptEditTarget.TotalAmount -> {
                        var amountText by remember(target) {
                            mutableStateOf(if (target.current > 0) "%.2f".format(target.current) else "")
                        }

                        fun doSave() {
                            val parsed = amountText.toDoubleOrNull()
                            if (parsed != null) {
                                onSaveTotalAmount(parsed)
                                onDismissRequest()
                            }
                        }

                        // Header with Centered Title & Done Checkmark (1:1 對齊 Flutter)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(48.dp))
                            Text(
                                text = "編輯總計".uppercase(),
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { doSave() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "確認儲存",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        val currencyPrefix = if (target.currency.isNotBlank()) "${target.currency} " else ""

                        TextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            ),
                            prefix = {
                                if (currencyPrefix.isNotEmpty()) {
                                    Text(
                                        text = currencyPrefix,
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryTextColor
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { doSave() }),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                disabledContainerColor = inputBgColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AppColors.primary
                            )
                        )
                    }
                }
            }
        }
    )
}
