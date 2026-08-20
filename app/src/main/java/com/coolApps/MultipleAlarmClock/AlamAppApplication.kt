package com.coolApps.MultipleAlarmClock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class AlarmApp : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		val options = FirebaseApp.getInstance().options
		logD("Options:$options, ")
//		FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//			if (!task.isSuccessful) {
//				logD( "Fetching FCM registration token failed ${task.exception?.message}\n result:${task.result}, stackTrace:${task.exception?.stackTrace},  " )
//				return@addOnCompleteListener
//			}
//
//			// Get new FCM registration token
//			val token = task.result
//			logD( "FCM Token: $token")
//			// Send this token to your server if required
//		}
	}
}
