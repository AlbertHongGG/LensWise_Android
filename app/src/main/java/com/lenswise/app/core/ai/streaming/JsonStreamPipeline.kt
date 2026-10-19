package com.lenswise.app.core.ai.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * 串流增量 JSON 解析與自適應修復管線 (JsonStreamPipeline)。
 *
 * 核心特徵：
 * 1. 首字節瞬時觸發 (TTFT Immediate Emit)：捕獲第一個合法 Snapshot 瞬間發射，作為頁面流轉訊號。
 * 2. 局部 JSON 補全修復：面對未傳輸完成的截斷字串與巢狀括號自動補閉合，防止反序列化例外。
 * 3. 自適應渲染防抖節流 (Throttle Interval)：後續 Emission 以 50ms 節流，杜絕高頻 chunk 引發 Compose 畫面重繪過載。
 * 4. 終止標記 Flush：串流關閉時進行最終反序列化解析，並發射 isComplete = true 終態。
 */
object JsonStreamPipeline {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 從原生字串中截取第一個 `{` 之後的 JSON 候選子字串，過濾 Markdown 標記 (如 ```json)
     */
    fun extractJsonSubstring(raw: String): String {
        val firstBrace = raw.indexOf('{')
        if (firstBrace == -1) return raw.trim()
        return raw.substring(firstBrace).trim()
    }

    /**
     * 輕量級串流 JSON 自動修復器：
     * 自動補齊未封閉的字串引號與大括號/中括號堆疊，產出可被 kotlinx.serialization 解析的合法 JSON
     */
    fun repairPartialJson(candidate: String): String {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith('{')) return "{}"

        val stack = mutableListOf<Char>()
        var inString = false
        var isEscaped = false

        for (ch in trimmed) {
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (ch == '\\') {
                isEscaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                when (ch) {
                    '{', '[' -> stack.add(ch)
                    '}' -> if (stack.isNotEmpty() && stack.last() == '{') stack.removeAt(stack.size - 1)
                    ']' -> if (stack.isNotEmpty() && stack.last() == '[') stack.removeAt(stack.size - 1)
                }
            }
        }

        val sb = StringBuilder(trimmed)
        if (inString) {
            sb.append('"') // 補閉未封閉的字串
        }

        // 依後進先出順序閉合括號
        while (stack.isNotEmpty()) {
            val open = stack.removeAt(stack.size - 1)
            if (open == '{') sb.append('}')
            else if (open == '[') sb.append(']')
        }

        return sb.toString()
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

            val candidate = extractJsonSubstring(buffer.toString())
            if (candidate.isEmpty() || !candidate.startsWith('{')) return@collect

            val now = System.currentTimeMillis()
            val shouldAttempt = !hasEmittedFirstToken || (now - lastEmitTime >= throttleMs)

            if (shouldAttempt) {
                try {
                    val repaired = repairPartialJson(candidate)
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
        val finalCandidate = extractJsonSubstring(buffer.toString())
        if (finalCandidate.isNotEmpty() && finalCandidate.startsWith('{')) {
            try {
                val repaired = repairPartialJson(finalCandidate)
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
