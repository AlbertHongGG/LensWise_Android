package com.lenswise.app.core.ai.provider.ollama

import android.util.Base64
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 原生 Ollama 本地模型 Provider。
 * 100% 對齊 LensWise 原版 OllamaProvider 規範：
 * - 端點：`POST /api/generate`
 * - 支援多模態 Base64 圖像編碼
 * - 支援非串流 (`stream: false`) 與 NDJSON 行串流 (`stream: true`)
 */
class OllamaProvider(
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val modelName: String = "llama3.2-vision:latest",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val name: String = "ollama"
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun generate(request: GenerateRequest): GenerateResponse = withContext(Dispatchers.IO) {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/api/generate"
        val payload = buildPayload(request, stream = false)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder().url(url).post(body).build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw IOException("Ollama API error (${response.code}): $errorBody")
            }
            val respString = response.body?.string().orEmpty()
            val json = jsonParser.parseToJsonElement(respString).jsonObject
            val text = json["response"]?.jsonPrimitive?.content.orEmpty()
            GenerateResponse(text = text, isDone = true)
        }
    }

    override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = callbackFlow {
        val sanitizedBaseUrl = baseUrl.trimEnd('/')
        val url = "$sanitizedBaseUrl/api/generate"
        val payload = buildPayload(request, stream = true)
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder().url(url).post(body).build()

        val call = client.newCall(httpRequest)

        try {
            val response = call.execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    close(IOException("Ollama stream error (${resp.code}): $err"))
                    return@callbackFlow
                }

                resp.body?.source()?.let { source ->
                    while (!source.exhausted() && isActive) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        runCatching {
                            val json = jsonParser.parseToJsonElement(trimmed).jsonObject
                            val chunk = json["response"]?.jsonPrimitive?.content.orEmpty()
                            val isDone = json["done"]?.jsonPrimitive?.booleanOrNull == true
                            if (chunk.isNotEmpty() || isDone) {
                                trySend(GenerateResponse(text = chunk, isDone = isDone))
                            }
                        }
                    }
                }
            }
            close()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPayload(request: GenerateRequest, stream: Boolean) = buildJsonObject {
        put("model", modelName)
        put("prompt", request.prompt)
        if (!request.systemPrompt.isNullOrEmpty()) {
            put("system", request.systemPrompt)
        }
        put("stream", stream)
        val images = request.images
        if (!images.isNullOrEmpty()) {
            putJsonArray("images") {
                images.forEach { bytes ->
                    add(Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
        }
    }
}
