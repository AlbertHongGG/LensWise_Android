package com.lenswise.app.core.scanner

import com.lenswise.app.core.image.AppImageEntity

/**
 * 掃描分析 Session 統一視圖狀態模型 (ScanSessionUiState)。
 *
 * @param sessionId 每次啟動分析時單調遞增的唯一識別碼，嚴格界定 Session 生命週期，杜絕競態與跨次殘留
 * @param status 當前分析狀態
 * @param record 泛型領域分析紀錄
 * @param capturedImages 本次分析輸入的圖片清單
 * @param isStreamingComplete 串流推論是否已全部接收完畢
 * @param errorMessage 錯誤提示資訊
 */
data class ScanSessionUiState<TRecord>(
    val sessionId: Long = 0L,
    val status: ScanAnalysisStatus = ScanAnalysisStatus.IDLE,
    val record: TRecord,
    val capturedImages: List<AppImageEntity> = emptyList(),
    val isStreamingComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val isAnalyzing: Boolean
        get() = status == ScanAnalysisStatus.CONNECTING || status == ScanAnalysisStatus.STREAMING
}
