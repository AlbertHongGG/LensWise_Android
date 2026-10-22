package com.lenswise.app.features.menu.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.presentation.components.StreamingSkeleton

/**
 * 菜單品項清單區塊 (MenuListSection)。
 * 1:1 對齊 LensWise Flutter 版：
 * - 串流中繼骨架屏 (無資料時 3 行 60dp 骨架屏、增量串流時末尾追加 60dp 骨架屏)
 * - 流暢漸進展開每一個已完整解析之菜單品項。
 */
@Composable
fun MenuListSection(
    record: MenuScannerRecord,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val items = record.items

    if (items.isEmpty()) {
        if (!isStreaming) return

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 60.dp, isDark = isDark)
            Spacer(modifier = Modifier.height(16.dp))
            StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 60.dp, isDark = isDark)
            Spacer(modifier = Modifier.height(16.dp))
            StreamingSkeleton(modifier = Modifier.fillMaxWidth(), height = 60.dp, isDark = isDark)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.size - 1 && !isStreaming
            MenuListItemTile(
                item = item,
                isLast = isLast
            )
        }

        if (isStreaming) {
            Spacer(modifier = Modifier.height(8.dp))
            StreamingSkeleton(
                modifier = Modifier.fillMaxWidth(),
                height = 60.dp,
                isDark = isDark
            )
        }
    }
}
