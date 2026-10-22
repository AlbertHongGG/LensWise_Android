package com.lenswise.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.lenswise.app.core.media.MediaStoreThumbnailFetcher

class LensWiseApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: LensWiseApplication
            private set
    }

    lateinit var notificationLogManager: com.lenswise.app.core.logging.NotificationLogManager
        private set
    lateinit var apiLogManager: com.lenswise.app.core.logging.ApiLogManager
        private set
    lateinit var receiptHistoryRepository: com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
        private set
    lateinit var aiConfigRepository: com.lenswise.app.features.ai.config.AiConfigRepository
        private set
    lateinit var capturedImageRepository: com.lenswise.app.core.image.CapturedImageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationLogManager = com.lenswise.app.core.logging.NotificationLogManager(this)
        apiLogManager = com.lenswise.app.core.logging.ApiLogManager(this)
        receiptHistoryRepository = com.lenswise.app.features.receipt.data.ReceiptHistoryRepository(this)
        aiConfigRepository = com.lenswise.app.features.ai.config.AiConfigRepository(this)
        capturedImageRepository = com.lenswise.app.core.image.DefaultCapturedImageRepository()

        com.lenswise.app.presentation.notification.NotificationController.addListener { type, message ->
            notificationLogManager.logNotification(type, message)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@LensWiseApplication))
            }
            .respectCacheHeaders(false)
            .crossfade(false)
            .build()
    }
}
