package com.lenswise.app.core.logging

import com.lenswise.app.domain.model.log.NotificationLogRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationLogManagerTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testNotificationLogRecordSerialization_roundTrip() {
        val record = NotificationLogRecord(
            timestamp = "2026-11-03T12:30:00.000",
            type = "success",
            message = "收據分析成功完成"
        )

        val serialized = json.encodeToString(record)
        val deserialized = json.decodeFromString<NotificationLogRecord>(serialized)

        assertEquals(record.timestamp, deserialized.timestamp)
        assertEquals(record.type, deserialized.type)
        assertEquals(record.message, deserialized.message)
    }
}
