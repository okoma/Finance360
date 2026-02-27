package com.i2medier.financialpro.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.Locale

object AnalyticsTracker {
    private const val KEY_CALCULATOR = "calculator"
    private const val KEY_SOURCE = "source"
    private const val KEY_ACTION = "action"
    private const val KEY_TAB = "tab"
    private const val KEY_TYPE = "type"
    private const val KEY_CATEGORY = "category"
    private const val KEY_STATUS = "status"

    fun logNavigationTab(context: Context, tab: String) {
        logEvent(context, "nav_tab_selected", bundleOf(KEY_TAB to sanitize(tab)))
    }

    fun logCalculatorOpened(context: Context, calculatorName: String, source: String) {
        logEvent(
            context,
            "calculator_open",
            bundleOf(
                KEY_CALCULATOR to sanitize(calculatorName),
                KEY_SOURCE to sanitize(source)
            )
        )
    }

    fun logCalculatorCalculated(context: Context, calculatorName: String) {
        logEvent(
            context,
            "calculator_calculate",
            bundleOf(KEY_CALCULATOR to sanitize(calculatorName))
        )
    }

    fun logCalculatorAddToPlanner(context: Context, calculatorName: String, status: String) {
        logEvent(
            context,
            "calculator_add_to_planner",
            bundleOf(
                KEY_CALCULATOR to sanitize(calculatorName),
                KEY_STATUS to sanitize(status)
            )
        )
    }

    fun logPlannerAction(context: Context, action: String, source: String? = null) {
        val params = Bundle().apply {
            putString(KEY_ACTION, sanitize(action))
            if (!source.isNullOrBlank()) putString(KEY_SOURCE, sanitize(source))
        }
        logEvent(context, "planner_action", params)
    }

    fun logPlannerTabSelected(context: Context, tab: String) {
        logEvent(context, "planner_tab_selected", bundleOf(KEY_TAB to sanitize(tab)))
    }

    fun logPlannerTransactionSaved(context: Context, type: String, category: String, source: String) {
        logEvent(
            context,
            "planner_tx_saved",
            bundleOf(
                KEY_TYPE to sanitize(type),
                KEY_CATEGORY to sanitize(category),
                KEY_SOURCE to sanitize(source)
            )
        )
    }

    fun logPlannerGoalSaved(context: Context, source: String, isEdit: Boolean) {
        logEvent(
            context,
            "planner_goal_saved",
            bundleOf(
                KEY_SOURCE to sanitize(source),
                KEY_ACTION to if (isEdit) "edit" else "create"
            )
        )
    }

    fun logPlannerBillSaved(context: Context, isEdit: Boolean) {
        logEvent(
            context,
            "planner_bill_saved",
            bundleOf(KEY_ACTION to if (isEdit) "edit" else "create")
        )
    }

    fun recordNonFatal(tag: String, message: String, throwable: Throwable? = null) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey("tag", sanitize(tag))
        crashlytics.log("$tag: $message")
        if (throwable != null) {
            crashlytics.recordException(throwable)
        }
    }

    private fun logEvent(context: Context, name: String, params: Bundle) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(name, params)
        }.onFailure {
            recordNonFatal("analytics_event_failed", "event=$name", it)
        }
    }

    private fun bundleOf(vararg entries: Pair<String, String>): Bundle {
        return Bundle().apply {
            entries.forEach { (key, value) -> putString(key, value) }
        }
    }

    private fun sanitize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "unknown" }
    }
}
