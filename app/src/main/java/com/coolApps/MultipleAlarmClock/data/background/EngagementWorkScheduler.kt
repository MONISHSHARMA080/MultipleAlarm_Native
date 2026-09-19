package com.coolApps.MultipleAlarmClock.data.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.coolApps.MultipleAlarmClock.util.EngagementConfig
import java.util.concurrent.TimeUnit


object EngagementScheduler {

	private const val WORK_NAME = "engagement-worker"

	fun ensureScheduled(context: Context, config: EngagementConfig) {

		val request = PeriodicWorkRequestBuilder<EngagementWorker>(config.checkIntervalHours, TimeUnit.HOURS).build()

		WorkManager.getInstance(context)
			.enqueueUniquePeriodicWork(
				WORK_NAME,
				ExistingPeriodicWorkPolicy.KEEP,
				request
			)
	}
}


