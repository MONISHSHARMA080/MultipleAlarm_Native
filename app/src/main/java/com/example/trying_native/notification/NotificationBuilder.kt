package com.example.trying_native.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.trying_native.R
import kotlin.random.Random

class NotificationBuilder(context:Context, notificationText:String, title:String, val NOTIFICATION_ID: Int = Random.nextInt(0, 1000)) {
     companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Channel"
        private const val CHANNEL_DESCRIPTION = "Default notification channel"
    }
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    var builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_app_icon_name)
        .setContentTitle(title)
        .setContentText(notificationText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText)   )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    init {
        createNotificationChannel()
    }
    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            importance
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification() {
        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
