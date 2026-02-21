package com.i2medier.financialpro.planner.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerAddRequest
import com.i2medier.financialpro.ui.CalculatorRegistry
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlannerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PlannerDatabase.getInstance(context)
            val txDao = db.transactionDao()
            val streakDao = db.streakDao()
            val billDao = db.billDao()
            val helper = PlannerReminderNotificationHelper(context)

            val today = System.currentTimeMillis().let { it - (it % DAY_IN_MILLIS) }
            val isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) == true
            val streak = streakDao.get()

            val prefs = context.getSharedPreferences(
                PlannerReminderConstants.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val savingEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
            val billEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_BILL_REMINDER_ENABLED, true)

            if (billEnabled) {
                val dueBills = billDao.getUnpaidDueUntil(today + DAY_IN_MILLIS)
                val due = dueBills.firstOrNull()
                if (due != null && (isSnooze || PlannerReminderPolicy.shouldNotifyBillNow(context, 0))) {
                    helper.showReminder(
                        message = context.getString(R.string.planner_reminder_bill_due_today, due.title),
                        urgency = PlannerReminderNotificationHelper.Urgency.HIGH,
                        notificationId = PlannerReminderConstants.BILL_NOTIFICATION_ID
                    )
                    PlannerReminderPolicy.markBillNotified(context)
                    PlannerReminderManager.ensureReminderScheduledSmart(context, streak)
                    return@launch
                }
            }

            if (savingEnabled) {
                val hasSavedToday = txDao.getSavingForDate(today) != null
                if (!hasSavedToday && (isSnooze || PlannerReminderPolicy.shouldNotifyNow(context, streak))) {
                    helper.showReminder(
                        message = context.getString(
                            R.string.planner_reminder_daily_nudge,
                            CurrencyManager.format(context, 500.0)
                        ),
                        primaryRequest = buildSavingRequest(
                            amount = 500.0,
                            title = context.getString(R.string.planner_notification_title),
                            note = context.getString(R.string.planner_notification_daily_note)
                        ),
                        secondaryAction = PlannerReminderNotificationHelper.SecondaryAction.REMIND_LATER
                    )
                    PlannerReminderPolicy.markNotified(context)
                }
            }

            PlannerReminderManager.ensureReminderScheduledSmart(context, streak)
        }
    }

    private fun buildSavingRequest(
        amount: Double,
        title: String?,
        note: String
    ): PlannerAddRequest {
        return PlannerAddRequest(
            amount = amount,
            suggestedType = TransactionType.SAVING,
            note = note,
            title = title,
            calculatorCategory = CalculatorRegistry.CATEGORY_SAVINGS
        )
    }

    companion object {
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
