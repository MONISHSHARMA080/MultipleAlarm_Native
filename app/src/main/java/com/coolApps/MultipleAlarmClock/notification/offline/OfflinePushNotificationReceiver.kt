package com.coolApps.MultipleAlarmClock.notification.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

/**
 * BroadcastReceiver responsible for receiving offline push notification alarms and
 * displaying the localized offline engagement notifications using NotificationHandler.
 */
@AndroidEntryPoint
class OfflinePushNotificationReceiver : BroadcastReceiver() {

	@Inject
	lateinit var notificationHandler: NotificationHandler

	@Inject
	lateinit var analytics: Analytics

	override fun onReceive(context: Context, intent: Intent) {
		val slotName = intent.getStringExtra(EXTRA_SLOT_NAME)
		val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

		val slot = OfflineNotificationTimeSlot.fromSlotName(slotName)
		val content = if (notificationId in 1..slot.notifications.size) {
			slot.getNotificationById(notificationId)
		} else {
			slot.getRandomNotification()
		}

		val title = context.getString(content.titleRes)
		val message = context.getString(content.messageRes)

		val notification = notificationHandler.build(
			notificationChannel = NotificationChannelType.PushNotification,
			notificationTitle = title,
			notificationText = message,
		)

		notificationHandler.show(notification)

		analytics.captureEvent(
			"push_notification_displayed",
			mapOf(
				"slot" to slot.slotName,
				"title" to title,
				"source" to "OfflinePushNotificationReceiver"
			)
		)
	}

	companion object {
		const val ACTION_SHOW_OFFLINE_NOTIFICATION = "com.coolApps.MultipleAlarmClock.ACTION_SHOW_OFFLINE_NOTIFICATION"
		const val EXTRA_SLOT_NAME = "extra_slot_name"
		const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

		fun createIntent(
			context: Context,
			slot: OfflineNotificationTimeSlot? = null,
			notificationId: Int? = null,
		): Intent {
			return Intent(context, OfflinePushNotificationReceiver::class.java).apply {
				action = ACTION_SHOW_OFFLINE_NOTIFICATION
				if (slot != null) {
					putExtra(EXTRA_SLOT_NAME, slot.slotName)
				}
				if (notificationId != null) {
					putExtra(EXTRA_NOTIFICATION_ID, notificationId)
				}
			}
		}
	}
}
