package com.lenswise.app.core.ai.streaming

/**
 * 輕量級高效串流 JSON 語法修復器 (JsonStreamRepairer)。
 * 面對 SSE 傳輸中斷片、未閉合字串與未閉合陣列/物件括號，
 * 透過堆疊狀態機進行即時閉合，產出可被 kotlinx.serialization 安全解析的合法 JSON。
 */
object JsonStreamRepairer {

    /**
     * 自動補齊未封閉的字串引號、修復懸掛鍵值對（如 `"price": `）與括號堆疊閉合。
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

        // 1. 若處於未閉合的字串內部，補全結束雙引號
        if (inString) {
            sb.append('"')
        }

        // 2. 檢查尾端是否遺留懸掛逗號或冒號
        var content = sb.toString().trimEnd()
        if (content.endsWith(",")) {
            content = content.dropLast(1).trimEnd()
            sb.clear()
            sb.append(content)
        } else if (content.endsWith(":")) {
            sb.append(" null")
        }

        // 3. 依後進先出順序閉合未封閉的物件與陣列括號
        while (stack.isNotEmpty()) {
            val open = stack.removeAt(stack.size - 1)
            if (open == '{') sb.append('}')
            else if (open == '[') sb.append(']')
        }

        return sb.toString()
    }
}
