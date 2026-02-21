package com.i2medier.financialpro.planner.reminder

import android.content.Context
import com.i2medier.financialpro.planner.data.local.StreakEntity

object PlannerReminderManager {
    private fun isReminderEngineEnabled(prefs: android.content.SharedPreferences): Boolean {
        val savingEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
        val goalEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_GOAL_REMINDER_ENABLED, true)
        val billEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_BILL_REMINDER_ENABLED, true)
        return savingEnabled || goalEnabled || billEnabled
    }

    fun ensureReminderScheduled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        if (!isReminderEngineEnabled(prefs)) return true

        val hour = prefs.getInt(
            PlannerReminderConstants.KEY_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_HOUR
        )
        val minute = prefs.getInt(
            PlannerReminderConstants.KEY_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_MINUTE
        )
        return PlannerReminderScheduler(context).scheduleReminder(hour, minute)
    }

    fun ensureReminderScheduledSmart(context: Context, streak: StreakEntity? = null): Boolean {
        val reminderTime = PlannerReminderPolicy.resolveReminderTime(context, streak)
        return PlannerReminderScheduler(context).scheduleReminder(reminderTime.hour, reminderTime.minute)
    }

    fun updateReminderTime(context: Context, hour: Int, minute: Int): Boolean {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
            .putInt(PlannerReminderConstants.KEY_REMINDER_HOUR, hour)
            .putInt(PlannerReminderConstants.KEY_REMINDER_MINUTE, minute)
            .putInt(PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR, hour)
            .putInt(PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE, minute)
            .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR, hour)
            .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE, minute)
            .apply()
        return PlannerReminderScheduler(context).scheduleReminder(hour, minute)
    }

    fun updateReminderSchedule(
        context: Context,
        weekdayHour: Int,
        weekdayMinute: Int,
        weekendHour: Int,
        weekendMinute: Int
    ): Boolean {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
            .putInt(PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR, weekdayHour)
            .putInt(PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE, weekdayMinute)
            .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR, weekendHour)
            .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE, weekendMinute)
            .putInt(PlannerReminderConstants.KEY_REMINDER_HOUR, weekdayHour)
            .putInt(PlannerReminderConstants.KEY_REMINDER_MINUTE, weekdayMinute)
            .apply()

        val reminderTime = PlannerReminderPolicy.resolveReminderTime(context, null)
        return PlannerReminderScheduler(context).scheduleReminder(reminderTime.hour, reminderTime.minute)
    }

    fun disableReminder(context: Context) {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, false)
            .apply()

        PlannerReminderScheduler(context).cancelReminder()
    }
}
