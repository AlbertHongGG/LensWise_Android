package com.lenswise.app.core.media

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 年月時間桶 (DateBucket)。
 *
 * 核心保證：
 * 1. 內建靜態快取池，數萬張照片共用極少數月份實例，達成近乎零記憶體分配 (Zero GC pressure)。
 * 2. 實作 Comparable，由新至舊 (Descending) 高效有序排序。
 * 3. 預先產生不可變 displayTitle 與 key，杜絕滑動時格式化字串的運算消耗。
 */
data class DateBucket(
    val year: Int,
    val month: Int
) : Comparable<DateBucket> {

    val key: String = "${year}_$month"
    val displayTitle: String = "${year} 年 ${month} 月"

    override fun compareTo(other: DateBucket): Int {
        val yearDiff = other.year.compareTo(this.year)
        return if (yearDiff != 0) yearDiff else other.month.compareTo(this.month)
    }

    companion object {
        private val zoneId = ZoneId.systemDefault()
        private val cache = ConcurrentHashMap<Int, DateBucket>()

        fun fromEpochMillis(epochMillis: Long): DateBucket {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis.coerceAtLeast(0L)), zoneId)
            val year = zdt.year
            val month = zdt.monthValue
            val cacheKey = year * 100 + month
            return cache.computeIfAbsent(cacheKey) { DateBucket(year, month) }
        }

        fun fromEpochSeconds(epochSeconds: Long): DateBucket {
            return fromEpochMillis(epochSeconds * 1000L)
        }
    }
}
