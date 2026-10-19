package com.lenswise.app.core.ai.provider.vertexai

import android.util.Base64
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * 原生 Google Cloud Vertex AI Provider。
 * 100% 對齊 LensWise 原版 VertexAIProvider 規範：
 * - 支援專案 ID、區域位置、模型名稱與 API Key / Bearer 權杖認證
 * - 支援標準多模態 inlineData 傳輸
 * - 支援單次 generate 與 streamGenerateContent (SSE) 漸進輸出
 */
class VertexAiProvider(
    private val projectId: String,
    private val location: String = "us-central1",
    private val modelName: String = "gemini-2.5-flash",
    private val apiKey: String = "",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val name: String = "vertexai"
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun generate(request: GenerateRequest): GenerateResponse = withContext(Dispatchers.IO) {
        val url = "https://$location-aiplatform.googleapis.com/v1/projects/$projectId/locations/$location/publishers/google/models/$modelName:generateContent"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val reqBuilder = Request.Builder().url(url).post(body)
        applyAuth(reqBuilder)

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw IOException("Vertex AI error (${response.code}): $err")
            }
            val respString = response.body?.string().orEmpty()
            val json = jsonParser.parseToJsonElement(respString).jsonObject
            val text = extractText(json)
            GenerateResponse(text = text, isDone = true)
        }
    }

    override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = callbackFlow {
        val url = "https://$location-aiplatform.googleapis.com/v1/projects/$projectId/locations/$location/publishers/google/models/$modelName:streamGenerateContent?alt=sse"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .post(body)
        applyAuth(reqBuilder)

        val call = client.newCall(reqBuilder.build())

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                close(IOException("Vertex AI stream error (${response.code}): $err"))
                return@callbackFlow
            }

            response.body?.source()?.let { source ->
                while (!source.exhausted() && isActive) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    if (trimmed.startsWith("data: ")) {
                        val jsonString = trimmed.removePrefix("data: ").trim()
                        if (jsonString.isEmpty()) continue
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

    private fun applyAuth(builder: Request.Builder) {
        if (apiKey.isBlank()) return
        if (apiKey.startsWith("AIza")) {
            builder.addHeader("x-goog-api-key", apiKey)
        } else {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }
    }

    private fun buildPayload(request: GenerateRequest): JsonObject {
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
                    put("role", "system")
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", request.systemPrompt) })
                    }
                }
            }
        }
    }

    private fun extractText(json: JsonObject): String {
        val candidates = json["candidates"]?.jsonArray ?: return ""
        if (candidates.isEmpty()) return ""
        val content = candidates[0].jsonObject["content"]?.jsonObject ?: return ""
        val parts = content["parts"]?.jsonArray ?: return ""
        return parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }.joinToString("")
    }
}
