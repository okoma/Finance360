package com.i2medier.financialpro.planner.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class PlannerReminderScheduler(private val context: Context) {
    fun scheduleReminder(hourOfDay: Int, minute: Int): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(
            requestCode = PlannerReminderConstants.REMINDER_REQUEST_CODE,
            isSnoozed = false
        )
        val triggerTime = nextTriggerAt(hourOfDay, minute)

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        return canScheduleExact
    }

    fun scheduleSnoozeReminder(delayMillis: Long = 2L * 60L * 60L * 1000L): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(
            requestCode = PlannerReminderConstants.REMINDER_SNOOZE_REQUEST_CODE,
            isSnoozed = true
        )
        val triggerTime = System.currentTimeMillis() + delayMillis

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        return true
    }

    fun cancelReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(
            reminderPendingIntent(
                requestCode = PlannerReminderConstants.REMINDER_REQUEST_CODE,
                isSnoozed = false
            )
        )
        alarmManager.cancel(
            reminderPendingIntent(
                requestCode = PlannerReminderConstants.REMINDER_SNOOZE_REQUEST_CODE,
                isSnoozed = true
            )
        )
    }

    private fun reminderPendingIntent(requestCode: Int, isSnoozed: Boolean): PendingIntent {
        val intent = Intent(context, PlannerReminderReceiver::class.java).apply {
            putExtra(PlannerReminderReceiver.EXTRA_IS_SNOOZE, isSnoozed)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAt(hourOfDay: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.timeInMillis
    }
}
