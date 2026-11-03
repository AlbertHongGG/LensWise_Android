package com.lenswise.app.domain.model.log

import kotlinx.serialization.Serializable

/**
 * 系統通知日誌序列化實體 (NotificationLogRecord)。
 * 取代舊有 org.json.JSONObject，提供強型別序列化與非同步存儲。
 */
@Serializable
data class NotificationLogRecord(
    val timestamp: String = "",
    val type: String = "info",
    val message: String = ""
)
