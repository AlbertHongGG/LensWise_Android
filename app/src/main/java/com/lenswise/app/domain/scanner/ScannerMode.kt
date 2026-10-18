package com.lenswise.app.domain.scanner

enum class ScannerMode(
    val title: String,
    val actionTitle: String,
    val minShots: Int
) {
    RECEIPT("收據", "開始解析", 1),
    PRODUCT("商品", "開始分析", 1),
    COMPARISON("比對", "開始比對", 2),
    MENU("菜單", "辨識菜單", 1);
}
