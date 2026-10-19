package com.lenswise.app.core.storage

import com.lenswise.app.domain.scanner.ScannerMode

/**
 * 掃描器偏好設定資料來源契約介面 (ScannerPreferencesDataSource)。
 *
 * 遵循介面隔離原則 (ISP) 與依賴反轉原則 (DIP)，
 * 使 ViewModel 僅依賴資料契約，與底層具體儲存技術 (SharedPreferences, DataStore, Room) 解耦。
 */
interface ScannerPreferencesDataSource {

    /**
     * 讀取前次持久化保存之掃描模式，預設為 [ScannerMode.PRODUCT]。
     */
    fun getSavedMode(): ScannerMode

    /**
     * 保存目前選擇之掃描模式。
     */
    fun saveMode(mode: ScannerMode)
}
