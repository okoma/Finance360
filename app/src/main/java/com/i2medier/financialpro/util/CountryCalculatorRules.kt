package com.i2medier.financialpro.util

import com.i2medier.financialpro.activity.BondYieldCalculatorActivity
import com.i2medier.financialpro.activity.TaxCalculatorActivity
object CountryCalculatorRules {
    private val hiddenByCountry: Map<String, Set<Class<*>>> = mapOf(
        // Country-driven feature configs. Keep all supported countries explicit,
        // even when they share the same visible set.
        "US" to emptySet(),
        "GB" to emptySet(),
        "AU" to emptySet(),
        "CA" to emptySet(),
        "NG" to setOf(
            TaxCalculatorActivity::class.java,
            BondYieldCalculatorActivity::class.java
        )
    )
    fun isVisible(countryCode: String, activityClass: Class<*>): Boolean {
        val hidden = hiddenByCountry[countryCode].orEmpty()
        return activityClass !in hidden
    }
}
