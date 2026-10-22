package com.lenswise.app.presentation.components.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lenswise.app.domain.model.settings.TargetLanguage

/**
 * 目標翻譯語言選擇抽屜 (LanguagePickerBottomSheet)。
 * 封裝 PickerBottomSheetShell 與 InfiniteWheelPicker，專職處理 TargetLanguage 選取邏輯。
 */
@Composable
fun LanguagePickerBottomSheet(
    currentLanguage: TargetLanguage,
    onConfirm: (TargetLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = remember { TargetLanguage.entries }
    val initialIndex = remember(currentLanguage) {
        languages.indexOf(currentLanguage).coerceAtLeast(0)
    }

    var selectedIndex by remember { mutableIntStateOf(initialIndex) }

    PickerBottomSheetShell(
        title = "目標翻譯語言",
        onConfirm = {
            val selected = languages.getOrElse(selectedIndex) { TargetLanguage.TRADITIONAL_CHINESE }
            onConfirm(selected)
        },
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        InfiniteWheelPicker(
            items = languages,
            initialIndex = initialIndex,
            itemText = { it.displayName },
            onItemSelected = { index ->
                selectedIndex = index
            }
        )
    }
}
