package com.i2medier.financialpro.activity

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.reminder.PlannerReminderConstants
import com.i2medier.financialpro.planner.reminder.PlannerReminderManager
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

class ReminderCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_center)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val prefs = getSharedPreferences(PlannerReminderConstants.PREFS_NAME, MODE_PRIVATE)
        val savingToggle = findViewById<SwitchCompat>(R.id.switchSavingReminder)
        val goalToggle = findViewById<SwitchCompat>(R.id.switchGoalReminder)
        val billsToggle = findViewById<SwitchCompat>(R.id.switchBillsReminder)
        val weekdayTimeValue = findViewById<TextView>(R.id.tvWeekdayTimeValue)
        val weekendTimeValue = findViewById<TextView>(R.id.tvWeekendTimeValue)

        var savingEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
        var goalEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_GOAL_REMINDER_ENABLED, true)
        var billsEnabled = prefs.getBoolean(PlannerReminderConstants.KEY_BILL_REMINDER_ENABLED, true)
        var weekdayHour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKDAY_HOUR
        )
        var weekdayMinute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKDAY_MINUTE
        )
        var weekendHour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKEND_HOUR
        )
        var weekendMinute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKEND_MINUTE
        )

        fun saveState() {
            prefs.edit()
                .putBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, savingEnabled)
                .putBoolean(PlannerReminderConstants.KEY_GOAL_REMINDER_ENABLED, goalEnabled)
                .putBoolean(PlannerReminderConstants.KEY_BILL_REMINDER_ENABLED, billsEnabled)
                .apply()

            val anyEnabled = savingEnabled || goalEnabled || billsEnabled
            if (anyEnabled) {
                PlannerReminderManager.updateReminderSchedule(
                    context = this,
                    weekdayHour = weekdayHour,
                    weekdayMinute = weekdayMinute,
                    weekendHour = weekendHour,
                    weekendMinute = weekendMinute
                )
            } else {
                PlannerReminderManager.disableReminder(this)
            }
        }

        fun formatTime(hour: Int, minute: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(cal.time)
        }

        fun updateTimeLabels() {
            weekdayTimeValue.text = formatTime(weekdayHour, weekdayMinute)
            weekendTimeValue.text = formatTime(weekendHour, weekendMinute)
        }

        savingToggle.isChecked = savingEnabled
        goalToggle.isChecked = goalEnabled
        billsToggle.isChecked = billsEnabled
        updateTimeLabels()

        savingToggle.setOnCheckedChangeListener { _, isChecked ->
            savingEnabled = isChecked
            saveState()
        }
        goalToggle.setOnCheckedChangeListener { _, isChecked ->
            goalEnabled = isChecked
            saveState()
        }
        billsToggle.setOnCheckedChangeListener { _, isChecked ->
            billsEnabled = isChecked
            saveState()
        }

        findViewById<android.view.View>(R.id.cardWeekdayTime).setOnClickListener {
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    weekdayHour = selectedHour
                    weekdayMinute = selectedMinute
                    updateTimeLabels()
                    saveState()
                },
                weekdayHour,
                weekdayMinute,
                false
            ).show()
        }
        findViewById<android.view.View>(R.id.cardWeekendTime).setOnClickListener {
            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    weekendHour = selectedHour
                    weekendMinute = selectedMinute
                    updateTimeLabels()
                    saveState()
                },
                weekendHour,
                weekendMinute,
                false
            ).show()
        }
    }
}
