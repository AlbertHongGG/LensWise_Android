package com.lenswise.app.features.menu.agent

import com.lenswise.app.domain.model.menu.MenuScannerRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 菜單深度對話提示詞建構器 (MenuChatPrompts)。
 * 1:1 精確對齊 LensWise 核心 Prompt 設計。
 */
object MenuChatPrompts {

    private val json = Json { prettyPrint = true }

    fun buildSystemPrompt(record: MenuScannerRecord): String {
        val itemsJson = try {
            json.encodeToString(record.items)
        } catch (_: Exception) {
            record.items.joinToString("\n") { "- ${it.translatedName} (${it.originalName}): ${it.price}" }
        }

        return """
你是一個幽默、專業且熟悉各國飲食文化的 AI 餐點顧問。
使用者正在閱讀一份剛被翻譯過的菜單。
以下是這份菜單的結構化資料：
餐廳名稱: ${record.storeName}
餐點清單:
$itemsJson

【回答鐵律】
1. 嚴格根據上方提供的菜單資料來回答問題，如果使用者詢問了不在清單上的餐點，請誠實告知。
2. 你可以運用你龐大的美食知識，來解釋這些餐點的特色、可能含有的過敏原（如花生、海鮮等）、或是辣度等。
3. 如果使用者要求推薦，你可以根據菜名或成分推薦招牌菜色。
4. 保持對話簡潔生動。
        """.trimIndent()
    }

    fun getInitialMessage(): String {
        return "已經為您翻譯好菜單了！有任何關於餐點的疑問（例如：哪道是不辣的？哪個含有海鮮？），都可以隨時問我喔！"
    }
}

