package com.coolApps.MultipleAlarmClock.workManager

import android.app.AlarmManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.trial.TrialReminderScheduler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.coolApps.MultipleAlarmClock.utils.Result.Result as ResultsCustom

@HiltWorker
class ResetAlarmAfterBoot @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted workerParams: WorkerParameters,
	private val analytics: Analytics,           // injected
	private val alarmsController: AlarmsController ,// injected
	private val alarmRepository: AlarmRepository,
	private val errorHandler: ErrorHandler
) : CoroutineWorker(appContext, workerParams) {

	val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

	override suspend fun doWork(): Result {
		// Re-schedule trial reminder if user is on an active trial
		rescheduleTrialReminderIfNeeded()

		// Do the work here--in this case, upload the images.
		val allAlarmsInDb =getAllAlarms(alarmRepository)
		val enabledAlarms: List<AlarmData> = allAlarmsInDb.filter { it.isReadyToUse }

		analytics.captureEvent("resetting Alarm after boot or app update", mapOf(
			"enabledAlarms" to enabledAlarms.toString(),
			"allAlarmsInDb" to allAlarmsInDb.toString(),
			"isPackageReplaced" to inputData.getBoolean("isPackageReplaced", false),
			"isBootCompleted" to inputData.getBoolean("isBootCompleted", false),
			"class" to "ResetAlarmAfterBoot"
		))
		// Process all alarms and collect results
		val results = coroutineScope {
			enabledAlarms.map { alarmData ->
				async {
					val result =alarmsController.resetAlarmsHandler(alarmData = alarmData, alarmManager = alarmManager, activityContext = applicationContext)
					return@async result
				}
			}.awaitAll()
		}
		val hasError = results.any { it.isErr() }

		results.forEach { result ->
			result.fold(onSuccess = {}, onError = { error ->
				errorHandler.handleError(ResultsCustom.Failure(error))
			})
		}
		return if (hasError) Result.failure() else Result.success()
	}

	/**
	 * Re-schedules the trial reminder alarm after boot/app update.
	 * Uses RevenueCat's cached CustomerInfo so this works offline.
	 */
	private suspend fun rescheduleTrialReminderIfNeeded() {
		try {
			val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
			TrialReminderScheduler.scheduleIfOnTrial(applicationContext, customerInfo)
		} catch (_: Exception) {
			// Non-critical — if cache is empty and offline, we skip.
			// The reminder is a best-effort enhancement.
		}
	}

	private suspend fun getAllAlarms(alarmRepository: AlarmRepository): List<AlarmData> {
		return alarmRepository.getAllAlarms()
	}
}

