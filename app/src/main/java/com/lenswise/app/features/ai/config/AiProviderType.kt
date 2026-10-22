package com.lenswise.app.features.ai.config

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 支援的 AI 供應商列舉。
 */
enum class AiProviderType(val displayName: String, val identifier: String) {
    GEMINIFLOW("GeminiFlow", "geminiflow"),
    GOOGLE_AI_STUDIO("Google AI Studio", "google_ai_studio"),
    OLLAMA("Ollama", "ollama"),
    VERTEX_AI("Vertex AI", "vertexai");

    val icon: ImageVector
        get() = when (this) {
            GEMINIFLOW -> Icons.Default.AutoAwesome
            GOOGLE_AI_STUDIO -> Icons.Default.Key
            OLLAMA -> Icons.Default.Memory
            VERTEX_AI -> Icons.Default.CloudDone
        }

    fun createDefaultConfig(): AiProviderConfig = when (this) {
        GEMINIFLOW -> GeminiFlowConfig()
        GOOGLE_AI_STUDIO -> GoogleAiStudioConfig()
        OLLAMA -> OllamaConfig()
        VERTEX_AI -> VertexAiConfig()
    }

    fun createEmptyConfig(): AiProviderConfig = when (this) {
        GEMINIFLOW -> GeminiFlowConfig.empty()
        GOOGLE_AI_STUDIO -> GoogleAiStudioConfig.empty()
        OLLAMA -> OllamaConfig.empty()
        VERTEX_AI -> VertexAiConfig.empty()
    }

    companion object {
        fun fromIdentifier(id: String?): AiProviderType {
            return entries.firstOrNull { it.identifier.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: GEMINIFLOW
        }
    }
}
