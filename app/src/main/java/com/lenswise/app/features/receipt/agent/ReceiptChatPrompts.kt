package com.lenswise.app.features.receipt.agent

import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord

/**
 * 收據專屬對話顧問 Agent 提示詞工廠。
 * 內容完全嚴格對標參考專案 (LensWise)。
 */
object ReceiptChatPrompts {

    fun buildSystemPrompt(record: ReceiptScannerRecord): String {
        val itemsStr = record.items.joinToString("\n") { e ->
            if (e.translatedName.isNotBlank() && e.translatedName != e.originalName) {
                "- ${e.originalName} / ${e.translatedName}: ${e.price}"
            } else {
                "- ${e.originalName}: ${e.price}"
            }
        }
        val discountsStr = record.discounts.joinToString("\n") { e ->
            "- ${e.name}: ${e.amount}"
        }
        val taxesStr = record.taxes.joinToString("\n") { e ->
            "- ${e.name}: ${e.amount}"
        }

        val discountsBlock = if (discountsStr.isNotEmpty()) "【折扣】\n$discountsStr\n" else ""
        val taxesBlock = if (taxesStr.isNotEmpty()) "【稅金】\n$taxesStr\n" else ""

        val storeName = record.storeName.ifBlank { "未知" }
        val receiptDate = record.receiptDate ?: "未知"
        val currency = record.currency ?: "未知"

        return """你是一個具備深厚專業知識的頂級帳務分析與消費顧問 AI。
使用者剛剛掃描了一張收據。請根據以下收據資訊，回答使用者的所有問題。

【重要指令】
當使用者針對收據上的商品、消費性質或理財提問時，絕對不要只是重複清單上的字句。
你必須運用你龐大的世界知識庫，為使用者進行「深度科普與延伸解析」。
例如：如果使用者詢問某項翻譯後的商品是什麼，你應該去解釋該商品的用途、成分、特色，甚至它是哪個地區的特產。
如果使用者詢問記帳或理財建議，你應該提供高維度的帳務整理或節流策略。
請扮演真正的領域專家，提供高維度的專業見解。
回答請保持結構化，並適當使用粗體來強調關鍵知識點。

【收據資訊】
- 商店名稱：$storeName
- 消費日期：$receiptDate
- 幣別：$currency
- 小計：${record.subtotal}
- 總金額：${record.totalAmount}

【購買品項】
$itemsStr

$discountsBlock$taxesBlock
你可以幫忙：
1. 分析這筆消費的性質。
2. 計算特定人員的平分金額或 AA 制分攤。
3. 解釋購買品項的詳細內容與商品用途。"""
    }

    fun getInitialMessage(): String {
        return "我已為您解析完這張收據，想進行帳務整理、計算 AA 制，或了解單一商品的用途嗎？"
    }
}

