package com.lenswise.app.features.scanner.gallery.fastscroll

import com.lenswise.app.core.media.DateBucket
import com.lenswise.app.core.media.GalleryUiItem
import kotlin.math.roundToInt

/**
 * 現代化月份離散時間軸索引引擎 (FastScrollTimeline)。
 *
 * 核心保證：
 * 1. 對標 Google 相簿互動模型：快速滑動條以「月份 (DateBucket)」為離散單位進行吸附跳轉，
 *    徹底終結逐像素隨機跳轉所導致的主線程佈局突變雪崩。
 * 2. 雙向映射：
 *    - 滑桿垂直行程 [0.0, 1.0] -> 月份區段 Section [0, M - 1] -> 該月 HeaderItem 絕對索引。
 *    - 網格首項索引 firstVisibleItemIndex -> 所屬月份 Section [0, M - 1] -> 滑塊進度 [0.0, 1.0]。
 * 3. 純 Kotlin 領域模型，完全解耦 Android UI，支援極速純 JVM 單元測試。
 */
class FastScrollTimeline(
    items: List<GalleryUiItem>
) {
    val totalItems: Int = items.size

    val headerIndices: IntArray
    val headerBuckets: Array<DateBucket>
    val sectionCount: Int

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
                    // 若清單開頭非 Header 或月份交替無 Header，補建月份錨點
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
        sectionCount = headerIndices.size
    }

    /**
     * 是否為有效時間軸（至少有一筆項目且含有至少一個月份區段）。
     */
    val isValid: Boolean get() = totalItems > 0 && sectionCount > 0

    /**
     * 將滑桿垂直比例 [0.0, 1.0] 換算為目標月份索引 (Section Index: 0 到 sectionCount - 1)。
     * - 0.0 -> 0 (最新月份)
     * - 1.0 -> sectionCount - 1 (最古老月份)
     */
    fun getSectionIndex(fraction: Float): Int {
        if (sectionCount <= 0) return 0
        val clamped = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
        return (clamped * (sectionCount - 1)).roundToInt().coerceIn(0, sectionCount - 1)
    }

    /**
     * 獲取指定月份區段在相簿網格中的 Header 絕對項目索引 (用於 scrollToItem)。
     */
    fun getHeaderItemIndexForSection(sectionIndex: Int): Int {
        if (sectionCount <= 0) return 0
        val clamped = sectionIndex.coerceIn(0, sectionCount - 1)
        return headerIndices[clamped]
    }

    /**
     * 獲取指定月份區段對應的 DateBucket。
     */
    fun getBucketForSection(sectionIndex: Int): DateBucket? {
        if (sectionCount <= 0) return null
        val clamped = sectionIndex.coerceIn(0, sectionCount - 1)
        return headerBuckets[clamped]
    }

    /**
     * 根據滑動百分比 [0.0, 1.0] 獲取當前所屬月份的 DateBucket。
     */
    fun getBucketForFraction(fraction: Float): DateBucket? {
        val section = getSectionIndex(fraction)
        return getBucketForSection(section)
    }

    /**
     * 根據項目索引，透過二分搜尋取得其所屬的月份區段索引 (Section Index)。
     * 複雜度：O(log M)，其中 M 為月份總數，查詢耗時 < 0.001ms。
     */
    fun getSectionIndexForVisibleItem(firstVisibleIndex: Int): Int {
        if (sectionCount <= 0 || totalItems <= 0) return 0
        val targetIndex = firstVisibleIndex.coerceIn(0, totalItems - 1)

        var low = 0
        var high = sectionCount - 1
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

        return bestMatch
    }

    /**
     * 根據可見項目索引獲取對應的 DateBucket。
     */
    fun getBucketForVisibleItem(firstVisibleIndex: Int): DateBucket? {
        val section = getSectionIndexForVisibleItem(firstVisibleIndex)
        return getBucketForSection(section)
    }

    /**
     * 將網格當前首項索引 (firstVisibleItemIndex) 換算為滑桿垂直比例 [0.0, 1.0]。
     * 結合月份間距提供連續平滑的滑塊跟隨反饋。
     */
    fun getFractionForVisibleItem(firstVisibleIndex: Int): Float {
        if (sectionCount <= 1 || totalItems <= 1) return 0f
        val section = getSectionIndexForVisibleItem(firstVisibleIndex)
        val sectionStart = headerIndices[section]
        val sectionEnd = if (section + 1 < sectionCount) headerIndices[section + 1] else totalItems
        val span = (sectionEnd - sectionStart).coerceAtLeast(1)
        val withinProgress = (firstVisibleIndex - sectionStart).toFloat() / span

        val continuousSection = section.toFloat() + withinProgress.coerceIn(0f, 1f)
        return (continuousSection / (sectionCount - 1).toFloat()).coerceIn(0f, 1f)
    }
}
