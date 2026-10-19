package com.lenswise.app.core.media

/**
 * 展平式相簿視圖單元項目密封介面 (GalleryUiItem)。
 *
 * 核心保證：
 * 1. 消除動態巢狀循環，在 Compose LazyVerticalGrid 中以一維平整清單呈現。
 * 2. 嚴格穩定的 stableKey 保證 Compose 能夠 100% 精準執行 Diffing 與視圖複用。
 * 3. 內建 spanCount：Header 自動跨滿 3 欄，Photo 佔據 1 欄。
 */
sealed interface GalleryUiItem {
    val stableKey: String
    val spanCount: Int

    data class Header(
        val bucket: DateBucket,
        val totalCount: Int
    ) : GalleryUiItem {
        override val stableKey: String get() = "header_${bucket.key}"
        override val spanCount: Int get() = 3
    }

    data class Photo(
        val asset: MediaAsset,
        val isSelected: Boolean,
        val selectionIndex: Int? = null
    ) : GalleryUiItem {
        override val stableKey: String get() = "photo_${asset.id}"
        override val spanCount: Int get() = 1
    }
}
