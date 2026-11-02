package com.lenswise.app.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 媒體相簿權限與系統存取能力管理器 (MediaAccessManager)。
 *
 * 核心保證：
 * 1. 統一封裝 Android 14+ (API 34+) 部分相片存取 (READ_MEDIA_VISUAL_USER_SELECTED)、
 *    Android 13 (API 33) READ_MEDIA_IMAGES 與 API 26-32 READ_EXTERNAL_STORAGE。
 * 2. 徹底消除 UI 層與具體 Android Manifest 權限字串之耦合。
 * 3. 物件化判斷授權狀態，杜絕因部分授權產生的拒絕/無窮請求循環。
 */
object MediaAccessManager {

    /**
     * 取得當前系統版本所需要請求的權限陣列。
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * 檢查當前 Context 是否已具備相簿讀取權限（完整存取或部分存取皆視為具備存取能力）。
     */
    fun hasPermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val partialGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
                fullGranted || partialGranted
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * 根據 RequestMultiplePermissions 的回傳結果 Map，判定是否具備存取能力。
     */
    fun isPermissionGranted(resultMap: Map<String, Boolean>): Boolean {
        if (resultMap.isEmpty()) return false
        // 任何一項相簿存取權限為 true 即可進入相簿（適配 Android 14+ 部分選取）
        return resultMap.values.any { it }
    }
}
