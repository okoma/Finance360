package com.i2medier.financialpro.planner.integration

import android.content.Context
import android.content.Intent
import com.i2medier.financialpro.activity.MainActivity
import com.i2medier.financialpro.planner.data.local.TransactionType

data class PlannerAddRequest(
    val amount: Double,
    val suggestedType: TransactionType,
    val note: String,
    val title: String? = null,
    val calculatorCategory: String = "all"
)

object PlannerIntegrationContract {
    const val ACTION_OPEN_PLANNER = "com.i2medier.financialpro.action.OPEN_PLANNER"
    private const val PLANNER_URI = "financialpro://planner"
    const val TAB_SUMMARY = "summary"
    const val TAB_HISTORY = "history"
    const val TAB_GOALS = "goals"
    const val TAB_BILLS = "bills"

    private const val EXTRA_OPEN_PLANNER = "planner_open"
    private const val EXTRA_TAB = "planner_tab"
    private const val EXTRA_AMOUNT = "planner_amount"
    private const val EXTRA_TYPE = "planner_type"
    private const val EXTRA_NOTE = "planner_note"
    private const val EXTRA_TITLE = "planner_title"
    private const val EXTRA_CATEGORY = "planner_category"

    fun createOpenPlannerIntent(context: Context, request: PlannerAddRequest): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLANNER
            data = android.net.Uri.parse(PLANNER_URI)
            putExtra(EXTRA_OPEN_PLANNER, true)
            putExtra(EXTRA_AMOUNT, request.amount)
            putExtra(EXTRA_TYPE, request.suggestedType.name)
            putExtra(EXTRA_NOTE, request.note)
            putExtra(EXTRA_TITLE, request.title)
            putExtra(EXTRA_CATEGORY, request.calculatorCategory)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun createOpenPlannerDeepLinkIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLANNER
            data = android.net.Uri.parse(PLANNER_URI)
            putExtra(EXTRA_OPEN_PLANNER, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun createOpenPlannerDeepLinkIntent(context: Context, tab: String): Intent {
        return createOpenPlannerDeepLinkIntent(context).apply {
            putExtra(EXTRA_TAB, tab)
        }
    }

    fun shouldOpenPlanner(intent: Intent?): Boolean {
        if (intent == null) return false
        val explicitExtra = intent.getBooleanExtra(EXTRA_OPEN_PLANNER, false)
        val actionMatch = intent.action == ACTION_OPEN_PLANNER
        val dataMatch = intent.data?.scheme == "financialpro" && intent.data?.host == "planner"
        return explicitExtra || actionMatch || dataMatch
    }

    fun getRequestFromIntent(intent: Intent?): PlannerAddRequest? {
        if (intent == null || !shouldOpenPlanner(intent)) return null
        if (!intent.hasExtra(EXTRA_AMOUNT) || !intent.hasExtra(EXTRA_TYPE)) return null

        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val type = intent.getStringExtra(EXTRA_TYPE)?.let {
            runCatching { TransactionType.valueOf(it) }.getOrNull()
        } ?: return null
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE)
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty().ifBlank { "all" }
        return PlannerAddRequest(
            amount = amount,
            suggestedType = type,
            note = note,
            title = title,
            calculatorCategory = category
        )
    }

    fun clearPlannerExtras(intent: Intent?) {
        intent?.removeExtra(EXTRA_OPEN_PLANNER)
        intent?.removeExtra(EXTRA_TAB)
        intent?.removeExtra(EXTRA_AMOUNT)
        intent?.removeExtra(EXTRA_TYPE)
        intent?.removeExtra(EXTRA_NOTE)
        intent?.removeExtra(EXTRA_TITLE)
        intent?.removeExtra(EXTRA_CATEGORY)
    }

    fun getRequestedTab(intent: Intent?): String? {
        return intent?.getStringExtra(EXTRA_TAB)
    }

    fun clearRequestedTab(intent: Intent?) {
        intent?.removeExtra(EXTRA_TAB)
    }
}
