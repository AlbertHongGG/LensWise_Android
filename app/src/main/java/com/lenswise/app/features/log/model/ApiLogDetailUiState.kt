package com.lenswise.app.features.log.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private val detailJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * API 日誌詳細畫面之展示層狀態模型 (UI State)。
 * 將原始 JSON 解耦為不可變之領域狀態，阻斷字串偏執 (Primitive Obsession)，
 * 統一採用跨平台之 kotlinx.serialization 作為單一解析事實標準，杜絕平台環境差異與反覆序列化。
 */
data class ApiLogDetailUiState(
    val title: String,
    val status: Int,
    val isSuccess: Boolean,
    val durationMs: Long,
    val formattedTimestamp: String,
    val requestData: Any?,
    val responseData: Any?,
    val rawJson: String
) {
    companion object {
        fun fromRawJson(rawJson: String): ApiLogDetailUiState {
            val jsonObject = runCatching {
                detailJson.parseToJsonElement(rawJson) as? JsonObject
            }.getOrNull()

            val title = jsonObject?.get("agentName")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "API Request"
            val durationMs = jsonObject?.get("durationMs")?.jsonPrimitive?.longOrNull ?: 0L
            val rawTimestamp = jsonObject?.get("timestamp")?.jsonPrimitive?.contentOrNull ?: ""
            val rawStatus = jsonObject?.get("status")?.jsonPrimitive?.intOrNull ?: 200

            // 判斷是否含錯誤訊息
            val respObj = jsonObject?.get("response") as? JsonObject
            val errorInResp = respObj?.get("error")?.jsonPrimitive?.contentOrNull
            val hasError = !errorInResp.isNullOrBlank()

            val effectiveStatus = if (hasError && rawStatus in 200..299) 500 else rawStatus
            val isSuccess = effectiveStatus in 200..299 && !hasError

            val requestData = jsonObject?.get("request")?.toString()
            val responseData = jsonObject?.get("response")?.toString()

            return ApiLogDetailUiState(
                title = title,
                status = effectiveStatus,
                isSuccess = isSuccess,
                durationMs = durationMs,
                formattedTimestamp = formatLogTimestamp(rawTimestamp),
                requestData = requestData,
                responseData = responseData,
                rawJson = rawJson
            )
        }

        /**
         * 格式化時間戳為精簡可讀格式 (例如 2026-11-09 18:19:19.756)。
         * 自動去除 ISO 'T' 字元並修剪微秒至 3 位毫秒。
         */
        fun formatLogTimestamp(rawTimestamp: String): String {
            if (rawTimestamp.isBlank()) return ""
            val clean = rawTimestamp.replace('T', ' ').trim()
            val dotIndex = clean.indexOf('.')
            return if (dotIndex != -1) {
                val base = clean.substring(0, dotIndex)
                val fraction = clean.substring(dotIndex + 1).take(3)
                "$base.$fraction"
            } else {
                clean
            }
        }
    }
}
