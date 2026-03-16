package com.coolApps.MultipleAlarmClock.workManager

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDatabase
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.jvm.java
import com.coolApps.MultipleAlarmClock.utils.Result.Result as ResultsCustom

class ResetAlarmAfterBoot(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {
	val alarmsController = AlarmsController()
	val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
	val analytics = Analytics(appContext)

	override suspend fun doWork(): Result {
		// Do the work here--in this case, upload the images.
		val alarmDao = Room.databaseBuilder(applicationContext, AlarmDatabase::class.java, "alarm-database").build().alarmDao()
		val allAlarmsInDb =getAllAlarms(alarmDao)
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
					val result =alarmsController.resetAlarms(alarmData = alarmData, alarmManager = alarmManager, activityContext = applicationContext, alarmDao = alarmDao)
					if (result.isErr()){
						alarmsController.updateAlarmStateInDb(alarmData.copy(isReadyToUse = false), alarmDao)
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
	private suspend fun getAllAlarms(alarmDao: AlarmDao): List<AlarmData> {
		return alarmDao.getAllAlarms()
	}
}
