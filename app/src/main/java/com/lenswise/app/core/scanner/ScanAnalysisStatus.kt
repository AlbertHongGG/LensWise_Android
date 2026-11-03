package com.lenswise.app.core.scanner

/**
 * 掃描與影像分析全局生命週期狀態列舉 (ScanAnalysisStatus)。
 * 統一全功能模式（收據、商品、比對、菜單）的推論與串流階段契約。
 */
enum class ScanAnalysisStatus {
    IDLE,
    CONNECTING, // 等待 AI Provider 建立連線與首個 Token (TTFT)
    STREAMING,  // 首個 Token 抵達，漸進串流展開結果
    SUCCESS,    // 串流結束且本機持久化成功落地
    ERROR       // 網路、模型或解析過程發生例外
}
