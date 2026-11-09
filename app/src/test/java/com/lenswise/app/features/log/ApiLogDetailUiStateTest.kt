package com.lenswise.app.features.log

import com.lenswise.app.features.log.model.ApiLogDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiLogDetailUiStateTest {

    @Test
    fun fromRawJson_validSuccessfulJson_parsesCorrectly() {
        val json = """
            {
                "timestamp": "2026-11-07T11:13:55.433000",
                "agentName": "ProductLens",
                "durationMs": 4185,
                "status": 200,
                "request": { "prompt": "test prompt" },
                "response": { "text": "result content" }
            }
        """.trimIndent()

        val state = ApiLogDetailUiState.fromRawJson(json)

        assertEquals("ProductLens", state.title)
        assertEquals(200, state.status)
        assertTrue(state.isSuccess)
        assertEquals(4185L, state.durationMs)
        assertEquals("2026-11-07 11:13:55.433", state.formattedTimestamp)
        assertNotNull(state.requestData)
        assertNotNull(state.responseData)
    }

    @Test
    fun fromRawJson_errorResponse_marksAsFailedAndStatus500() {
        val json = """
            {
                "timestamp": "2026-11-09T18:19:19.756",
                "agentName": "ProductLens",
                "durationMs": 515,
                "status": 200,
                "request": {},
                "response": { "error": "Quota exceeded" }
            }
        """.trimIndent()

        val state = ApiLogDetailUiState.fromRawJson(json)

        assertEquals("ProductLens", state.title)
        assertEquals(500, state.status)
        assertFalse(state.isSuccess)
        assertEquals(515L, state.durationMs)
        assertEquals("2026-11-09 18:19:19.756", state.formattedTimestamp)
    }

    @Test
    fun fromRawJson_malformedJson_fallsBackGracefully() {
        val state = ApiLogDetailUiState.fromRawJson("invalid json content")

        assertEquals("API Request", state.title)
        assertEquals(200, state.status)
        assertTrue(state.isSuccess)
        assertEquals(0L, state.durationMs)
        assertEquals("", state.formattedTimestamp)
        assertNull(state.requestData)
        assertNull(state.responseData)
    }

    @Test
    fun formatLogTimestamp_variousFormats_trimmedAccurately() {
        assertEquals(
            "2026-11-07 11:13:55.433",
            ApiLogDetailUiState.formatLogTimestamp("2026-11-07T11:13:55.433000")
        )
        assertEquals(
            "2026-11-09 18:19:19.756",
            ApiLogDetailUiState.formatLogTimestamp("2026-11-09 18:19:19.756")
        )
        assertEquals(
            "2026-11-09 18:19:19",
            ApiLogDetailUiState.formatLogTimestamp("2026-11-09T18:19:19")
        )
        assertEquals(
            "",
            ApiLogDetailUiState.formatLogTimestamp("")
        )
    }
}
