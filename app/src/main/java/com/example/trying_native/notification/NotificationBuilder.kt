package com.example.trying_native.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.trying_native.R
import com.example.trying_native.logD
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class NotificationBuilder(context:Context, notificationText:String, title:String, val NOTIFICATION_ID: Int = Random.nextInt(0, 1000)) {
     companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Channel"
        private const val CHANNEL_DESCRIPTION = "Default notification channel"

    }
    private  val logFile = File(context.getExternalFilesDir(null), "Notification_logs.txt")
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val notificationTitle = title
    val notificationBody = notificationText
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
        writeLogToFile(title = notificationTitle, message = notificationBody)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    private fun writeLogToFile(title: String, message: String) {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "NotificationClass ($now) : --title:$title --\n\n --message:$message--- \n"
            // Append the log entry to the file
            FileWriter(logFile, true).use { writer -> writer.append(logEntry) }
            logD("Logged from Notification Builder: $logEntry")
        } catch (e: Exception) {
            logD("Failed to log from AlarmReceiver: ${e.message}")
        }
    }

}
