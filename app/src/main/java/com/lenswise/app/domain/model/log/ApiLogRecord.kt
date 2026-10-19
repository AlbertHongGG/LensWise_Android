package com.lenswise.app.domain.model.log

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 系統標準 API / AI 互動紀錄領域實體。
 * 遵循「零幽靈欄位原則」：未提供或為空的屬性在序列化與儲存時一律不輸出。
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ApiLogRecord(
    val timestamp: String,
    val agentName: String,
    val durationMs: Long,
    val request: ApiLogRequest,
    val response: ApiLogResponse
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ApiLogRequest(
    val model: String,
    val prompt: String,
    @SerialName("system_prompt")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val systemPrompt: String? = null,
    @SerialName("session_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sessionId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val images: List<String>? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ApiLogResponse(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val text: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val images: List<String>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val error: String? = null
)
