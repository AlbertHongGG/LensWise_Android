package com.lenswise.app.core.ai.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * 串流增量 JSON 解析與自適應修復管線 (JsonStreamPipeline)。
 *
 * 核心特徵：
 * 1. 髒字串清洗防禦 (JsonStreamSanitizer)：精確過濾前置引言、Markdown Code Block 標籤與尾端雜訊。
 * 2. 語法狀態機補全 (JsonStreamRepairer)：自動閉合引號、懸掛欄位與括號堆疊。
 * 3. 首字節瞬時觸發 (TTFT Immediate Emit)：捕獲第一個合法 Snapshot 瞬間發射，作為頁面流轉訊號。
 * 4. 自適應渲染防抖節流 (Throttle Interval)：後續 Emission 以 50ms 節流，杜絕高頻 chunk 引發 Compose 畫面重繪過載。
 * 5. 終止標記 Flush：串流關閉時進行最終反序列化解析，並發射 isComplete = true 終態。
 */
object JsonStreamPipeline {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** 便捷提供靜態字串提取接口 */
    fun extractJsonSubstring(raw: String): String {
        return JsonStreamSanitizer.extractJsonCandidate(raw)
    }

    /** 便捷提供靜態語法修復接口 */
    fun repairPartialJson(candidate: String): String {
        return JsonStreamRepairer.repairPartialJson(candidate)
    }

    /**
     * 將原生文字串流轉換為強型別領域快照串流。
     */
    fun <T> bind(
        rawStream: Flow<String>,
        mapper: (JsonObject) -> T,
        initialData: T,
        throttleMs: Long = 50L
    ): Flow<StreamingSnapshot<T>> = flow {
        val buffer = StringBuilder()
        var currentData = initialData
        var lastEmitTime = 0L
        var hasEmittedFirstToken = false

        rawStream.collect { chunk ->
            if (chunk.isEmpty()) return@collect
            buffer.append(chunk)

            val candidate = JsonStreamSanitizer.extractJsonCandidate(buffer.toString())
            if (candidate.isEmpty() || !candidate.startsWith('{')) return@collect

            val now = System.currentTimeMillis()
            val shouldAttempt = !hasEmittedFirstToken || (now - lastEmitTime >= throttleMs)

            if (shouldAttempt) {
                try {
                    val repaired = JsonStreamRepairer.repairPartialJson(candidate)
                    val jsonElement = json.parseToJsonElement(repaired)
                    if (jsonElement is JsonObject) {
                        currentData = mapper(jsonElement)
                        hasEmittedFirstToken = true
                        lastEmitTime = now
                        emit(
                            StreamingSnapshot(
                                data = currentData,
                                isComplete = false,
                                rawChunk = chunk
                            )
                        )
                    }
                } catch (_: Exception) {
                    // 若 JSON 目前處於關鍵語法斷點無法修復，靜默繼續累積 chunk
                }
            }
        }

        // 串流結束時的最終 Flush 解析
        val finalCandidate = JsonStreamSanitizer.extractJsonCandidate(buffer.toString())
        if (finalCandidate.isNotEmpty() && finalCandidate.startsWith('{')) {
            try {
                val repaired = JsonStreamRepairer.repairPartialJson(finalCandidate)
                val jsonElement = json.parseToJsonElement(repaired)
                if (jsonElement is JsonObject) {
                    currentData = mapper(jsonElement)
                }
            } catch (_: Exception) {
                // 保持已解析之 currentData
            }
        }

        emit(
            StreamingSnapshot(
                data = currentData,
                isComplete = true
            )
        )
    }
}
