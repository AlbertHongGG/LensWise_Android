package com.lenswise.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.lenswise.app.domain.scanner.ScannerMode

/**
 * 掃描器偏好設定本地持久化倉庫 (ScannerPreferencesRepository)。
 *
 * 實作 [ScannerPreferencesDataSource]，透過 Android [SharedPreferences] 進行本地鍵值存取。
 *
 * 核心保證：
 * 1. 記憶使用者選擇之 ScannerMode，杜絕進程重啟或頁面重建時重置。
 * 2. 具備容錯機制，若枚舉解析失敗安全退回預設 [ScannerMode.PRODUCT]。
 * 3. 支援純 SharedPreferences 依賴注入，兼顧可測試性與執行期效率。
 */
class ScannerPreferencesRepository(
    private val prefs: SharedPreferences
) : ScannerPreferencesDataSource {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun getSavedMode(): ScannerMode {
        val savedName = prefs.getString(KEY_CURRENT_MODE, null) ?: return ScannerMode.PRODUCT
        return runCatching { ScannerMode.valueOf(savedName) }.getOrDefault(ScannerMode.PRODUCT)
    }

    override fun saveMode(mode: ScannerMode) {
        prefs.edit().putString(KEY_CURRENT_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "lenswise_scanner_prefs"
        private const val KEY_CURRENT_MODE = "saved_scanner_mode"
    }
}
