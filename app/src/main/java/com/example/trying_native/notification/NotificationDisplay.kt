package com.example.trying_native.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.trying_native.R
import androidx.core.app.NotificationCompat
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
	companion object {
		val values:List<NotificationChannelType> = listOf(AlarmNotification, ErrorChannel, GeneralNotification)
	}
}

class NotificationHandler(val context: Context) {

	 private var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	fun build(
		notificationChannel: NotificationChannelType, notificationTitle: String, notificationText: String
	): Notification {
		return NotificationCompat.Builder(context, notificationChannel.channelId )
			.setSmallIcon(R.mipmap.ic_launcher_app_icon_name)
			.setContentTitle(notificationTitle)
			.setContentText(notificationText)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.build()
	}

	fun createNotificationChannel(notificationChannelDetail: NotificationChannelType){
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