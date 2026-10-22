package com.lenswise.app.features.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenswise.app.features.ai.config.ConfigField
import com.lenswise.app.presentation.theme.AppColors
import com.lenswise.app.presentation.theme.AppTypography
import com.lenswise.app.presentation.theme.OutfitFontFamily

/**
 * AI 供應商單一動態表單欄位元件 (ConfigFieldTile)。
 * 1:1 復刻參考原型之單行圖標、文字與下拉箭頭佈局。
 */
@Composable
fun ConfigFieldTile(
    field: ConfigField,
    isDark: Boolean,
    onSelectOptionClick: (ConfigField.Select) -> Unit,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGlobal: Boolean = false
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val icon = getFieldIcon(field.key)

    when (field) {
        is ConfigField.Select -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onSelectOptionClick(field) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                val isUnset = field.value.isBlank()

                Text(
                    text = field.displayLabel,
                    style = AppTypography.itemValue.copy(
                        color = if (isUnset) textSecondary else textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        is ConfigField.Text -> {
            var localText by remember(field.value) { mutableStateOf(field.value) }

            LaunchedEffect(field.value) {
                if (localText != field.value) {
                    localText = field.value
                }
            }

            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (localText.isEmpty()) {
                        val placeholderText = field.resolvePlaceholder(isGlobal)
                        Text(
                            text = placeholderText,
                            style = AppTypography.itemValue.copy(
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color.White.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.38f)
                            )
                        )
                    }

                    BasicTextField(
                        value = localText,
                        onValueChange = {
                            localText = it
                            onTextChanged(it)
                        },
                        singleLine = true,
                        textStyle = AppTypography.inputText.copy(
                            color = textPrimary
                        ),
                        cursorBrush = SolidColor(AppColors.primary),
                        visualTransformation = if (field.isSecret) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 依欄位 key 匹配專屬圖標。
 */
private fun getFieldIcon(key: String): ImageVector {
    val k = key.lowercase()
    return when {
        k.contains("model") -> Icons.Default.SmartToy
        k.contains("url") || k.contains("endpoint") -> Icons.Default.Dns
        k.contains("key") || k.contains("token") -> Icons.Default.VpnKey
        k.contains("id") -> Icons.Default.Fingerprint
        k.contains("location") || k.contains("region") -> Icons.Default.LocationOn
        k.contains("thinking") -> Icons.Default.Tune
        else -> Icons.Default.Tune
    }
}


