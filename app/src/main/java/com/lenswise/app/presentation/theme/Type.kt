package com.lenswise.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lenswise.app.R

// Google Font Provider 定義 (Outfit 字體族，支援繁體中文 Sans-Serif 自動回退)
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val outfitGoogleFont = GoogleFont("Outfit")

/**
 * 完整註冊 Outfit 全階字重 (Light w300 ~ Black w900)。
 * 杜絕因字重缺漏導致的系統降級回退或 Faux Bold 粗鈍現象。
 */
val OutfitFontFamily = FontFamily(
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = outfitGoogleFont, fontProvider = fontProvider, weight = FontWeight.Black)
)

/**
 * LensWise 全域排版設計代幣 (AppTypography)。
 *
 * 全面貫徹「細一階」(Step-Down-One-Tier) 設計規範：
 * 消除散裝硬編碼與魔術數字，英數字負空間更清晰，線條更加幹練輕盈。
 */
object AppTypography {
    // 1. Hero & 頁面大標題 (原 ExtraBold w800 -> 現 Bold w700)
    val heroTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 36.sp
    )

    // 2. 全大寫追蹤英文副標題 (原 Bold w700 / SemiBold w600 -> 現 Medium w500)
    val heroSubtitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
        lineHeight = 14.sp
    )

    // 3. 區塊與分組大寫追蹤標題 (原 SemiBold w600 -> 現 Medium w500)
    val sectionHeader = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp
    )

    // 4. 抽屜與確認按鈕追蹤大寫標題 (原 Bold w700 -> 現 Medium w500)
    val sheetHeader = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.0.sp
    )

    // 5. 彈窗與底層抽屜主標題 (原 Bold w700 / SemiBold w600 -> 現 SemiBold w600)
    val dialogTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )

    // 6. 卡片與紀錄項目主標題 (原 Bold w700 -> 現 SemiBold w600)
    val cardTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )

    // 7. 設定項 Tile 標題 (原 SemiBold w600 -> 現 Medium w500)
    val tileTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    )

    // 8. 欄位數值與選取項目 (原 SemiBold w600 -> 現 Medium w500)
    val itemValue = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )

    // 9. 輸入框文字 (URL、API Key、模型名) (原 SemiBold w600 -> 現 Medium w500)
    val inputText = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )

    // 10. 選單清單項目 (選中 SemiBold w600 / 未選中 Normal w400)
    val listItemSelected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    val listItemUnselected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )

    // 11. 分頁標籤文字 (選中 SemiBold w600 / 未選中 Normal w400)
    val tabSelected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    val tabUnselected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )

    // 12. 輪盤選擇器 (選中 Bold w700 / 未選中 Normal w400)
    val wheelSelected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.0.sp
    )
    val wheelUnselected = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.0.sp
    )

    // 13. 收據票面專用排版
    val receiptStoreTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    )
    val receiptTableHeader = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
    val receiptTotalLabel = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    val receiptTotalAmount = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    // 14. 提示訊息與空狀態
    val toastMessage = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    )
    val emptyStateTitle = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
    val emptyStateDesc = TextStyle(
        fontFamily = OutfitFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    )
}

/**
 * Material 3 語意化字型排版規範。
 * 全面套用「細一階」字重映射。
 */
val LensWiseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold, // 原 ExtraBold
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold, // 原 Bold
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold, // 原 Bold
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium, // 原 SemiBold
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium, // 原 SemiBold
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal, // 原 Medium
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium, // 原 SemiBold
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium, // 原 SemiBold
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold, // 原 Bold
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
