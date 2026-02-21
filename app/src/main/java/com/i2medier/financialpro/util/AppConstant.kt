package com.i2medier.financialpro.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object AppConstant {
    @JvmField
    var DISCLOSURE_DIALOG_DESC: String =
        "We would like to inform you regarding the 'Consent to Collection and Use Of Data'\n\nTo share result of calculation image, allow storage permission.\n\nWe store your data on your device only, we don’t store them on our server."

    @JvmField
    var PRIVACY_POLICY_URL: String = "https://www.google.com"

    @JvmField
    var TERMS_OF_SERVICE_URL: String = "https://www.google.com"

    @JvmField
    var showDatePatternDDMMYYYY: String = "dd-MM-yyyy"

    @JvmField
    val Date_FoRMAT_DDMMYY: DateFormat = SimpleDateFormat(showDatePatternDDMMYYYY)

    @SuppressLint("WrongConstant")
    @JvmStatic
    fun openUrl(context: Context, str: String) {
        val intent = Intent("android.intent.action.VIEW", Uri.parse(str))
        intent.addFlags(1208483840)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(PRIVACY_POLICY_URL)
                )
            )
        }
    }

    @JvmStatic
    fun getFormattedDate(j: Long, dateFormat: DateFormat): String {
        return dateFormat.format(Date(j))
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        try {
            val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            var currentFocus = activity.currentFocus
            if (currentFocus == null) {
                currentFocus = View(activity)
            }
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun visibleResult(cardView: CardView) {
        cardView.visibility = View.VISIBLE
    }

    @JvmStatic
    fun visibleGraph(linearLayout: LinearLayout) {
        linearLayout.visibility = View.VISIBLE
    }
}
