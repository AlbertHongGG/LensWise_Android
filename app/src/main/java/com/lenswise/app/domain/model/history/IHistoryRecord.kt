package com.lenswise.app.domain.model.history

/**
 * 全域歷史紀錄領域實體統一契約介面 (IHistoryRecord)。
 * 所有功能模組 (收據、菜單、比對、商品分析) 的歷史紀錄均實作此介面，
 * 支援統一歷史畫面檢視與獨立儲存解耦。
 */
interface IHistoryRecord {
    val id: String
    val timestamp: Long
    val displayTitle: String
    val displaySubtitle: String
    val imagePaths: List<String>
}
