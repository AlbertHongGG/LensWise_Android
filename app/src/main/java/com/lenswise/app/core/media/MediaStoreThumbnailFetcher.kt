package com.lenswise.app.core.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job

/**
 * 原生 MediaStore 系統硬體微縮圖 Fetcher (v2 強化版)。
 *
 * 核心保證：
 * 1. 深度綁定協程生命週期與 Android 原生 CancellationSignal：
 *    當使用者快速滑過視口時，被取消項目的協程立即向系統 ContentResolver 發出 signal.cancel()，
 *    瞬間釋放底層 IPC 與圖片解碼線程池，杜絕滾動白塊與 Binder 塞車。
 * 2. Android 10+ (API 29+) 採用 loadThumbnail 直接讀取系統 15KB 微縮圖快取，完全繞過原圖 I/O。
 * 3. 完善的 API 26-28 降級相容機制。
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val cr = context.contentResolver
        val signal = CancellationSignal()

        // 綁定協程 Job 取消監聽，快速滑動時立即中止 IPC
        val job = currentCoroutineContext().job
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                signal.cancel()
            }
        }

        val bitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cr.loadThumbnail(data, Size(360, 360), signal)
            } else {
                val id = data.lastPathSegment?.toLongOrNull() ?: return null
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    cr,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }
        }.getOrNull() ?: return null

        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (isMediaStoreImageUri(data)) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }

        private fun isMediaStoreImageUri(uri: Uri): Boolean {
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) return false
            val authority = uri.authority ?: return false
            val path = uri.path ?: return false
            return authority.contains("media", ignoreCase = true) && path.contains("images", ignoreCase = true)
        }
    }
}
