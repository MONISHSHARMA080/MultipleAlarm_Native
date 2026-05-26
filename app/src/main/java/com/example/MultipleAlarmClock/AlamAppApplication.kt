package com.example.MultipleAlarmClock

import android.app.Application
import android.content.res.Configuration
import androidx.hilt.work.HiltWorkerFactory
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
