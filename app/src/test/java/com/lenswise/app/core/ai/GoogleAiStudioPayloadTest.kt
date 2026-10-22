package com.lenswise.app.core.ai

import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.provider.googleaistudio.GoogleAiStudioProvider
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAiStudioPayloadTest {

    @Test
    fun testBuildPayload_formatsContentsAndSystemInstruction() {
        val provider = GoogleAiStudioProvider(
            apiKey = "AIzaSyTest123",
            modelName = "gemini-3.8-flash",
            thinkingLevel = "low"
        )

        val request = GenerateRequest(
            prompt = "辨識這張收據的消費明細",
            systemPrompt = "你是一個發票辨識專家",
            temperature = 0.5f
        )

        val payload = provider.buildPayload(request)

        // 1. 驗證 contents
        val contents = payload["contents"]?.jsonArray
        assertNotNull(contents)
        assertEquals(1, contents?.size)
        val userTurn = contents?.get(0)?.jsonObject
        assertEquals("user", userTurn?.get("role")?.jsonPrimitive?.content)
        val parts = userTurn?.get("parts")?.jsonArray
        assertNotNull(parts)
        assertEquals("辨識這張收據的消費明細", parts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content)

        // 2. 驗證 systemInstruction
        val systemInstruction = payload["systemInstruction"]?.jsonObject
        assertNotNull(systemInstruction)
        val sysParts = systemInstruction?.get("parts")?.jsonArray
        assertEquals("你是一個發票辨識專家", sysParts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content)

        // 3. 驗證 generationConfig 與 thinkingConfig
        val genConfig = payload["generationConfig"]?.jsonObject
        assertNotNull(genConfig)
        assertEquals("0.5", genConfig?.get("temperature")?.jsonPrimitive?.content)
        val thinkingConfig = genConfig?.get("thinkingConfig")?.jsonObject
        assertNotNull(thinkingConfig)
        assertEquals("1024", thinkingConfig?.get("thinkingBudget")?.jsonPrimitive?.content)
    }

    @Test
    fun testBuildPayload_encodesMultimodalImageInlineData() {
        val provider = GoogleAiStudioProvider(
            apiKey = "AIzaSyTest123",
            modelName = "gemini-3.6-flash"
        )

        val dummyImageBytes = byteArrayOf(1, 2, 3, 4, 5)
        val request = GenerateRequest(
            prompt = "分析圖片",
            images = listOf(dummyImageBytes)
        )

        val payload = provider.buildPayload(request)
        val userTurn = payload["contents"]?.jsonArray?.get(0)?.jsonObject
        val parts = userTurn?.get("parts")?.jsonArray
        assertNotNull(parts)
        assertEquals(2, parts?.size)

        val textPart = parts?.get(0)?.jsonObject
        assertEquals("分析圖片", textPart?.get("text")?.jsonPrimitive?.content)

        val imagePart = parts?.get(1)?.jsonObject
        val inlineData = imagePart?.get("inlineData")?.jsonObject
        assertNotNull(inlineData)
        assertEquals("image/jpeg", inlineData?.get("mimeType")?.jsonPrimitive?.content)
        assertTrue(inlineData?.get("data")?.jsonPrimitive?.content?.isNotEmpty() == true)
    }
}
