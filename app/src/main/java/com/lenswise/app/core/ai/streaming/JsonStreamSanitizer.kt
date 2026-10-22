package com.lenswise.app.core.ai.streaming

/**
 * 串流增量 JSON 文字清洗器 (JsonStreamSanitizer)。
 * 專職負責在 SSE 串流過程中即時剝離 Markdown 圍欄代碼塊 (```json)、前置對話引言與後置雜訊，
 * 精確定位合法 JSON 根節點。
 */
object JsonStreamSanitizer {

    /**
     * 從原生 chunk 累積緩衝區中擷取乾淨的 JSON 候選子字串：
     * 1. 剝離 Markdown 圍欄（如 ```json 或 ```）與前置客套引言（如「這兩張圖片... 以下為資料：」）。
     * 2. 定位第一個 JSON 根物件 `{`。若模型意外以陣列 `[` 開頭，則定位陣列內的第一個物件 `{`。
     * 3. 截斷後續出現的閉合 Markdown 圍欄 (```) 與後置對話文字。
     */
    fun extractJsonCandidate(raw: String): String {
        if (raw.isBlank()) return ""

        val firstBrace = raw.indexOf('{')
        if (firstBrace == -1) {
            // 尚未產出任何 JSON 物件起始大括號
            return ""
        }

        // 從第一個 '{' 開始截取，丟棄前面所有引言與 Markdown 代碼塊前綴
        var candidate = raw.substring(firstBrace).trim()

        // 若後面出現結尾的 Markdown 代碼塊標記 (```)，截斷拋棄其後的文字
        val closingFence = candidate.indexOf("```")
        if (closingFence != -1) {
            candidate = candidate.substring(0, closingFence).trim()
        }

        return candidate
    }
}
