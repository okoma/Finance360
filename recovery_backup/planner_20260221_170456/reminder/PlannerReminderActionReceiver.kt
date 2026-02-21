package com.i2medier.financialpro.planner.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class PlannerReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_REMIND_LATER -> {
                PlannerReminderScheduler(context).scheduleSnoozeReminder()
            }
        }
    }
    companion object {
        const val ACTION_REMIND_LATER = "com.i2medier.financialpro.planner.reminder.ACTION_REMIND_LATER"
}
