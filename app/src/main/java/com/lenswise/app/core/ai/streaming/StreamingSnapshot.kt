package com.lenswise.app.core.ai.streaming

/**
 * 串流快照不可變資料容器。
 *
 * @param T 領域資料類型
 * @property data 當前累計反序列化後的領域資料實體
 * @property isComplete 是否整個串流已經閉合且結束
 * @property rawChunk 本次觸發快照的最新原生文字片段
 */
data class StreamingSnapshot<T>(
    val data: T,
    val isComplete: Boolean = false,
    val rawChunk: String? = null
)
