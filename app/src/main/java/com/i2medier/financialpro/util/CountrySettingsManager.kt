package com.i2medier.financialpro.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class CountryOption(
    val countryCode: String,
    val countryName: String,
    val currencyCode: String
)

object CountrySettingsManager {
    private const val PREFS = "country_settings"
    private const val KEY_COUNTRY = "country_code"

    private val supported = listOf(
        CountryOption("US", "United States", "USD"),
        CountryOption("GB", "United Kingdom", "GBP"),
        CountryOption("CA", "Canada", "CAD"),
        CountryOption("AU", "Australia", "AUD"),
        CountryOption("NG", "Nigeria", "NGN")
    )

    fun getSupportedCountries(): List<CountryOption> = supported

    fun getSelectedCountry(context: Context): CountryOption {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_COUNTRY, Locale.getDefault().country)
            .orEmpty()
            .uppercase()
        return supported.firstOrNull { it.countryCode == code } ?: supported.first()
    }

    fun setSelectedCountry(context: Context, option: CountryOption) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COUNTRY, option.countryCode)
            .apply()
        applySelectedLocale(context.applicationContext)
    }

    fun applySelectedLocale(context: Context) {
        val selected = getSelectedCountry(context)
        val locale = Locale("en", selected.countryCode)
        Locale.setDefault(locale)
        val appLocales = LocaleListCompat.forLanguageTags(locale.toLanguageTag())
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    fun getSelectedLocale(context: Context): Locale {
        val selected = getSelectedCountry(context)
        return Locale("en", selected.countryCode)
    }
}
