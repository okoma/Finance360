package com.i2medier.financialpro.util

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyManager {
    fun getCurrency(context: Context): Currency {
        val locale = CountrySettingsManager.getSelectedLocale(context)
        return runCatching { Currency.getInstance(locale) }.getOrElse { Currency.getInstance("USD") }
    }

    fun getCurrencyCode(context: Context): String = getCurrency(context).currencyCode

    fun getCurrencySymbol(context: Context): String {
        val locale = CountrySettingsManager.getSelectedLocale(context)
        return runCatching { getCurrency(context).getSymbol(locale) }.getOrDefault("$")
    }

    fun format(context: Context, amount: Double): String {
        val locale = CountrySettingsManager.getSelectedLocale(context)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = getCurrency(context)
        return runCatching { formatter.format(amount) }.getOrDefault("$amount")
    }
}
