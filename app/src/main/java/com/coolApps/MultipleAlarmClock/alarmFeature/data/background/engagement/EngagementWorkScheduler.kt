package com.coolApps.MultipleAlarmClock.alarmFeature.data.background.engagement

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


object EngagementScheduler {

	private const val WORK_NAME = "engagement-worker"

	fun ensureScheduled(context: Context) {

		val request = PeriodicWorkRequestBuilder<EngagementWorker>(24, TimeUnit.HOURS).build()

		WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(
				WORK_NAME,
				ExistingPeriodicWorkPolicy.KEEP,
				request
			)
	}
}


