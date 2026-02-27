package com.i2medier.financialpro

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.i2medier.financialpro.planner.reminder.PlannerReminderManager
import com.i2medier.financialpro.util.AnalyticsTracker

class FinancialProApp : Application(), Application.ActivityLifecycleCallbacks {
    private data class InitialPadding(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    override fun onCreate() {
        super.onCreate()
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCustomKey("app_id", packageName)
        }.onFailure {
            AnalyticsTracker.recordNonFatal("crashlytics_init", "init_failed", it)
        }
        runCatching {
            PlannerReminderManager.ensureReminderScheduledSmart(applicationContext, null)
        }.onFailure {
            AnalyticsTracker.recordNonFatal("reminder_init", "schedule_failed", it)
        }
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        applyEdgeToEdge(activity)
        applyDefaultInsets(activity)
    }

    private fun applyEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            activity.window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun applyDefaultInsets(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return

        // Activities with custom insets handling keep their own logic.
        if (root.id == com.i2medier.financialpro.R.id.rootMain ||
            root.id == com.i2medier.financialpro.R.id.rootGoalDetail ||
            root.id == com.i2medier.financialpro.R.id.rootBillDetail
        ) {
            return
        }

        val initialPadding = (root.getTag(R.id.tag_edge_to_edge_initial_padding) as? InitialPadding)
            ?: InitialPadding(
                left = root.paddingLeft,
                top = root.paddingTop,
                right = root.paddingRight,
                bottom = root.paddingBottom
            ).also {
                root.setTag(R.id.tag_edge_to_edge_initial_padding, it)
            }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view: View, insets: WindowInsetsCompat ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialPadding.left + bars.left,
                top = initialPadding.top + bars.top,
                right = initialPadding.right + bars.right,
                bottom = initialPadding.bottom + bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onActivityStarted(activity: Activity) {
        applyDefaultInsets(activity)
    }
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
