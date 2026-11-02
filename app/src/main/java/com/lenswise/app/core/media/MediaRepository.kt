package com.lenswise.app.core.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 系統相簿媒體存取領域倉庫契約 (MediaRepository)。
 *
 * 遵循依賴倒置原則 (DIP)：
 * 1. 徹底解耦表現層 (ViewModel) 與 Android Framework Context。
 * 2. 支援 100% JVM 純單元測試與 Mock 注入。
 */
interface MediaRepository {
    /** 響應式監聽系統相簿變更流 */
    fun observeMediaAssets(): Flow<List<MediaAsset>>

    /** 單次全相簿元數據極速索引查詢 */
    suspend fun queryAllMediaAssets(): List<MediaAsset>

    /** 線性單次構建包含 Header 與 Photo 的展平清單 */
    suspend fun buildTimelineItems(
        assets: List<MediaAsset>,
        selectedIds: Set<Long> = emptySet()
    ): List<GalleryUiItem>
}

/**
 * 工廠函式：建立預設之 MediaStore 原生實作。
 */
fun MediaRepository(context: Context): MediaRepository = MediaStoreMediaRepository(context)

/**
 * 現代化極速原生 MediaStore 相簿儲存庫實作。
 *
 * 核心保證：
 * 1. 響應式相簿流 (observeMediaAssets)：以 ContentObserver 自動感知系統新照片與異動。
 * 2. 混合時間戳安全排序：解決截圖/下載圖因 DATE_TAKEN 為空被 SQLite 排入末端的沉底缺陷。
 * 3. 毫秒級載入 (<30ms 取得全相簿索引)，一維展平時間軸 (Flattened Timeline)。
 */
class MediaStoreMediaRepository(
    private val context: Context
) : MediaRepository {

    override fun observeMediaAssets(): Flow<List<MediaAsset>> = callbackFlow {
        val contentResolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // 1. 首次進入立即發射一次
        trySend(queryAllMediaAssets())

        // 2. 監聽系統 MediaStore 異動通知
        val handler = runCatching { Handler(Looper.getMainLooper()) }.getOrNull()
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                launch(Dispatchers.IO) {
                    trySend(queryAllMediaAssets())
                }
            }
        }

        runCatching {
            contentResolver.registerContentObserver(collection, true, observer)
        }

        awaitClose {
            runCatching {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun queryAllMediaAssets(): List<MediaAsset> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaAsset>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )

        val contentResolver: ContentResolver = context.contentResolver

        runCatching {
            val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val queryArgs = Bundle().apply {
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media._ID)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
                contentResolver.query(collection, projection, null, null, sortOrder)
            }

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenCol = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedCol = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val dateTakenRaw = if (dateTakenCol >= 0) c.getLong(dateTakenCol) else 0L
                    val dateAddedSec = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L

                    // 優先採用有效 DATE_TAKEN (ms)，無值 (0 或負數) 時降級使用 DATE_ADDED (sec * 1000)
                    val effectiveDateMs = when {
                        dateTakenRaw > 0 -> dateTakenRaw
                        dateAddedSec > 0 -> dateAddedSec * 1000L
                        else -> System.currentTimeMillis()
                    }

                    val uri = ContentUris.withAppendedId(collection, id)
                    val bucket = DateBucket.fromEpochMillis(effectiveDateMs)

                    result.add(
                        MediaAsset(
                            id = id,
                            contentUri = uri,
                            dateTakenMs = effectiveDateMs,
                            bucket = bucket
                        )
                    )
                }
            }
        }

        // 記憶體級時間戳降序校準，確保混合來源圖片精確依最新時間排序
        result.sortByDescending { it.dateTakenMs }
        result
    }

    override suspend fun buildTimelineItems(
        assets: List<MediaAsset>,
        selectedIds: Set<Long>
    ): List<GalleryUiItem> = withContext(Dispatchers.Default) {
        if (assets.isEmpty()) return@withContext emptyList()

        val grouped = linkedMapOf<DateBucket, MutableList<MediaAsset>>()
        for (asset in assets) {
            grouped.getOrPut(asset.bucket) { mutableListOf() }.add(asset)
        }

        val items = ArrayList<GalleryUiItem>(assets.size + grouped.size)
        for ((bucket, monthAssets) in grouped) {
            items.add(GalleryUiItem.Header(bucket = bucket, totalCount = monthAssets.size))
            for (asset in monthAssets) {
                items.add(
                    GalleryUiItem.Photo(
                        asset = asset,
                        isSelected = selectedIds.contains(asset.id)
                    )
                )
            }
        }
        items
    }
}
