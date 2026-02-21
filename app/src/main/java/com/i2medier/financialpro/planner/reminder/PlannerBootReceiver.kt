package com.i2medier.financialpro.planner.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class PlannerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            PlannerReminderManager.ensureReminderScheduledSmart(context, null)
        }
    }
}
