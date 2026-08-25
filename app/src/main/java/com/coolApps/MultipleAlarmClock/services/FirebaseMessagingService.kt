package com.coolApps.MultipleAlarmClock.services

import android.annotation.SuppressLint
import android.util.Log
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {
	@Inject
	lateinit var analytics: Analytics

	@Inject
	lateinit var notificationHandler: NotificationHandler

	override fun onRegistered(firebaseID: String) {
		super.onRegistered(firebaseID)
		// send this to server
		logD("got new token $firebaseID")
		sendRegistrationToPostHog(firebaseID)
	}

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: return

        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: return

        logD("title:$title, message: $message and  remoteMessage:  $remoteMessage ")

        showNotification(title, message, remoteMessage.data)
        analytics.captureEvent("fcm_message_received",
			mapOf(
				"title" to title,
				"message" to message,
				"remoteMessage" to remoteMessage.toString()
			)
		)

    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val notification = notificationHandler.build(
            notificationChannel = NotificationChannelType.PushNotification,
            notificationTitle = title,
            notificationText = message
        )
        notificationHandler.show(notification)
    }


    /**
     * Send the Firebase Installation ID (FID) to your application server.
     * Left blank for future implementation.
     */
    private fun sendRegistrationToPostHog(fid: String) {
		logD("in sendRegistrationToServer and fid: $fid")
		analytics.setFcmToken(fid)
    }

	fun logD(message: String){
		Log.d("AAAAAA", "[FirebaseMessagingService] $message")
	}
}