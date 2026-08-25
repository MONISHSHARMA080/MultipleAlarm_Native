package com.coolApps.MultipleAlarmClock.notification.offline

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

/**
 * Utility to schedule offline push notifications using AlarmManager.
 */
object OfflineNotificationScheduler {

	private const val OFFLINE_NOTIFICATION_REQUEST_CODE = 99881

	/**
	 * Schedules an offline notification alarm for a specific slot or the next upcoming slot.
	 */
	fun scheduleNotification(
			context: Context,
			slot: OfflineNotificationTimeSlot = OfflineNotificationTimeSlot.Night,
			triggerAtMillis: Long = slot.calculateNextTriggerTime(),
	) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		val intent = OfflinePushNotificationReceiver.createIntent(context, slot)
		val pendingIntent = PendingIntent.getBroadcast(
			context,
			OFFLINE_NOTIFICATION_REQUEST_CODE,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		alarmManager.setAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			triggerAtMillis,
			pendingIntent
		)
	}

	/**
	 * Cancels any scheduled offline push notification.
	 */
	fun cancelScheduledNotification(context: Context) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
		val intent = OfflinePushNotificationReceiver.createIntent(context)
		val pendingIntent = PendingIntent.getBroadcast(
			context,
			OFFLINE_NOTIFICATION_REQUEST_CODE,
			intent,
			PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
		)
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent)
			pendingIntent.cancel()
		}
	}
}
