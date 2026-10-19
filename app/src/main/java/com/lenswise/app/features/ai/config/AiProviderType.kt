package com.lenswise.app.features.ai.config

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 支援的 AI 供應商列舉。
 */
enum class AiProviderType(val displayName: String, val identifier: String) {
    GEMINIFLOW("GeminiFlow", "geminiflow"),
    OLLAMA("Ollama", "ollama"),
    VERTEX_AI("Vertex AI", "vertexai");

    val icon: ImageVector
        get() = when (this) {
            GEMINIFLOW -> Icons.Default.AutoAwesome
            OLLAMA -> Icons.Default.Memory
            VERTEX_AI -> Icons.Default.CloudDone
        }

    companion object {
        fun fromIdentifier(id: String?): AiProviderType {
            return entries.firstOrNull { it.identifier.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: GEMINIFLOW
        }
    }
}
