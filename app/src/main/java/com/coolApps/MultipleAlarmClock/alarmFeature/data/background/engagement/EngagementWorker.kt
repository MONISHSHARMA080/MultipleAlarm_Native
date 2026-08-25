package com.coolApps.MultipleAlarmClock.alarmFeature.data.background.engagement


import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.offline.OfflineNotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


@HiltWorker
class EngagementWorker @AssistedInject constructor(
		@Assisted appContext: Context,
		@Assisted workerParams: WorkerParameters,
		private val alarmDao: AlarmDao,
		private val analytics: Analytics,
) : CoroutineWorker(appContext, workerParams) {

	override suspend fun doWork(): Result {

		val now = System.currentTimeMillis()
		val config = analytics.getEngagementConfig()

		if (!config.enabled) {
//			scheduleNextCheck(now + config.checkIntervalHours.hoursToMillis)
			return Result.success()
		}

		// we want to schedule the alarm if
		// a) not alarm in future
		// b) the last alarm in past is some time back

		val nextAlarm = alarmDao.getNextUpcomingAlarm(now)
		if (nextAlarm != null) {
			return Result.success()
		}

		val lastAlarmEnd = alarmDao.getLastCompletedAlarmTime(now) ?: run {
			return Result.success()
		}

		val eligibleAt = lastAlarmEnd + config.inactiveDays.daysToMillis

		if (now < eligibleAt) {
			return Result.success()
		}

		// send notification
		OfflineNotificationScheduler.scheduleNotification(
			context = applicationContext, slot = config.notificationTimeSlot
		)

		analytics.captureEvent(
			"engagement_notification_sent",
			mapOf(
				"inactive_days" to config.inactiveDays,
				"cooldown_days" to config.cooldownDays,
				"notificaton_time_slot" to config.notificationTimeSlot
			)
		)

		return Result.success()
	}

//	private suspend fun scheduleNextCheck(nextRunAt: Long) {
//
//		val workManager = WorkManager.getInstance(applicationContext)
//
//		val workInfo = withContext(Dispatchers.IO) {
//				workManager.getWorkInfosForUniqueWork("engagement-worker").get()
//		}.firstOrNull() ?: return
//
//		val updatedRequest = PeriodicWorkRequestBuilder<EngagementWorker>(24, TimeUnit.HOURS)
//			.setId(workInfo.id)
//			.setNextScheduleTimeOverride(nextRunAt)
//			.build()
//
//		withContext(Dispatchers.IO) {
//			workManager.updateWork(updatedRequest).get()
//		}
//	}

}

private val Long.hoursToMillis: Long
	get() = this.hours.inWholeMilliseconds

private val Long.daysToMillis: Long
	get() = this.days.inWholeMilliseconds
