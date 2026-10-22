package com.lenswise.app.features.comparison.agent

import com.lenswise.app.domain.model.comparison.ProductComparisonRecord

/**
 * 商品比對深度對話提示詞建構器 (ComparisonChatPrompts)。
 * 1:1 精確對齊 LensWise 核心 Prompt 設計。
 */
object ComparisonChatPrompts {

    fun buildSystemPrompt(result: ProductComparisonRecord): String {
        val productsContext = result.products.joinToString("\n") { p ->
            "- ${p.translatedName} (${p.originalName})"
        }

        val diffContext = result.comparisonPoints.joinToString("\n\n") { p ->
            val valuesLines = p.values.mapIndexed { idx, v -> "商品${idx + 1}: $v" }.joinToString("\n")
            "【${p.aspect}】\n$valuesLines"
        }

        return """
你是一個具備深厚專業知識的頂級商品分析與選購顧問 AI。
使用者剛剛比對了多項商品，以下是該次比對的基礎報告。

【重要指令】
當使用者針對商品差異提問時，**絕對不要只是重複或照抄報告中的字句**。
你必須運用你龐大的世界知識庫，為使用者進行「深度科普與延伸解析」。
例如：使用者問「麵體差在哪」，你不只要說出外觀或時間差異，更要解釋背後的「根本原因」（例如：製麵工法的不同、成分比例、物理化學原理、業界技術演進等）。
請扮演真正的領域專家，提供高維度的專業見解。
回答請保持結構化，並適當使用粗體來強調關鍵知識點。

【比較商品清單】
$productsContext

【AI 分析總結】
${result.summary}

【各項規格與特色差異】
$diffContext

你可以幫忙：
1. 針對使用者的特定需求 (例如保濕、CP值)，推薦最適合的商品。
2. 解釋某些規格的差異與實際影響。
3. 提供購買建議與總結。
        """.trimIndent()
    }

    fun getInitialMessage(): String {
        return "我已為您完成商品比對報告，想針對特定差異深入了解，或是需要我幫您針對情境推薦嗎？"
    }
}

