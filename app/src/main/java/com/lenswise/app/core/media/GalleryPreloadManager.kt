package com.lenswise.app.core.media

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 相簿首屏預載管理器契約 (GalleryPreloadManager)。
 * 於相機取景待命或背景閒置期間，預先將最新相片之微縮圖解碼至 Coil MemoryCache。
 * 達成點開相簿瞬間 0ms 呈現首屏。
 */
interface GalleryPreloadManager {
    /** 預先將前 count 張相片讀入 Coil MemoryCache (RAM) */
    fun preloadTopThumbnails(assets: List<MediaAsset>, count: Int = 21)
}

/**
 * 基于 Coil 的相簿首屏微縮圖預載器實作。
 */
class CoilGalleryPreloadManager(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : GalleryPreloadManager {

    override fun preloadTopThumbnails(assets: List<MediaAsset>, count: Int) {
        if (assets.isEmpty()) return
        val targets = assets.take(count)
        scope.launch {
            for (asset in targets) {
                val request = ImageRequest.Builder(context)
                    .data(asset)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }
}
