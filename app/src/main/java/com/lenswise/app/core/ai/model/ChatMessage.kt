package com.lenswise.app.core.ai.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiChatRequest(
    val systemContextPrompt: String,
    val history: List<ChatMessage>,
    val newQuery: String
)
