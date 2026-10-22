package com.lenswise.app.core.ai.provider.geminiflow

import android.util.Base64
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.core.ai.provider.geminiflow.dto.GeminiFlowChatRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
 * 透過強型別 [GeminiFlowChatRequestDto] 與 OkHttp SSE 實現通訊，
 * 嚴格遵循蛇形命名協定 (system_prompt)，杜絕提示詞遺失。
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
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun generate(request: GenerateRequest): GenerateResponse = withContext(Dispatchers.IO) {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/chat"
        val payloadJson = buildPayloadJson(request)
        val body = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType())
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
            GenerateResponse(text = text, isDone = true)
        }
    }

    override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = callbackFlow {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/stream"
        val payloadJson = buildPayloadJson(request)
        val body = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType())
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
    }.flowOn(Dispatchers.IO)

    internal fun buildPayloadJson(request: GenerateRequest): String {
        val imagesBase64 = request.images?.map {
            Base64.encodeToString(it, Base64.NO_WRAP)
        } ?: emptyList()

        val dto = GeminiFlowChatRequestDto(
            prompt = request.prompt,
            systemPrompt = request.systemPrompt?.takeIf { it.isNotBlank() },
            model = request.model ?: modelName,
            thinking = request.thinking,
            language = request.language,
            images = imagesBase64,
            sessionId = request.sessionId
        )

        return jsonParser.encodeToString(dto)
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
