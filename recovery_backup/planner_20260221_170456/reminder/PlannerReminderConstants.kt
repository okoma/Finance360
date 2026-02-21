package com.i2medier.financialpro.planner.reminder

object PlannerReminderConstants {
    const val PREFS_NAME = "planner_prefs"
    const val KEY_REMINDER_ENABLED = "reminder_enabled"
    const val KEY_GOAL_REMINDER_ENABLED = "goal_reminder_enabled"
    const val KEY_BILL_REMINDER_ENABLED = "bill_reminder_enabled"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    const val KEY_WEEKDAY_REMINDER_HOUR = "weekday_reminder_hour"
    const val KEY_WEEKDAY_REMINDER_MINUTE = "weekday_reminder_minute"
    const val KEY_WEEKEND_REMINDER_HOUR = "weekend_reminder_hour"
    const val KEY_WEEKEND_REMINDER_MINUTE = "weekend_reminder_minute"
    const val KEY_COOLDOWN_HOURS = "reminder_cooldown_hours"
    const val KEY_LAST_NOTIFICATION_AT = "last_notification_at"
    const val KEY_LAST_BILL_NOTIFICATION_AT = "last_bill_notification_at"
    const val KEY_LAST_STREAK_NOTIFICATION_DATE = "last_streak_notification_date"
    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0
    const val DEFAULT_WEEKDAY_HOUR = 20
    const val DEFAULT_WEEKDAY_MINUTE = 0
    const val DEFAULT_WEEKEND_HOUR = 10
    const val DEFAULT_WEEKEND_MINUTE = 0
    const val DEFAULT_COOLDOWN_HOURS = 20L
    const val REMINDER_REQUEST_CODE = 1001
    const val REMINDER_SNOOZE_REQUEST_CODE = 1002
    const val NOTIFICATION_ID = 2001
    const val BILL_NOTIFICATION_ID = 2002
    const val CHANNEL_ID_DEFAULT = "saving_reminders_default"
    const val CHANNEL_ID_HIGH = "saving_reminders_high"
}
