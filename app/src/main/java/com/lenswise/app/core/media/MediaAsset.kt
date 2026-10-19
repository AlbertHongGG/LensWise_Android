package com.lenswise.app.core.media

import android.net.Uri

/**
 * 相簿媒體檔案不可變領域實體 (MediaAsset)。
 *
 * @param id MediaStore 系統唯一列識別碼。
 * @param contentUri 指向該圖片的 content:// URI。
 * @param dateTakenMs 照片拍攝/建立之毫秒級時間戳。
 * @param bucket 預先歸類的月份時間桶。
 */
data class MediaAsset(
    val id: Long,
    val contentUri: Uri,
    val dateTakenMs: Long,
    val bucket: DateBucket
)
