package com.lenswise.app.core.ai

import com.lenswise.app.core.ai.agent.BaseAgent
import com.lenswise.app.core.ai.model.GenerateRequest
import com.lenswise.app.core.ai.model.GenerateResponse
import com.lenswise.app.core.ai.provider.AiProvider
import com.lenswise.app.core.ai.streaming.StreamingSnapshot
import com.lenswise.app.domain.model.log.AiLogger
import com.lenswise.app.domain.model.log.ApiLogRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseAgentLoggingTest {

    private class FakeLogger : AiLogger {
        val loggedRecords = mutableListOf<ApiLogRecord>()
        override fun isLoggingEnabled(): Boolean = true
        override fun setLoggingEnabled(enabled: Boolean) {}
        override fun logInteraction(record: ApiLogRecord) {
            loggedRecords.add(record)
        }
    }

    private class FakeStreamingProvider : AiProvider {
        override val name: String = "fake_stream"
        override suspend fun generate(request: GenerateRequest): GenerateResponse {
            return GenerateResponse("Full text", isDone = true)
        }
        override fun generateStream(request: GenerateRequest): Flow<GenerateResponse> = flow {
            emit(GenerateResponse("Chunk 1, ", isDone = false))
            emit(GenerateResponse("Chunk 2, ", isDone = false))
            emit(GenerateResponse("Chunk 3.", isDone = true))
        }
    }

    private class TestAgent(
        provider: AiProvider,
        logger: AiLogger
    ) : BaseAgent<String, String>("TestAgent", provider, logger) {
        override suspend fun execute(input: String): String = "noop"
        override fun executeStreamSnapshot(input: String): Flow<StreamingSnapshot<String>> {
            return executeStreamingPipeline(
                request = GenerateRequest(prompt = input),
                mapper = { it.toString() },
                initialData = ""
            )
        }

        fun testPromptStream(request: GenerateRequest): Flow<GenerateResponse> {
            return executePromptStream(request)
        }
    }

    @Test
    fun testDelayedLogging_onlyLogsOnceAtStreamCompletion() = runBlocking {
        val fakeLogger = FakeLogger()
        val fakeProvider = FakeStreamingProvider()
        val agent = TestAgent(fakeProvider, fakeLogger)

        val stream = agent.testPromptStream(GenerateRequest(prompt = "Hello"))

        // 在尚未 collect 之前，日誌應為 0
        assertEquals(0, fakeLogger.loggedRecords.size)

        var emittedChunks = 0
        stream.collect { chunk ->
            emittedChunks++
            // 串流傳輸過程中，日誌仍應為 0 (絕不逐 chunk 寫入日誌)
            assertEquals(0, fakeLogger.loggedRecords.size)
        }

        assertEquals(3, emittedChunks)
        // 串流完全終止後，日誌精確為 1 筆
        assertEquals(1, fakeLogger.loggedRecords.size)

        val log = fakeLogger.loggedRecords.first()
        assertEquals("TestAgent", log.agentName)
        assertEquals("Chunk 1, Chunk 2, Chunk 3.", log.response.text)
        assertTrue("串流耗時應大於或等於 0", log.durationMs >= 0)
    }
}
