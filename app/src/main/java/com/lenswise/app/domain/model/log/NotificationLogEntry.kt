package com.lenswise.app.domain.model.log

import com.lenswise.app.presentation.notification.NotificationType
import java.io.File

data class NotificationLogEntry(
    override val id: String,
    override val file: File,
    val type: NotificationType,
    val message: String,
    val timestampStr: String,
    override val timestampMillis: Long
) : DeletableLog
