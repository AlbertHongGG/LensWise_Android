package com.lenswise.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatterTest {

    @Test
    fun format_integerAmounts_shouldOmitDecimalPointAndZeros() {
        assertEquals("2300", AmountFormatter.format(2300.0))
        assertEquals("900", AmountFormatter.format(900.0))
        assertEquals("980", AmountFormatter.format(980.0))
        assertEquals("0", AmountFormatter.format(0.0))
        assertEquals("1000000", AmountFormatter.format(1000000.0))
    }

    @Test
    fun format_decimalAmounts_shouldPreserveValidDecimalsAndTrimTrailingZeros() {
        assertEquals("12.5", AmountFormatter.format(12.5))
        assertEquals("3.99", AmountFormatter.format(3.99))
        assertEquals("12.05", AmountFormatter.format(12.05))
        assertEquals("0.5", AmountFormatter.format(0.50))
    }

    @Test
    fun format_negativeAmounts_shouldHandleSignCorrectly() {
        assertEquals("-50", AmountFormatter.format(-50.0))
        assertEquals("-12.5", AmountFormatter.format(-12.5))
        assertEquals("-0.99", AmountFormatter.format(-0.99))
    }

    @Test
    fun formatWithCurrency_shouldFormatWithCurrencyPrefixCorrectly() {
        assertEquals("JPY 2300", AmountFormatter.formatWithCurrency("JPY", 2300.0))
        assertEquals("NT$ 145", AmountFormatter.formatWithCurrency("NT$", 145.0))
        assertEquals("$ 12.5", AmountFormatter.formatWithCurrency("$", 12.5))
        assertEquals("900", AmountFormatter.formatWithCurrency(null, 900.0))
        assertEquals("900", AmountFormatter.formatWithCurrency("", 900.0))
    }
}
