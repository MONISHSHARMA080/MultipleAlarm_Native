package com.coolApps.MultipleAlarmClock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.navigation.Screen
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.util.NotificationChannelType
import com.coolApps.MultipleAlarmClock.util.NotificationHandler
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

/**
 * BroadcastReceiver that fires 1 day before the user's free trial expires.
 * Displays a notification linking to the paywall so the user can subscribe.
 */
@AndroidEntryPoint
class TrialReminderReceiver : BroadcastReceiver() {

	@Inject
	lateinit var notificationHandler: NotificationHandler

	@Inject
	lateinit var analytics: Analytics

	override fun onReceive(context: Context, intent: Intent) {
		val title = context.getString(R.string.trial_reminder_title)
		val message = context.getString(R.string.trial_reminder_message)

		val notification = notificationHandler.build(
			notificationChannel = NotificationChannelType.Engagement,
			notificationTitle = title,
			notificationText = message,
			targetScreen = Screen.AlarmContainer,
		)
		notificationHandler.show(notification)
		logD("in TrialReminderReceiver ")

		analytics.captureEvent(
			"trial_reminder_notification_shown",
			mapOf("source" to "TrialReminderReceiver")
		)
	}

	companion object {
		const val ACTION_TRIAL_REMINDER =
			"com.coolApps.MultipleAlarmClock.ACTION_TRIAL_REMINDER"
	}
}
