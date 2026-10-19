package com.lenswise.app.core.ai.provider.geminiflow

import android.util.Base64
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GeminiFlow 核心 Provider。
 * 透過 OkHttp SSE 實現雙工/單向串流事件解碼，嚴格相容參考專案伺服器規範。
 */
class GeminiFlowProvider(
    private val baseUrl: String = "http://10.0.2.2:5000",
    private val modelName: String = "gemini-2.5-flash",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val name: String = "geminiflow"
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun generate(request: GenerateRequest): GenerateResponse {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/chat"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder().url(url).post(body).build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw IOException("GeminiFlow error (${response.code}): $errorBody")
            }
            val respString = response.body?.string().orEmpty()
            val json = jsonParser.parseToJsonElement(respString).jsonObject
            val text = json["text"]?.jsonPrimitive?.content
                ?: json["response"]?.jsonPrimitive?.content
                ?: ""
            return GenerateResponse(text = text, isDone = true)
        }
    }

    override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = callbackFlow {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/stream"
        val payload = buildPayload(request)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(body)
            .build()

        val sseFactory = EventSources.createFactory(client)
        val eventSource = sseFactory.newEventSource(httpRequest, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    when (type) {
                        "text" -> {
                            val chunkText = extractText(data)
                            if (chunkText.isNotEmpty()) {
                                trySend(GenerateResponse(text = chunkText, isDone = false))
                            }
                        }
                        "done" -> {
                            trySend(GenerateResponse(text = "", isDone = true))
                            close()
                        }
                        "error" -> {
                            close(IOException("SSE Stream Error: $data"))
                        }
                        else -> {
                            val chunkText = extractText(data)
                            if (chunkText.isNotEmpty()) {
                                trySend(GenerateResponse(text = chunkText, isDone = false))
                            }
                        }
                    }
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = t?.message ?: "SSE Failure with code ${response?.code}"
                close(IOException(errorMsg, t))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }

    private fun buildPayload(request: GenerateRequest): JsonObject {
        val imagesBase64 = request.images?.map {
            Base64.encodeToString(it, Base64.NO_WRAP)
        } ?: emptyList()

        return buildJsonObject {
            put("prompt", request.prompt)
            request.systemPrompt?.let { put("systemPrompt", it) }
            put("model", request.model ?: modelName)
            putJsonArray("images") {
                imagesBase64.forEach { add(JsonPrimitive(it)) }
            }
        }
    }

    private fun extractText(dataStr: String): String {
        if (dataStr.isBlank()) return ""
        return try {
            val element = jsonParser.parseToJsonElement(dataStr)
            if (element is JsonObject) {
                element["chunk"]?.jsonPrimitive?.content
                    ?: element["text"]?.jsonPrimitive?.content
                    ?: element["response"]?.jsonPrimitive?.content
                    ?: element["content"]?.jsonPrimitive?.content
                    ?: ""
            } else {
                dataStr
            }
        } catch (_: Exception) {
            dataStr
        }
    }
}
