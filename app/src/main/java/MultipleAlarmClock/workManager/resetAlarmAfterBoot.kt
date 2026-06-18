package com.coolApps.MultipleAlarmClock.workManager

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import android.app.AlarmManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
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
	private val alarmRepository: AlarmRepository
) : CoroutineWorker(appContext, workerParams) {

	val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

	override suspend fun doWork(): Result {
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
					val result =alarmsController.resetAlarms(alarmData = alarmData, alarmManager = alarmManager, activityContext = applicationContext)
					if (result.isErr()){
						alarmsController.updateAlarmStateInDb(alarmData.copy(isReadyToUse = false))
					}
					return@async result
				}
			}.awaitAll()
		}
		val hasError = results.any { it.isErr() }

		results.forEach { result ->
			result.fold(onSuccess = {}, onError = {errorToDisplayUser, exception ->
				ErrorHandler(NotificationHandler(applicationContext), Analytics(applicationContext)).handleError(ResultsCustom.Failure(errorToDisplayUser, exception), "error in resetting the alarm, after boot or app update ")
			})
		}
		return if (hasError) Result.failure() else Result.success()
	}
	private suspend fun getAllAlarms(alarmRepository: AlarmRepository): List<AlarmData> {
		return alarmRepository.getAllAlarms()
	}
}
