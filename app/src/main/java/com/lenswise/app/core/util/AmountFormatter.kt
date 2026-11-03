package com.lenswise.app.core.util

import java.util.Locale

/**
 * 智慧金額格式化工具 (AmountFormatter)。
 *
 * 核心保證：
 * 1. 消除冗餘小數點：整數金額 (如 2300.0, 900.0) 格式化為純整數 "2300", "900"，絕不強加 ".00"。
 * 2. 保留真實小數位：含有小數金額 (如 12.5, 3.99) 精確保留實際小數位並自動剔除末尾無效的 0。
 * 3. 符號與負數保證：正確處理 0.0 ("0") 與負數金額 (如 -50.0 -> "-50")。
 */
object AmountFormatter {

    fun format(amount: Double): String {
        val longVal = amount.toLong()
        return if (amount == longVal.toDouble()) {
            longVal.toString()
        } else {
            val formatted = String.format(Locale.US, "%.2f", amount)
            formatted.trimEnd('0').trimEnd('.')
        }
    }

    fun formatWithCurrency(currency: String?, amount: Double): String {
        val curr = if (!currency.isNullOrBlank()) "$currency " else ""
        return "$curr${format(amount)}"
    }
}
