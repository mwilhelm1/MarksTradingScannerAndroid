package com.example.markstradingscanner

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale.US,
    ).format(value)
}

fun formatSignedCurrency(value: Double): String {
    val formatted = formatCurrency(abs(value))

    return when {
        value > 0 -> "+$formatted"
        value < 0 -> "-$formatted"
        else -> formatted
    }
}

fun formatPercent(value: Double): String {
    return "${value.format(2)}%"
}

fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.format(4)
    }
}

fun displayText(value: String): String {
    return value
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar {
            it.titlecase(Locale.US)
        }
}

fun Double.format(decimals: Int): String {
    return String.format(
        Locale.US,
        "%.${decimals}f",
        this,
    )
}