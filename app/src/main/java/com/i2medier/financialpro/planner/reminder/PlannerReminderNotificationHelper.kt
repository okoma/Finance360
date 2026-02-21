package com.i2medier.financialpro.planner.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerAddRequest
import com.i2medier.financialpro.planner.integration.PlannerIntegrationContract
import com.i2medier.financialpro.ui.CalculatorRegistry

class PlannerReminderNotificationHelper(private val context: Context) {
    enum class Urgency {
        DEFAULT,
        HIGH
    }

    enum class SecondaryAction {
        VIEW_PLANNER,
        REMIND_LATER,
        NONE
    }

    init {
        createChannelIfNeeded()
    }

    fun showReminder(
        message: String,
        urgency: Urgency = Urgency.DEFAULT,
        notificationId: Int = PlannerReminderConstants.NOTIFICATION_ID,
        primaryLabel: String = context.getString(R.string.planner_action_add_saving),
        primaryRequest: PlannerAddRequest = defaultSavingRequest(),
        secondaryAction: SecondaryAction = SecondaryAction.VIEW_PLANNER,
        secondaryLabel: String? = null
    ) {
        if (!hasNotificationPermission()) return

        val intent = PlannerIntegrationContract.createOpenPlannerDeepLinkIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (urgency == Urgency.HIGH) {
            PlannerReminderConstants.CHANNEL_ID_HIGH
        } else {
            PlannerReminderConstants.CHANNEL_ID_DEFAULT
        }

        val priority = if (urgency == Urgency.HIGH) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_money)
            .setContentTitle(context.getString(R.string.planner_notification_title))
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, primaryLabel, createAddSavingPendingIntent(primaryRequest))

        when (secondaryAction) {
            SecondaryAction.VIEW_PLANNER -> builder.addAction(
                0,
                secondaryLabel ?: context.getString(R.string.planner_action_view_planner),
                createViewPlannerPendingIntent()
            )
            SecondaryAction.REMIND_LATER -> builder.addAction(
                0,
                secondaryLabel ?: context.getString(R.string.planner_action_remind_later),
                createRemindLaterPendingIntent()
            )
            SecondaryAction.NONE -> Unit
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val defaultChannel = NotificationChannel(
            PlannerReminderConstants.CHANNEL_ID_DEFAULT,
            context.getString(R.string.planner_notification_channel_name_default),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.planner_notification_channel_description)
            enableVibration(true)
        }

        val highChannel = NotificationChannel(
            PlannerReminderConstants.CHANNEL_ID_HIGH,
            context.getString(R.string.planner_notification_channel_name_high),
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(defaultChannel)
        manager.createNotificationChannel(highChannel)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun defaultSavingRequest(): PlannerAddRequest {
        return PlannerAddRequest(
            amount = 500.0,
            suggestedType = TransactionType.SAVING,
            note = context.getString(R.string.planner_notification_default_saving_note),
            title = context.getString(R.string.planner_notification_title),
            calculatorCategory = CalculatorRegistry.CATEGORY_SAVINGS
        )
    }

    private fun createAddSavingPendingIntent(request: PlannerAddRequest): PendingIntent {
        val addIntent = PlannerIntegrationContract.createOpenPlannerIntent(context, request).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            101,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createViewPlannerPendingIntent(): PendingIntent {
        val plannerIntent = PlannerIntegrationContract.createOpenPlannerDeepLinkIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            102,
            plannerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createRemindLaterPendingIntent(): PendingIntent {
        val intent = Intent(context, PlannerReminderActionReceiver::class.java).apply {
            action = PlannerReminderActionReceiver.ACTION_REMIND_LATER
        }
        return PendingIntent.getBroadcast(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
