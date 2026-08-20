package com.coolApps.MultipleAlarmClock.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

	override fun onRegistered(installationId: String) {
		super.onRegistered(installationId)
		// send this to server
		logD("got new token $installationId")
		sendRegistrationToServer(installationId)
	}

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
//            ?: getString(R.string.app_name)
			?: return

		val message = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: return

		logD("title:$title, message: $message")
        showNotification(title, message, remoteMessage.data)
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
		// use notification handler to handle it later
    }


    /**
     * Send the Firebase Installation ID (FID) to your application server.
     * Left blank for future implementation.
     */
    private fun sendRegistrationToServer(fid: String) {
//         TODO: Implement sending FID to server in future prompt
			logD("in sendRegistrationToServer")
//		   TODO()
    }

	fun logD(message: String){
		Log.d("AAAAAA", "[FirebaseMessagingService] $message")
	}
}