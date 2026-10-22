package com.lenswise.app.core.ai.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Parcelize
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

data class AiChatRequest(
    val systemContextPrompt: String,
    val history: List<ChatMessage>,
    val newQuery: String
)
