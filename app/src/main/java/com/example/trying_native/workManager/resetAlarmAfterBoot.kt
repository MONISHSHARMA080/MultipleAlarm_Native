package com.example.trying_native.workManager

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.AlarmLogic.ResetAlarmError
import com.example.trying_native.ErrorHandling.ErrorHandler
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.notification.NotificationHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.filter
import  com.example.trying_native.utils.Result.Result as ResultsCustom

class ResetAlarmAfterBoot(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {
	val alarmsController = AlarmsController()
	val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

	override suspend fun doWork(): Result {
		// Do the work here--in this case, upload the images.
		val alarmDao = Room.databaseBuilder(applicationContext, AlarmDatabase::class.java, "alarm-database").build().alarmDao()
		val enabledAlarms: List<AlarmData> = getAllAlarms(alarmDao).filter { it.isReadyToUse }

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
				ErrorHandler(NotificationHandler(applicationContext)).handleError(ResultsCustom.Failure(errorToDisplayUser, exception), "error in resetting the alarm, after boot or app update ")
			})
		}
		return if (hasError) Result.failure() else Result.success()
	}
	private suspend fun getAllAlarms(alarmDao: AlarmDao): List<AlarmData> {
		return alarmDao.getAllAlarms()
	}
//	private suspend fun resetAlarm(alarm: AlarmData, alarmDao: AlarmDao): ResultObj<Unit> {
//		return runCatching {
//			 alarmsController.resetAlarms(alarmData = alarm, alarmManager = alarmManager, activityContext = applicationContext, alarmDao = alarmDao).fold(onSuccess = {}, onError = {errorToDisplayUser, exception ->
//				 logD("there is an exception in resetting the alarm after boot and  it is $exception -- and the message for the user is $errorToDisplayUser")
//
//			 })
//		}
//	}
}
