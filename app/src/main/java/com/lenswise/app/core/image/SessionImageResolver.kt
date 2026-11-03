package com.lenswise.app.core.image

import com.lenswise.app.LensWiseApplication
import java.io.File

/**
 * 封裝「即時串流暫存圖片 (Live Session)」與「歷史持久化磁碟圖片 (History Mode)」的統一雙軌圖片解析器。
 *
 * 核心保證：
 * 1. 隔離 UI 元件與底層圖片來源（AppImageEntity.file vs 磁碟持久化相對/絕對路徑 File），實現依賴倒置。
 * 2. 徹底消除在 UI 執行緒執行 readBytes() 產生的大型 ByteArray，始終提供本地 File 給 Coil 進行高階非同步解碼。
 * 3. 支援純淨相對路徑解析 (baseDir/history/receipt_images/xxx.jpg)，杜絕因 Android 內部儲存絕對路徑更動導致破圖。
 * 4. 嚴格執行索引保護（Index Clamping），當要求索引超出圖片長度時平滑鎖定至邊界，杜絕 IndexOutOfBoundsException。
 * 5. 支援邊界防護與空值安全返回，無可用圖片時優雅返回 null，杜絕 NullPointerException。
 */
class SessionImageResolver(
    private val memoryImages: List<AppImageEntity>? = null,
    private val imagePaths: List<String> = emptyList(),
    private val baseDir: File? = null
) {
    /** 是否處於記憶體暫存態（即時串流分析階段） */
    val isMemorySource: Boolean
        get() = !memoryImages.isNullOrEmpty()

    /** 當前可用圖片總數 */
    val count: Int
        get() = if (isMemorySource) memoryImages!!.size else imagePaths.size

    /** 是否包含至少一張可用圖片 */
    val hasImages: Boolean
        get() = count > 0

    /**
     * 取得安全快取的實體 File 物件。
     * 若為相對路徑，優先使用提供的 baseDir 或 LensWiseApplication.instance.filesDir 解析。
     *
     * @param index 目標圖片索引
     * @return 存在且有效的本機 File，無圖片時返回 null
     */
    fun resolveFile(index: Int): File? {
        if (count == 0) return null
        val safeIndex = index.coerceIn(0, count - 1)
        return if (isMemorySource) {
            memoryImages!![safeIndex].file
        } else {
            val path = imagePaths[safeIndex]
            if (path.isNotBlank()) {
                val f = File(path)
                if (f.isAbsolute) {
                    f
                } else {
                    val root = baseDir ?: runCatching { LensWiseApplication.instance.filesDir }.getOrNull()
                    if (root != null) File(root, path) else f
                }
            } else null
        }
    }

    /**
     * 批次取得所有可用實體 File 清單。
     */
    fun getAllFiles(): List<File> {
        return if (isMemorySource) {
            memoryImages!!.map { it.file }
        } else {
            imagePaths.indices.mapNotNull { resolveFile(it) }
        }
    }

    /**
     * 取得磁碟檔案路徑（歷史模式下有效，即時模式下若未持久化可能為 null）。
     */
    fun getDiskPath(index: Int): String? {
        if (imagePaths.isEmpty()) return null
        val safeIndex = index.coerceIn(0, imagePaths.size - 1)
        val path = imagePaths[safeIndex]
        return path.takeIf { it.isNotBlank() }
    }
}
