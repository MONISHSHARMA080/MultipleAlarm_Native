package com.coolApps.MultipleAlarmClock.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.coolApps.MultipleAlarmClock.MainActivity
import com.coolApps.MultipleAlarmClock.R
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlin.random.Random

sealed class NotificationChannelType(val channelId: String, val channelName: String, val importance: Int, val description: String ) {
	data object AlarmNotification : NotificationChannelType(
		channelId = "alarm_channel",
		channelName = "Alarms",
		importance = NotificationManager.IMPORTANCE_HIGH,
		description = "Alarm notification channel"
	)

	data object ErrorChannel : NotificationChannelType(
		channelId = "reminder_channel",
		channelName = "Reminders",
		importance = NotificationManager.IMPORTANCE_HIGH,
		description = "Alarm error channel"
	)

	data object GeneralNotification : NotificationChannelType(
		channelId = "general_channel",
		channelName = "General",
		importance = NotificationManager.IMPORTANCE_DEFAULT ,
		description = "Alarm notification channel"
	)
	data object PushNotification : NotificationChannelType(
		channelId = "push_notification",
		channelName = "General",
		importance = NotificationManager.IMPORTANCE_DEFAULT ,
		description = "Updates related to the app"
	)

	data object Engagement : NotificationChannelType(
		channelId = "engagement_channel",
		channelName = "Reminders",
		importance = NotificationManager.IMPORTANCE_DEFAULT,
		description = "Re-engagement reminders when you haven't used alarms recently"
	)

	companion object {
		val values:List<NotificationChannelType> = listOf(AlarmNotification, ErrorChannel, GeneralNotification, PushNotification, Engagement)
	}
}

class NotificationHandler @Inject constructor(@ApplicationContext val context: Context) {

	 private var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	fun build(
			contentIntent: PendingIntent? = createDefaultPendingIntent(),
			notificationChannel: NotificationChannelType, notificationTitle: String, notificationText: String,
	): Notification {

		return NotificationCompat.Builder(context, notificationChannel.channelId)
			.setSmallIcon(R.mipmap.app_icon)
			.setContentTitle(notificationTitle)
			.setContentText(notificationText)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setAutoCancel(true) // Automatically dismisses notification when tapped
			.apply {
				if (contentIntent != null) {
					setContentIntent(contentIntent) // Launches app when tapped
				}
			}
			.build()
	}

	private fun createDefaultPendingIntent(): PendingIntent? {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
		}
		return PendingIntent.getActivity(
			context,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

	}

	private fun createNotificationChannel(notificationChannelDetail: NotificationChannelType){
		val channel = NotificationChannel(
			 notificationChannelDetail.channelId, notificationChannelDetail.channelName,  notificationChannelDetail.importance
		).apply {
			description = notificationChannelDetail.description
		}
		notificationManager.createNotificationChannel(channel)
	}
	/** call this function when you start your app on the coroutine to create all the notification channel*/
	 fun createNotificationChannels(){
		NotificationChannelType.values.forEach {
			createNotificationChannel(it)
		}
	}

	fun show(notification: Notification){
		notificationManager.notify(Random.nextInt(1, 100_000_000), notification)
	}


}