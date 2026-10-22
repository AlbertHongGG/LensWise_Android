package com.lenswise.app.core.ai.provider.googleaistudio

import android.util.Base64
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.features.ai.config.GeminiThinkingLevel
import com.lenswise.app.features.ai.config.GoogleAiStudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 原生 Google AI Studio (Gemini API) 供應商實作。
 * 透過 Google Generative Language API 端點與 API Key 直接通訊：
 * - 支援標準多模態 inlineData 傳輸
 * - 支援系統指令 (systemInstruction) 與思考等級 (thinkingConfig)
 * - 支援單次 generate 與 streamGenerateContent (SSE) 漸進輸出
 * - 支援 Google REST 結構化錯誤解析與安全調用
 */
class GoogleAiStudioProvider(
    private val apiKey: String,
    private val modelName: String = GoogleAiStudioModel.FLASH_3_8,
    private val thinkingLevel: String = GeminiThinkingLevel.LOW.value,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val name: String = "google_ai_studio"
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun generate(request: GenerateRequest): GenerateResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IOException("Google AI Studio API Key 未設定，請至調度中心填寫。")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(body)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                val message = parseErrorMessage(err)
                throw IOException("Google AI Studio error (${response.code}): $message")
            }
            val respString = response.body?.string().orEmpty()
            val json = jsonParser.parseToJsonElement(respString).jsonObject
            val text = extractText(json)
            GenerateResponse(text = text, isDone = true)
        }
    }

    override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = callbackFlow {
        if (apiKey.isBlank()) {
            close(IOException("Google AI Studio API Key 未設定，請至調度中心填寫。"))
            return@callbackFlow
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?alt=sse"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = client.newCall(httpRequest)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                val message = parseErrorMessage(err)
                close(IOException("Google AI Studio stream error (${response.code}): $message"))
                return@callbackFlow
            }

            response.body?.source()?.let { source ->
                while (!source.exhausted() && isActive) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    if (trimmed.startsWith("data: ")) {
                        val jsonString = trimmed.removePrefix("data: ").trim()
                        if (jsonString.isEmpty() || jsonString == "[DONE]") continue

                        runCatching {
                            val json = jsonParser.parseToJsonElement(jsonString).jsonObject
                            val text = extractText(json)
                            if (text.isNotEmpty()) {
                                trySend(GenerateResponse(text = text, isDone = false))
                            }
                        }
                    }
                }
            }
            trySend(GenerateResponse(text = "", isDone = true))
            close()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            call.cancel()
        }
    }

    internal fun buildPayload(request: GenerateRequest): JsonObject {
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", request.prompt) })
            request.images?.forEach { bytes ->
                add(buildJsonObject {
                    putJsonObject("inlineData") {
                        put("mimeType", "image/jpeg")
                        put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    }
                })
            }
        }

        val contents = buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("parts", parts)
            })
        }

        return buildJsonObject {
            put("contents", contents)

            if (!request.systemPrompt.isNullOrEmpty()) {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", request.systemPrompt) })
                    }
                }
            }

            putJsonObject("generationConfig") {
                request.temperature?.let { put("temperature", it) }
                val effectiveThinking = if (thinkingLevel.isNotEmpty()) thinkingLevel else request.thinking
                val budget = resolveThinkingBudget(effectiveThinking)
                if (budget != null) {
                    putJsonObject("thinkingConfig") {
                        put("thinkingBudget", budget)
                    }
                }
            }
        }
    }

    private fun resolveThinkingBudget(level: String): Int? {
        return when (GeminiThinkingLevel.fromValue(level)) {
            GeminiThinkingLevel.LOW -> 1024
            GeminiThinkingLevel.MEDIUM -> 2048
            GeminiThinkingLevel.HIGH -> 8192
        }
    }

    private fun parseErrorMessage(rawError: String): String {
        return runCatching {
            val json = jsonParser.parseToJsonElement(rawError).jsonObject
            json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: rawError
        }.getOrDefault(rawError)
    }

    private fun extractText(json: JsonObject): String {
        val candidates = json["candidates"]?.jsonArray ?: return ""
        if (candidates.isEmpty()) return ""
        val content = candidates[0].jsonObject["content"]?.jsonObject ?: return ""
        val parts = content["parts"]?.jsonArray ?: return ""
        return parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }.joinToString("")
    }
}
