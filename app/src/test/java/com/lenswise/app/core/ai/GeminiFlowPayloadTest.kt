package com.lenswise.app.core.ai

import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.provider.geminiflow.GeminiFlowProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiFlowPayloadTest {

    @Test
    fun testGeminiFlowPayload_serializesSnakeCaseSystemPrompt() {
        val provider = GeminiFlowProvider()
        val request = GenerateRequest(
            prompt = "請解析收據照片。",
            systemPrompt = "你是一個發票解析專家，嚴格輸出 JSON。",
            model = "gemini-3.8-flash",
            thinking = "low",
            sessionId = "session-test-123"
        )

        val payloadJson = provider.buildPayloadJson(request)

        // 必須包含蛇形命名的 system_prompt
        assertTrue(payloadJson.contains("\"system_prompt\":"))
        assertTrue(payloadJson.contains("你是一個發票解析專家"))

        // 絕對不可包含駝峰命名的 systemPrompt (此為歷史 bug)
        assertFalse(payloadJson.contains("\"systemPrompt\":"))

        // 必須包含 thinking, language, model, session_id
        assertTrue(payloadJson.contains("\"thinking\":\"low\"") || payloadJson.contains("\"thinking\": \"low\""))
        assertTrue(payloadJson.contains("\"language\":\"zh-TW\"") || payloadJson.contains("\"language\": \"zh-TW\""))
        assertTrue(payloadJson.contains("\"model\":\"gemini-3.8-flash\"") || payloadJson.contains("\"model\": \"gemini-3.8-flash\""))
        assertTrue(payloadJson.contains("\"session_id\":\"session-test-123\"") || payloadJson.contains("\"session_id\": \"session-test-123\""))
    }
}
