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

    override fun onCreate() {
        super.onCreate()
        instance = this
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
