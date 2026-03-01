package com.example.trying_native.workManager

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.logD
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.filter
import  kotlin.Result as ResultObj

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
					resetAlarm(alarmData, alarmDao)
				}
			}.awaitAll()
		}

		// Check if any failed
		val hasError = results.any { it.isFailure }

		results.forEach { result ->
			result.onFailure { exception ->
				logD("Failed to reset alarm: ${exception.message}")
			}
		}

		return if (hasError) Result.failure() else Result.success()

//		enabledAlarms.forEach { alarmData ->
//			coroutineScope { launch {
//					resetAlarm(alarmData, alarmDao, appContext).fold(onSuccess = {}, onFailure = {exception ->
//						logD("there is an exception in resetting the alarm after boot and  it is ${exception.message} --")
//						doWeHaveError = true
//					})
//				} }
//		}
	}
	private suspend fun getAllAlarms(alarmDao: AlarmDao): List<AlarmData> {
		return alarmDao.getAllAlarms()
	}
	private suspend fun resetAlarm(alarm: AlarmData, alarmDao: AlarmDao): ResultObj<Unit> {
		return runCatching {
			val a = alarmsController.resetAlarms(alarmData = alarm, alarmManager = alarmManager, activityContext = applicationContext, alarmDao = alarmDao)
			if (a.isFailure) {
				logD("there is an exception in resetting the alarm after boot and  it is ${a.exceptionOrNull()} --")
				throw Exception(a.exceptionOrNull()?.message ?:"there is an exception while setting the alarm after boot")
			} else return@runCatching
		}
	}
}
