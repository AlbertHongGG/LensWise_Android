package com.lenswise.app.core.logging

import com.lenswise.app.domain.model.log.ApiLogModel
import com.lenswise.app.features.ai.domain.AiAgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class ApiLogManagerTest {

    @Test
    fun testFileNameAgentTypeParsing_resolvesKnownAgent() {
        val fileName = "20261019_201500_ReceiptScanner_123456.json"
        val parts = fileName.split("_")
        val rawAgentName = if (parts.size >= 4) parts[2] else null
        val agentType = rawAgentName?.let { AiAgentType.fromId(it) }

        assertNotNull(agentType)
        assertEquals(AiAgentType.RECEIPT_SCANNER, agentType)
        assertEquals("收據掃描", agentType?.displayName)
    }

    @Test
    fun testFileNameAgentTypeParsing_resolvesAiChatAgent() {
        val fileName = "20261019_201500_AiChat_987654.json"
        val parts = fileName.split("_")
        val rawAgentName = if (parts.size >= 4) parts[2] else null
        val agentType = rawAgentName?.let { AiAgentType.fromId(it) }

        assertNotNull(agentType)
        assertEquals(AiAgentType.AI_CHAT, agentType)
        assertEquals("AI 深度探討", agentType?.displayName)
    }

    @Test
    fun testApiLogModelSource_matchesAgentDisplayName() {
        val model = ApiLogModel(
            id = "test.json",
            file = File("test.json"),
            title = "ReceiptScanner",
            displayTime = "2026-10-19 20:15:00",
            durationMs = 1200L,
            timestamp = 1760876100000L,
            agentType = AiAgentType.RECEIPT_SCANNER
        )

        assertEquals("收據掃描", model.source)
    }
}
