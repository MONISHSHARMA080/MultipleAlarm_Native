package com.coolApps.MultipleAlarmClock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

//	override fun onCreate() {
//		super.onCreate()
////		val options = FirebaseApp.getInstance().options
////		logD("Options:$options, ")
////		FirebaseMessaging.getInstance().register().addOnCompleteListener { task ->
////			if (!task.isSuccessful) {
////				logD("Fetching FCM registration failed: ${task.exception?.message}")
////				task.exception?.printStackTrace()
////				return@addOnCompleteListener
////			}
////
////			// Registration successful.
////			// Note: result is Void for register() task.
////			// The actual token (FID) is delivered via FirebaseMessagingService.onRegistered(String)
////			logD("FCM Registration successful")
////		}
//	}
}
