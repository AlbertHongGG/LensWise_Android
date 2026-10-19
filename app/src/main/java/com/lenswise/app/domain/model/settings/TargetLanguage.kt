package com.lenswise.app.domain.model.settings

/**
 * 目標翻譯語言列舉 (TargetLanguage)。
 * 1:1 對齊 LensWise 原版定義：
 * 封裝 UI 顯示名稱 (displayName) 與 AI 提示詞注入專用名稱 (promptName)。
 */
enum class TargetLanguage(
    val displayName: String,
    val promptName: String
) {
    TRADITIONAL_CHINESE("繁體中文", "Traditional Chinese"),
    ENGLISH("English", "English"),
    JAPANESE("日本語", "Japanese"),
    KOREAN("한국어", "Korean");

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
