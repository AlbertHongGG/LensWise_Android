package com.lenswise.app.domain.model.settings

/**
 * 目標翻譯語言列舉 (TargetLanguage)。
 * 1:1 對齊 LensWise 原版定義：
 * 封裝 UI 顯示名稱 (displayName)、AI 提示詞注入名稱 (promptName) 與標準 BCP-47 語言代碼 (code / languageCode)。
 */
enum class TargetLanguage(
    val displayName: String,
    val promptName: String,
    val languageCode: String
) {
    TRADITIONAL_CHINESE("繁體中文", "Traditional Chinese", "zh-TW"),
    ENGLISH("English", "English", "en"),
    JAPANESE("日本語", "Japanese", "ja"),
    KOREAN("한국어", "Korean", "ko");

    val code: String get() = languageCode

    companion object {
        /**
         * 依據 UI 顯示名稱解析語言，解析失敗時預設回傳繁體中文
         */
        fun fromDisplayName(name: String): TargetLanguage {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                ?: TRADITIONAL_CHINESE
        }

        /**
         * 依據 enum name 或字串解析語言，解析失敗時預設回傳繁體中文
         */
        fun fromString(name: String): TargetLanguage {
            return entries.firstOrNull { 
                it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true)
            } ?: TRADITIONAL_CHINESE
        }
    }
}
