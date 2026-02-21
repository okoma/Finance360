package com.i2medier.financialpro.util

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyManager {
    fun getCurrency(context: Context): Currency {
        val locale = Locale.getDefault()
        return runCatching { Currency.getInstance(locale) }.getOrElse { Currency.getInstance("USD") }
    }

    fun getCurrencyCode(context: Context): String = getCurrency(context).currencyCode

    fun getCurrencySymbol(context: Context): String {
        val locale = Locale.getDefault()
        return runCatching { getCurrency(context).getSymbol(locale) }.getOrDefault("$")
    }

    fun format(context: Context, amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = getCurrency(context)
        return runCatching { formatter.format(amount) }.getOrDefault("$amount")
    }
}
