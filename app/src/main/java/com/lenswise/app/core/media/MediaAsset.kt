package com.lenswise.app.core.media

import android.net.Uri

data class MediaAsset(
    val id: Long,
    val contentUri: Uri,
    val dateAddedSec: Long,
    val groupKey: String
)
