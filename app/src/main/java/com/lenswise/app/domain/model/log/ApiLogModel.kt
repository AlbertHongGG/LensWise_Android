package com.lenswise.app.domain.model.log

import com.lenswise.app.features.ai.domain.AiAgentType
import java.io.File

/**
 * API 日誌列表輕量元資料模型。
 *
 * 遵循記憶體解耦原則：
 * 列表常駐記憶體僅保存輕量索引（id, title, time, durationMs），
 * 巨量酬載（rawJson 及內含之 Base64 圖片）改為按需延遲載入（Lazy Loading），
 * 徹底杜絕常駐 Heap 飆破引發的系統 OOM 或背景查殺。
 */
data class ApiLogModel(
    override val id: String,
    override val file: File,
    val title: String,
    val displayTime: String,
    val durationMs: Long,
    val timestamp: Long,
    val rawJson: String = "",
    val agentType: AiAgentType? = null,
    val source: String = agentType?.displayName ?: "其他"
) : DeletableLog {
    override val timestampMillis: Long get() = timestamp

    /**
     * 按需取得完整 JSON 內容（若 rawJson 為空則自磁碟即時延遲讀取）
     */
    fun resolveRawJson(): String = if (rawJson.isNotEmpty()) rawJson else runCatching { file.readText() }.getOrDefault("{}")
}
