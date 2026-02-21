package com.i2medier.financialpro.planner.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.daysBetweenUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.integration.PlannerAddRequest
import com.i2medier.financialpro.ui.CalculatorRegistry
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class PlannerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = PlannerDatabase.getInstance(context)
            val txDao = database.transactionDao()
            val streakDao = database.streakDao()
            val goalDao = database.goalDao()
            val billDao = database.billDao()
            val notificationHelper = PlannerReminderNotificationHelper(context)
            val todayMidnight = System.currentTimeMillis().toUtcMidnight()
            val yesterdayMidnight = todayMidnight - DAY_IN_MILLIS
            val tomorrowMidnight = todayMidnight + DAY_IN_MILLIS
            val dayAfterTomorrowMidnight = tomorrowMidnight + DAY_IN_MILLIS
            val isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) == true
            val hasSavedToday = txDao.getSavingForDate(todayMidnight) != null
            val hasSavedYesterday = txDao.getSavingForDate(yesterdayMidnight) != null
            val prefs = context.getSharedPreferences(
                PlannerReminderConstants.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val savingReminderEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
            val goalReminderEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_GOAL_REMINDER_ENABLED, true)
            val billReminderEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_BILL_REMINDER_ENABLED, true)
            val streak = streakDao.get()
            if (billReminderEnabled) {
                val dueBills = billDao.getUnpaidDueUntil(dayAfterTomorrowMidnight)
                val billReminder = buildBillReminder(context, dueBills, todayMidnight, tomorrowMidnight)
                if (billReminder != null &&
                    (isSnooze || PlannerReminderPolicy.shouldNotifyBillNow(
                        context = context,
                        overdueDays = billReminder.overdueDays
                    ))
                ) {
                    notificationHelper.showReminder(
                        message = billReminder.message,
                        urgency = billReminder.urgency,
                        notificationId = PlannerReminderConstants.BILL_NOTIFICATION_ID
                    )
                    PlannerReminderPolicy.markBillNotified(context)
                    PlannerReminderManager.ensureReminderScheduledSmart(context, streak)
                    return@launch
                }
            }
            if ((savingReminderEnabled || goalReminderEnabled) && !hasSavedToday) {
                val daysSinceLastSave = streak?.let { daysBetweenUtc(it.lastSaveDate, todayMidnight) } ?: Int.MAX_VALUE
                if (savingReminderEnabled &&
                    streak != null &&
                    hasSavedYesterday &&
                    PlannerReminderPolicy.shouldNotifyStreakToday(context, todayMidnight, isSnooze)
                        message = context.getString(
                            R.string.planner_reminder_streak_protection,
                            streak.currentStreak,
                            CurrencyManager.format(context, 500.0)
                        ),
                        urgency = PlannerReminderNotificationHelper.Urgency.HIGH,
                        primaryLabel = context.getString(R.string.planner_action_add_saving),
                        primaryRequest = buildSavingRequest(
                            context = context,
                            amount = 500.0,
                            title = context.getString(R.string.planner_notification_title),
                            note = context.getString(R.string.planner_notification_streak_note)
                        secondaryAction = PlannerReminderNotificationHelper.SecondaryAction.REMIND_LATER
                    PlannerReminderPolicy.markStreakNotified(context, todayMidnight)
                    daysSinceLastSave >= 3 &&
                    (isSnooze || PlannerReminderPolicy.shouldNotifyNow(context, streak))
                        message = context.getString(R.string.planner_reminder_comeback),
                        urgency = PlannerReminderNotificationHelper.Urgency.DEFAULT,
                        primaryLabel = context.getString(R.string.planner_action_start_saving),
                            note = context.getString(R.string.planner_notification_comeback_note)
                    PlannerReminderPolicy.markNotified(context)
                if (goalReminderEnabled) {
                    val goals = goalDao.getWithTargetDate()
                    val savedAmountByGoalId = goals.associate { goal ->
                        goal.id to (txDao.getTotalSavingForGoal(goal.id) ?: 0.0)
                    }
                    val goalReminder = buildGoalReminder(context, goals, todayMidnight, savedAmountByGoalId)
                    if (goalReminder != null &&
                        (isSnooze || PlannerReminderPolicy.shouldNotifyNow(context, streak))
                    ) {
                        notificationHelper.showReminder(
                            message = goalReminder.message,
                            urgency = PlannerReminderNotificationHelper.Urgency.DEFAULT,
                            primaryLabel = context.getString(R.string.planner_action_add_to_goal, goalReminder.goalTitle),
                            primaryRequest = buildSavingRequest(
                                context = context,
                                amount = goalReminder.suggestedAmount,
                                title = goalReminder.goalTitle,
                                note = context.getString(R.string.planner_goal_contribution_note, goalReminder.goalTitle)
                            ),
                            secondaryAction = PlannerReminderNotificationHelper.SecondaryAction.VIEW_PLANNER,
                            secondaryLabel = context.getString(R.string.planner_action_view_goal)
                        )
                        PlannerReminderPolicy.markNotified(context)
                        PlannerReminderManager.ensureReminderScheduledSmart(context, streak)
                        return@launch
                if (savingReminderEnabled && (isSnooze || PlannerReminderPolicy.shouldNotifyNow(context, streak))) {
                    val hasActiveGoal = goals.isNotEmpty()
                    val message = if (hasActiveGoal) {
                        context.getString(R.string.planner_reminder_daily_nudge_goal, CurrencyManager.format(context, 500.0))
                    } else {
                        context.getString(R.string.planner_reminder_daily_nudge, CurrencyManager.format(context, 500.0))
                        message = message,
                        notificationId = PlannerReminderConstants.NOTIFICATION_ID,
                        primaryLabel = context.getString(R.string.planner_action_save_now),
                            note = context.getString(R.string.planner_notification_daily_note)
                        secondaryAction = PlannerReminderNotificationHelper.SecondaryAction.VIEW_PLANNER
            PlannerReminderManager.ensureReminderScheduledSmart(context, streak)
        }
    }
    private fun buildBillReminder(
        context: Context,
        dueBills: List<BillEntity>,
        todayMidnight: Long,
        tomorrowMidnight: Long
    ): BillReminder? {
        if (dueBills.isEmpty()) return null
        val bill = dueBills.first()
        return when {
            bill.dueDate == todayMidnight -> BillReminder(
                message = context.getString(R.string.planner_reminder_bill_due_today, bill.title),
                urgency = PlannerReminderNotificationHelper.Urgency.HIGH,
                overdueDays = 0
            bill.dueDate == tomorrowMidnight -> BillReminder(
                message = context.getString(R.string.planner_reminder_bill_due_tomorrow, bill.title),
                urgency = PlannerReminderNotificationHelper.Urgency.DEFAULT,
            bill.dueDate < todayMidnight -> BillReminder(
                message = context.getString(
                    R.string.planner_reminder_bill_overdue,
                    bill.title,
                    daysBetweenUtc(bill.dueDate, todayMidnight)
                ),
                overdueDays = daysBetweenUtc(bill.dueDate, todayMidnight)
            else -> null
    private fun buildGoalReminder(
        goals: List<GoalEntity>,
        savedByGoalId: Map<Long, Double>
    ): GoalReminder? {
        if (goals.isEmpty()) return null
        val candidate = goals
            .mapNotNull { goal ->
                val targetDate = goal.targetDate ?: return@mapNotNull null
                val daysLeft = daysBetweenUtc(todayMidnight, targetDate.toUtcMidnight())
                if (daysLeft < 0) return@mapNotNull null
                val saved = savedByGoalId[goal.id] ?: 0.0
                val remaining = (goal.targetAmount - saved).coerceAtLeast(0.0)
                if (remaining <= 0.0) return@mapNotNull null
                GoalReminder(goal.title, remaining, daysLeft)
            .minWithOrNull(compareBy<GoalReminder> { it.daysLeft }.thenByDescending { it.remaining })
            ?: return null
        val message = context.getString(
            R.string.planner_reminder_goal_context,
            candidate.goalTitle,
            CurrencyManager.format(context, candidate.remaining),
            candidate.daysLeft
        )
        return candidate.copy(message = message, suggestedAmount = candidate.remaining.coerceAtMost(500.0))
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
    companion object {
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    private data class GoalReminder(
        val goalTitle: String,
        val remaining: Double,
        val daysLeft: Int,
        val message: String = "",
        val suggestedAmount: Double = 500.0
    )
    private data class BillReminder(
        val message: String,
        val urgency: PlannerReminderNotificationHelper.Urgency,
        val overdueDays: Int
}
