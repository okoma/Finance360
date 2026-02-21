package com.i2medier.financialpro.util

import android.annotation.SuppressLint
import android.content.Context

object AppPref {
    private const val IS_RATEUS = "IS_RATEUS"
    private const val IS_TERMS_ACCEPT = "IS_TERMS_ACCEPT"
    private const val MY_PREF = "userPref"

    @JvmStatic
    fun IsTermsAccept(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(MY_PREF, 0)
            .getBoolean(IS_TERMS_ACCEPT, false)
    }

    @SuppressLint("ApplySharedPref")
    @JvmStatic
    fun setIsTermsAccept(context: Context, accepted: Boolean) {
        val edit = context.applicationContext.getSharedPreferences(MY_PREF, 0).edit()
        edit.putBoolean(IS_TERMS_ACCEPT, accepted)
        edit.commit()
    }

    @JvmStatic
    fun IsRateUS(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(MY_PREF, 0)
            .getBoolean(IS_RATEUS, false)
    }

    @JvmStatic
    fun setIsRateUS(context: Context, isRateUS: Boolean) {
        val edit = context.applicationContext.getSharedPreferences(MY_PREF, 0).edit()
        edit.putBoolean(IS_RATEUS, isRateUS)
        edit.commit()
    }
}
