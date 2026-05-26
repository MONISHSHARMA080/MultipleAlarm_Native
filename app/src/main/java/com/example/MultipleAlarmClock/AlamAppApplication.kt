package com.example.MultipleAlarmClock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject


@HiltAndroidApp
class AlarmApp : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory  // ← inject Hilt's factory

	override fun onCreate() {
		super.onCreate()
	}

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)        // ← needed for Hilt workers
			.build()
}
