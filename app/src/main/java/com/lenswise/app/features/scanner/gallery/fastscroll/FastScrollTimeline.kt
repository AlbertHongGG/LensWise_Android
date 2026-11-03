package com.lenswise.app.features.scanner.gallery.fastscroll

import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import kotlin.math.roundToInt

/**
 * 時間軸快速索引引擎 (FastScrollTimeline)。
 *
 * 核心保證：
 * 1. 純 Kotlin 領域模型，完全解耦 Android UI 與 Compose Runtime，支援極速 JVM 單元測試。
 * 2. 雙向映射：
 *    - 幾何進度 [0.0, 1.0] -> 相簿絕對項目索引 TargetIndex (保證 0.0 對齊最新相片，1.0 對齊歷史最古老相片)。
 *    - 相簿絕對項目索引 TargetIndex -> 所屬月份 DateBucket (透過預編譯 Header 索引二分查找，O(log M) 複雜度)。
 * 3. 嚴格邊界保護：對於空清單、極端浮點數 (負數、大於 1.0、NaN) 進行嚴密數值校準。
 */
class FastScrollTimeline(
    items: List<GalleryUiItem>
) {
    val totalItems: Int = items.size

    val headerIndices: IntArray
    val headerBuckets: Array<DateBucket>

    init {
        val indicesList = ArrayList<Int>(items.size.coerceAtMost(64))
        val bucketsList = ArrayList<DateBucket>(items.size.coerceAtMost(64))

        var lastBucket: DateBucket? = null
        for (i in items.indices) {
            val item = items[i]
            when (item) {
                is GalleryUiItem.Header -> {
                    indicesList.add(i)
                    bucketsList.add(item.bucket)
                    lastBucket = item.bucket
                }
                is GalleryUiItem.Photo -> {
                    // 若清單開頭非 Header 或月份交替無 Header，補建錨點
                    if (indicesList.isEmpty() || (lastBucket != null && lastBucket != item.asset.bucket)) {
                        indicesList.add(i)
                        bucketsList.add(item.asset.bucket)
                        lastBucket = item.asset.bucket
                    }
                }
            }
        }

        headerIndices = indicesList.toIntArray()
        headerBuckets = bucketsList.toTypedArray()
    }

    /**
     * 是否為有效時間軸（至少有一筆項目）。
     */
    val isValid: Boolean get() = totalItems > 0 && headerIndices.isNotEmpty()

    /**
     * 將滑桿垂直比例 [0.0, 1.0] 換算為清單目標索引 (Target Item Index)。
     * 遵循內容密度實體映射 (Content-Density Timeline Mapping)：
     * - 0.0 -> 0 (最新相片)
     * - 1.0 -> totalItems - 1 (最古老相片)
     */
    fun getTargetIndex(fraction: Float): Int {
        if (totalItems <= 0) return 0
        val clamped = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
        return (clamped * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
    }

    /**
     * 將清單當前索引 (Current Item Index) 換算為滑桿垂直比例 [0.0, 1.0]。
     */
    fun getFractionForIndex(index: Int): Float {
        if (totalItems <= 1) return 0f
        return (index.coerceIn(0, totalItems - 1).toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
    }

    /**
     * 根據項目索引，透過二分搜尋取得其所屬的月份時間桶 (DateBucket)。
     * 複雜度：O(log M)，其中 M 為月份總數 (通常 <= 60)，查詢耗時 < 0.001ms。
     */
    fun getBucketForIndex(index: Int): DateBucket? {
        if (headerIndices.isEmpty() || totalItems <= 0) return null
        val targetIndex = index.coerceIn(0, totalItems - 1)

        var low = 0
        var high = headerIndices.size - 1
        var bestMatch = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (headerIndices[mid] <= targetIndex) {
                bestMatch = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return headerBuckets[bestMatch]
    }

    /**
     * 根據滑動百分比 [0.0, 1.0] 獲取當前對應的 DateBucket。
     */
    fun getBucketForFraction(fraction: Float): DateBucket? {
        val targetIndex = getTargetIndex(fraction)
        return getBucketForIndex(targetIndex)
    }
}
