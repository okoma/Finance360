package com.i2medier.financialpro.planner.reminder

import android.content.Context
import com.i2medier.financialpro.planner.data.local.StreakEntity
import com.i2medier.financialpro.planner.domain.daysBetweenUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import java.util.Calendar
import java.util.TimeZone

data class ReminderTime(val hour: Int, val minute: Int)

object PlannerReminderPolicy {
    fun resolveReminderTime(context: Context, streak: StreakEntity? = null): ReminderTime {
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val now = Calendar.getInstance(TimeZone.getDefault())
        val isWeekend = now.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        val baseHour = if (isWeekend) {
            prefs.getInt(
                PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR,
                PlannerReminderConstants.DEFAULT_WEEKEND_HOUR
            )
        } else {
            prefs.getInt(
                PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR,
                PlannerReminderConstants.DEFAULT_WEEKDAY_HOUR
            )
        }

        val baseMinute = if (isWeekend) {
            prefs.getInt(
                PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE,
                PlannerReminderConstants.DEFAULT_WEEKEND_MINUTE
            )
        } else {
            prefs.getInt(
                PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE,
                PlannerReminderConstants.DEFAULT_WEEKDAY_MINUTE
            )
        }

        val yesterday = System.currentTimeMillis().toUtcMidnight() - DAY_IN_MILLIS
        val missedRecently = streak == null || streak.lastSaveDate < yesterday
        val adjustedHour = if (missedRecently) (baseHour - 1).coerceAtLeast(7) else baseHour

        return ReminderTime(hour = adjustedHour, minute = baseMinute)
    }

    fun shouldNotifyNow(
        context: Context,
        streak: StreakEntity?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!passesMissedDayCadence(streak, nowMillis)) return false

        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val cooldownHours = prefs.getLong(
            PlannerReminderConstants.KEY_COOLDOWN_HOURS,
            PlannerReminderConstants.DEFAULT_COOLDOWN_HOURS
        )
        val lastAt = prefs.getLong(PlannerReminderConstants.KEY_LAST_NOTIFICATION_AT, 0L)
        if (lastAt <= 0L) return true
        return nowMillis - lastAt >= cooldownHours * 60L * 60L * 1000L
    }

    fun markNotified(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putLong(PlannerReminderConstants.KEY_LAST_NOTIFICATION_AT, nowMillis)
            .apply()
    }

    fun shouldNotifyBillNow(
        context: Context,
        overdueDays: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val lastAt = prefs.getLong(PlannerReminderConstants.KEY_LAST_BILL_NOTIFICATION_AT, 0L)
        val cooldownHours = if (overdueDays >= 3) {
            48L
        } else {
            prefs.getLong(
                PlannerReminderConstants.KEY_COOLDOWN_HOURS,
                PlannerReminderConstants.DEFAULT_COOLDOWN_HOURS
            )
        }
        if (lastAt <= 0L) return true
        return nowMillis - lastAt >= cooldownHours * 60L * 60L * 1000L
    }

    fun markBillNotified(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putLong(PlannerReminderConstants.KEY_LAST_BILL_NOTIFICATION_AT, nowMillis)
            .apply()
    }

    fun shouldNotifyStreakToday(
        context: Context,
        todayUtcMidnight: Long,
        isSnooze: Boolean
    ): Boolean {
        if (isSnooze) return true
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val lastDate = prefs.getLong(PlannerReminderConstants.KEY_LAST_STREAK_NOTIFICATION_DATE, 0L)
        return lastDate != todayUtcMidnight
    }

    fun markStreakNotified(
        context: Context,
        todayUtcMidnight: Long
    ) {
        context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putLong(PlannerReminderConstants.KEY_LAST_STREAK_NOTIFICATION_DATE, todayUtcMidnight)
            .putLong(PlannerReminderConstants.KEY_LAST_NOTIFICATION_AT, System.currentTimeMillis())
            .apply()
    }

    private fun passesMissedDayCadence(streak: StreakEntity?, nowMillis: Long): Boolean {
        if (streak == null) return true
        val daysSinceLastSave = daysBetweenUtc(streak.lastSaveDate, nowMillis.toUtcMidnight())
        return when {
            daysSinceLastSave <= 2 -> true
            daysSinceLastSave in 3..7 -> daysSinceLastSave % 2 == 0
            else -> daysSinceLastSave % 7 == 0
        }
    }

    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
}
