package com.coolApps.MultipleAlarmClock.AlarmLogic

	import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.toDomain
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.alarmFeature.receiver.AlarmInfoNotification
import com.coolApps.MultipleAlarmClock.alarmFeature.receiver.AlarmReceiver
import com.coolApps.MultipleAlarmClock.alarmFeature.receiver.LastAlarmUpdateDBReceiver
	import com.coolApps.MultipleAlarmClock.analytics.Analytics
	import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.coolApps.MultipleAlarmClock.utils.Result.Result as ResultCustom

const val ALARM_ACTION = "com.coolApps.trying_native.ALARM_TRIGGERED"

interface  TimeProvider{
	fun getCurrentTime(): Long
}
class TimeProviderImpl : TimeProvider {
	override fun getCurrentTime() = System.currentTimeMillis()
}

class AlarmsController @Inject constructor(
		private val alarmRepository: AlarmRepository,
		private val timeProvider: TimeProvider,
		private val alarmManager: AlarmManager,
		val analytics: Analytics,
		private val errorHandler: ErrorHandler,
		@ApplicationContext  val context: Context
) {
	var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val alarmInfoNotificationClass:Class<out BroadcastReceiver> = AlarmInfoNotification::class.java
	private val alarmReceiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java
	data class NextAlarmInfo(
			val nextAlarmTriggerTime: Long,
			val newSeriesStartTime: Long,
			val newSeriesEndTime: Long,
			val alarmsController: AlarmsController
	){
		override fun toString(): String {
			return "nextAlarmTriggerTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTriggerTime)}, newSeriesStartTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(newSeriesStartTime)}, newSeriesEndTime: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(newSeriesEndTime)}"
		}
	}
	data class PendingIntentCreated(
			/** [pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked] is for what happens when the user clicks on the upcoming alarm notification in the notification shade, we have to give info about alarm */
			val pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked: PendingIntent?,
			val pendingIntentForAlarm: PendingIntent
	){
		override fun toString(): String {
			return "pendingIntentForAlarmNotificationInfo--$pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked --  pendingIntentForAlarm--$pendingIntentForAlarm"
		}
	}

	suspend fun scheduleAlarm(
			alarmManager:AlarmManager, componentActivity: Context, receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, alarmData: AlarmData, alarmTriggerTime:Long
	): ResultCustom<Unit, ScheduleAlarmError>{
		return ResultCustom.runCatching({ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }){
			val res =alarmData.validate()
			if (res != AlarmDataValidationResult.Success) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed (internalErrorMessage ="AlarmData validation failed, and res:$res" ))
			val resultForAlarmOpr = getPendingIntentForAlarm(receiverClass = receiverClass, context = componentActivity, alarmTriggerTime, alarmData = alarmData)
			val PIForAlarm = resultForAlarmOpr.fold(
				onSuccess = {PI -> PI},
				onError = {failureRes->
					return when(failureRes){
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes )
						is AlarmControllerErrorSet.PendingIntentAlreadyExist -> ResultCustom.Failure(errorClass = failureRes )
					}
				}
			)
			val alarmClockInfoObject = AlarmManager.AlarmClockInfo(alarmTriggerTime, PIForAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked)
			alarmManager.setAlarmClock(alarmClockInfoObject, PIForAlarm.pendingIntentForAlarm)
			logD("Alarm successfully scheduled.")
		}
	}

	fun getPendingIntentForAlarm(
			receiverClass:Class<out BroadcastReceiver>, context: Context, alarmTriggerTime:Long, alarmData: AlarmData,
	): ResultCustom<PendingIntentCreated, GetPendingIntentForAlarmError> {
		return ResultCustom.runCatching(
		{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val alarmId = alarmData.id
			val intent = Intent(ALARM_ACTION)
			val intentData = AlarmActivityIntentData(
				startTimeForDb = alarmData.startTime, alarmTriggerTime = alarmTriggerTime, endTime = alarmData.endTime,
				message = alarmData.message, alarmIdInDb = alarmData.id
			)
			intent.setClass(context, receiverClass)
			intent.putExtra("intentData", intentData)
			// this is for the alarm receiver
			var pendingIntentForAlarm = PendingIntent.getBroadcast(context, alarmTriggerTime.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
			if(pendingIntentForAlarm != null){
				return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.PendingIntentAlreadyExist(internalErrorMessage = "Alarm on (${getTimeInHumanReadableFormat(alarmData.startTime)}) already exists" ))
			}
			pendingIntentForAlarm = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
			// meaning that the pending intent does not exist, and it is safe to create one
			intent.putExtra("alarmIdInDb", alarmId)
			var pendingIntentForAlarmInfo: PendingIntent
			val intentForAlarmMetaData:Intent = intent.clone() as Intent
			intentForAlarmMetaData.setClass(context, alarmInfoNotificationClass)
			pendingIntentForAlarmInfo = PendingIntent.getBroadcast(
				context, alarmTriggerTime.toInt(), intentForAlarmMetaData,
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
			)
			return ResultCustom.Success(PendingIntentCreated(pendingIntentForAlarmInfo, pendingIntentForAlarm))
		}
	}

	/**given an alarm Series(or alarm, or startTime->endTime, this function will start it. If the alarm startTime is greater than current startTime then we will iterate over it and set that alarm from that time */
	suspend fun startAlarmSeries(
			alarmData: AlarmData, alarmManager: AlarmManager, activityContext: Context,
			receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java,
	): ResultCustom<Unit, StartAlarmSeriesError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val currentTIme = Calendar.getInstance()
			alarmData.validate().let {
				if (it != AlarmDataValidationResult.Success) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = "AlarmData validation failed, and res:$it"))
			}
			// since I could have startTime < now < endTime
			val timeReturned = alarmData.alarmTimeSequence().firstOrNull{ it > currentTIme.timeInMillis }
				?: return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = "Can't get first alarm to start the series\n alarmData:$alarmData"))
			val scheduleAlarmHandler  =   scope.async {
				scheduleAlarm(
					alarmData = alarmData, alarmManager = alarmManager, alarmTriggerTime = timeReturned, componentActivity = activityContext, receiverClass = receiverClass
				)
			}
			// TODO: last db update is handled in alarmReceiver, alarmReceiver rewrite, it should just receive the intent and schedule next alarm and not make decisions, that code should  be in this class

			scheduleAlarmHandler.await().fold(
				onSuccess = {},
				onError = { failureRes ->
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentNotFound -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentAlreadyExist -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
		}
	}
	/** handle setting the alarms and if fails then cancel it and update the DB state not running else running */
	suspend fun startAlarmSeriesHandler(
			alarm: AlarmData, alarmManager: AlarmManager, activityContext: Context, receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java,
	): ResultCustom<Unit, StartAlarmSeriesHandlerError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		) {
			val upsertResult = alarmRepository.upsertAlarm(alarm.copy(isReadyToUse = true))
			val insertedAlarmData: AlarmData =	if (upsertResult == -1L){
				alarm.copy(isReadyToUse = true) // updated the alarm
			}else {
				alarm.copy(id = upsertResult.toInt(), isReadyToUse = true )
			}
			val alarmResult = this@AlarmsController.startAlarmSeries(
				insertedAlarmData,
				alarmManager,
				activityContext,
				receiverClass,
			)
			alarmResult.fold(
				onSuccess = {},
				onError = { failureRes ->
					// ToDo: fix this error in all alarmController, if the alarmValidation(some cases) doesn't have problem then we don't need to delete it, better for user experience
					alarmRepository.deleteAlarm(insertedAlarmData)
					this@AlarmsController.cancelAlarmHandler(insertedAlarmData, activityContext, alarmManager)
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentNotFound -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentAlreadyExist -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
		}
	}


	/** tries to cancel the alarm and update the Db state, if error  */
	suspend fun cancelAlarmHandler(
			alarmData: AlarmData, context: Context, alarmManager: AlarmManager
	): ResultCustom<Unit, CancelAlarmHandlerError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			this@AlarmsController.updateAlarmStateInDb(alarmData.copy(isReadyToUse = false)).fold(
				onSuccess = {},
				onError = { failureRes ->
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.DatabaseOperationFailed -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
			cancelAlarm(alarmData, context,alarmManager).fold(
				onSuccess = {},
				onError = { failureRes ->
					alarmRepository.upsertAlarm(alarmData.copy(isReadyToUse = false))
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.CancellingAlarmError -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
		}
	}

	/** tries to delete the alarm and update the Db state, if error  */
	suspend fun deleteAlarmHandler(
			alarmData: AlarmData, context: Context, alarmManager: AlarmManager
	): ResultCustom<Unit, DeleteAlarmHandlerError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			// first update the Db as it is more visible to the user, and there is a race condition here as we can't do it in parallel, as are accessing the db in failure in both
			// the alarm was not in the DB so return
			// since the alarm is not in the DB we can't insert it, and also we can't just insert it as it wasn't there and the user did not wanted it
			val rowsAffected = alarmRepository.deleteAlarm(alarmData)
			if (rowsAffected == 0) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.DatabaseOperationFailed(internalErrorMessage = "No such alarm in the Db to delete, alarmData:$alarmData, rowsAffected:$rowsAffected"))
			cancelAlarm(alarmData, context,alarmManager).fold(
				onSuccess = {},
				onError = { failureRes ->
					// since we can't delete it then we should just put it back and tell the user to try to cancel it again
					logD("error when cancelling alarm while deleting alarm is $failureRes")
					if ( failureRes is AlarmControllerErrorSet.Unknown) {
						alarmRepository.upsertAlarm(alarmData.copy(isReadyToUse = false))
					}
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.CancellingAlarmError -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
		}
	}

	/** cancels scheduled alarm*/
	suspend fun cancelAlarm(
			alarmData: AlarmData, context: Context, alarmManager: AlarmManager
	): ResultCustom<Unit, CancelAlarmError > {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val cal = Calendar.getInstance()
			val currentTime = cal.timeInMillis

			val validationResult = alarmData.validate()
			if (validationResult != AlarmDataValidationResult.Success) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = validationResult.errorMessage()))
			val alarmObj = alarmData.toDomain()
			alarmObj.alarmTimeSequence()
				.dropWhile { it > currentTime }
				.take(5)
				.forEach { alarmIterVal ->
					logD("the time value gotten in iterating is ${getTimeInHumanReadableFormatProtectFrom0Included(alarmIterVal)}")

					val intentData = AlarmActivityIntentData(
						startTimeForDb = alarmData.startTime,
						alarmTriggerTime = alarmIterVal,
						endTime = alarmData.endTime,
						message = alarmData.message,
						alarmIdInDb = alarmData.id
					)

					val baseIntent = Intent(ALARM_ACTION)
					cancelPendingIntentReceiver(baseIntent, context, intentData, alarmReceiverClass, alarmManager, alarmData.id)
					cancelPendingIntentReceiver(baseIntent, context, intentData, alarmReceiverClass, alarmManager, alarmData.startTime.toInt())
				}

			// cancel the lastPi that is there to stop the alarm
			val lastAlarmRequestCode = (alarmData.endTime + alarmData.startTime).toInt()
			val lastAlarmIntent = Intent(context, LastAlarmUpdateDBReceiver::class.java)
			val lastAlarmPI = PendingIntent.getBroadcast(
				context, lastAlarmRequestCode, lastAlarmIntent,
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
			)
			lastAlarmPI?.let {
				alarmManager.cancel(it)
				it.cancel()
				logD("Cancelled LastAlarmUpdateDBReceiver")
			}
			// now try to mark it as not ready to use in db and we done if problem then reschedule it and return error
			logD("the alarmData is $alarmData")
		}
	}

	private fun cancelPendingIntentReceiver(
			baseIntent: Intent, context: Context, intentData: AlarmActivityIntentData, alarmReceiverClass:Class<out BroadcastReceiver>, alarmManager: AlarmManager, intentRequestCode: Int
	){
		baseIntent.apply {
			setClass(context, alarmReceiverClass)
			putExtra("intentData", intentData)
		}
		val pendingIntent = PendingIntent.getBroadcast(context, intentRequestCode, baseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
		pendingIntent?.let { alarmManager.cancel(it) }
	}

	suspend fun updateAlarmStateInDb(
			alarmData: AlarmData
	): ResultCustom<Unit, UpdateAlarmInDbError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val rowsAffected = alarmRepository.updateAlarm(alarmData)
			if (rowsAffected == 0) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.DatabaseOperationFailed(internalErrorMessage = "No rows affected for alarmData:$alarmData"))
		}
	}


	suspend fun rescheduleAlarm(
			alarmManager: AlarmManager,
			 activityContext: Context, alarmData:AlarmData,
			receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, nextAlarmInfo: NextAlarmInfo
	): ResultCustom<Unit, RescheduleAlarmError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		) {
			// should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
			// we can't get it form the alarmData as this func is for the reset alarm and that could be only one
			logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
			// these are the new/incremented/updated time
			var alarmDataForDeleting: AlarmData = alarmData
			if (this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime) != this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = "startDate from new startSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime)} and the end date from the new endSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)}"))
			// cause I want to see if there is a error and if there is then I want to react
			try {
				val res = scope.async {
					val updatedAlarm =alarmData.copy(
						isReadyToUse = true,
						startTime = nextAlarmInfo.newSeriesStartTime,
						endTime = nextAlarmInfo.newSeriesEndTime
					)
					alarmRepository.updateAlarm(updatedAlarm)
					return@async updatedAlarm
				}
				val newAlarm = res.await()
				alarmDataForDeleting = newAlarm
				val alarmSchedule = scope.async {
					scheduleAlarm(
						alarmManager = alarmManager, componentActivity = activityContext, alarmData = newAlarm, alarmTriggerTime = nextAlarmInfo.nextAlarmTriggerTime, receiverClass = receiverClass
					)
				}
				alarmSchedule.await().fold(
					onSuccess = {},
					onError = { failureRes ->
						this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(alarmData.startTime, activityContext, alarmManager,"alarm_start_time_to_search_db", "alarm_end_time_to_search_db", alarmData.endTime, LastAlarmUpdateDBReceiver())
						return when (failureRes) {
							is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
							is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
							is AlarmControllerErrorSet.PendingIntentNotFound -> ResultCustom.Failure(errorClass = failureRes)
							is AlarmControllerErrorSet.PendingIntentAlreadyExist -> ResultCustom.Failure(errorClass = failureRes)
						}
					}
				)
			} catch (e: Exception) {
				this.cancelAlarm(alarmDataForDeleting, activityContext, alarmManager,)
				logD("error occurred in the schedule multiple alarms, so we are going to cancel the alarm whole, in scheduleAlarm2-->${e}")
				logD("[UNEXPECTED ERROR] in reschedule alarm failed to account for exception/error (exception here) -> $e")
				return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.Unknown(internalErrorMessage = e.toString()))
			}
		}
	}

	suspend fun resetAlarms(
			alarmData:AlarmData, alarmManager: AlarmManager, activityContext: Context
	): ResultCustom<Unit, ResetAlarmError> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val res = this.calculateNextAlarmInfo(alarmData)
			print("\n\n----- the res from calculating the next alarm info is $res -----\n\n")
			val nextAlarmInfo = res.fold(
				onSuccess = { nextAlarmInfo -> nextAlarmInfo },
				onError = { failureRes ->
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
			val newAlarm = alarmData.copy(startTime = nextAlarmInfo.newSeriesStartTime, endTime = nextAlarmInfo.newSeriesEndTime, isReadyToUse = true,)
			val validationRes = newAlarm.validate()
			if (validationRes != AlarmDataValidationResult.Success){ return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = validationRes.errorMessage())) }

			val updatingAlarmStateJob = this.scope.async {this@AlarmsController.updateAlarmStateInDb(newAlarm)  }
			val scheduleAlarmJob =  scheduleAlarm(
				alarmManager =alarmManager, componentActivity = activityContext, receiverClass = alarmReceiverClass, alarmData = newAlarm, alarmTriggerTime = nextAlarmInfo.nextAlarmTriggerTime
			)
			scheduleAlarmJob.fold(
				onSuccess = {},
				onError = { failureRes ->
					logD("the scheduleAlarm() failed with error $failureRes here cancelling and returning")
					updatingAlarmStateJob.await()// don't care about the error
					this.updateAlarmStateInDb(newAlarm.copy(isReadyToUse = false))
					cancelAlarm(newAlarm, activityContext, alarmManager) // if this fails then god help us
					return when (failureRes) {
						is AlarmControllerErrorSet.Unknown -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.ValidationFailed -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentNotFound -> ResultCustom.Failure(errorClass = failureRes)
						is AlarmControllerErrorSet.PendingIntentAlreadyExist -> ResultCustom.Failure(errorClass = failureRes)
					}
				}
			)
			updatingAlarmStateJob.await()
		}
	}


	// schedule the next alarm and if error notify the user
	suspend fun  scheduleNextAlarmInSeries(alarmIntent: AlarmActivityIntentData) {
		val alarmData: AlarmData? = alarmRepository.getAlarmById(alarmIntent.alarmIdInDb)

		if (alarmData == null) {
			logD("AlarmData not found for ID: ${alarmIntent.alarmIdInDb}")
			// ERROR: this shouldn't be true
			return
		}
		val currentTimeAlarmFired = alarmIntent.alarmTriggerTime
		val nextAlarmTime = currentTimeAlarmFired + alarmData.getFreqInMillisecond()
		logD("Current: ${getTimeInHumanReadableFormatProtectFrom0Included(currentTimeAlarmFired)}, Next: ${getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTime)}")

		if (nextAlarmTime < alarmData.endTime) {
			// put this logic in alarmController (end time calc too)
			val res = scheduleAlarm(
				alarmManager = alarmManager,
				componentActivity = context,
				receiverClass = AlarmReceiver::class.java,
				alarmData = alarmData,
				alarmTriggerTime = nextAlarmTime
			)

			res.fold(
				onSuccess = { logD("Scheduled next alarm successfully") },
				onError = { error ->
					logD("Error scheduling next alarm: ${error.internalErrorMessage}")
					errorHandler.handleError(ResultCustom.Failure(error))
					if (error is AlarmControllerErrorSet.ValidationFailed) {
						alarmRepository.updateAlarm(alarmData.copy(isReadyToUse = false))
					}
				}
			)
		} else {
			logD("No more instances to schedule for this alarm series")
		}


	}


	private fun calculateNextAlarmInfo(
			alarmData: AlarmData
	): ResultCustom<NextAlarmInfo, CalculateNextAlarmInfo> {
		return ResultCustom.runCatching(
			{ exception ->AlarmControllerErrorSet.Unknown(internalErrorMessage = exception.toString()) }
		){
			val originalSeriesStart = alarmData.startTime
			val originalSeriesEnd = alarmData.endTime
			val frequencyMillis = alarmData.getFreqInMillisecond()
			val calendarNow=Calendar.getInstance()
			val now = calendarNow.timeInMillis
			if (originalSeriesStart >= originalSeriesEnd) return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.ValidationFailed(internalErrorMessage = "The startTime: ${this@AlarmsController.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)} is not less than endTime: ${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)}"))
			when {
				now > originalSeriesStart && now > originalSeriesEnd -> {

					logD("Calculator: Series is in the past. Projecting to the next valid day.")
					val alarmSeriesDuration = originalSeriesEnd - originalSeriesStart
					val startCalendar = Calendar.getInstance().apply {
						timeInMillis = originalSeriesStart
					}
					startCalendar.apply {
						set(Calendar.DAY_OF_YEAR, calendarNow.get(Calendar.DAY_OF_YEAR))
						set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
						set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
					}
					// if we set the alarm at 3:00 - 3:10 Pm and today it is 4:00Pm so I want it to be 3:00Pm of tomorrow
					if (startCalendar.timeInMillis < now){
						logD("moved the startTIme:${getTimeInHumanReadableFormat(startCalendar.timeInMillis)} to today but it is less than currentTime:${getTimeInHumanReadableFormat(now)}, so changed it to tomorrow")
						startCalendar.add(Calendar.DAY_OF_YEAR, 1)
					}
					val newStartTime = startCalendar.timeInMillis
					val newEndTime = newStartTime + alarmSeriesDuration
					val nextAlarm = NextAlarmInfo(
						nextAlarmTriggerTime = newStartTime,
						newSeriesStartTime = newStartTime,
						newSeriesEndTime = newEndTime,
						this@AlarmsController
					)
					logD("in now > seriesStart && now > seriesEnd and the updated value is $nextAlarm")
					return@runCatching nextAlarm

				}

				now < originalSeriesStart && now < originalSeriesEnd -> {
					// the alarm is still in the future
					val nextAlarm = NextAlarmInfo(
						nextAlarmTriggerTime = originalSeriesStart,
						newSeriesStartTime = originalSeriesStart,
						newSeriesEndTime = originalSeriesEnd,
						this@AlarmsController
					)
					logD("in the now < originalSeriesStart && now < originalSeriesEnd  and the value of upcoming alarm is same -> $nextAlarm ")
					return@runCatching nextAlarm
				}
				now  in originalSeriesStart..originalSeriesEnd -> {
					// --- Scenario 3: Series is entirely in the past (or no slots were left in Scenario 2) ---
					var nextTrigger = originalSeriesStart

					while (nextTrigger <= now) {
						nextTrigger += frequencyMillis
					}
					val nextAlarm = NextAlarmInfo(
						nextAlarmTriggerTime = nextTrigger,
						newSeriesStartTime = originalSeriesStart, // The series bounds DO NOT change
						newSeriesEndTime = originalSeriesEnd,
						this@AlarmsController
					)
					logD("in the now in startTime...endTime and the upcoming alarm is $nextAlarm")
					return@runCatching nextAlarm
				}
				else ->{
					return ResultCustom.Failure(errorClass = AlarmControllerErrorSet.Unknown(internalErrorMessage = "in the Calculate Next alarm in the reset and reached the state where the alarm does not match any clause, now(${this.getTimeInHumanReadableFormatProtectFrom0Included(now)}) and originalSeriesStart(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)}) and originalSeriesEnd(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)})"))
				}
			}
		}
	}

	fun lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(alarm_start_time_to_search_db: Long, context_of_activity:Context, alarmManager:AlarmManager, message_name_for_start_time:String, message_name_for_end_time: String, alarm_end_time_to_search_db:Long, broadcastReceiverClass:BroadcastReceiver){
		val intent = Intent(context_of_activity, broadcastReceiverClass::class.java)
		// probably should hardcode message_name_for_start_time to be alarm_end_time_to_search_db and same for message_name_for_end_time
		intent.putExtra(message_name_for_start_time,alarm_start_time_to_search_db)
		intent.putExtra(message_name_for_end_time,alarm_end_time_to_search_db)
		val pendingIntent = PendingIntent.getBroadcast(context_of_activity,(alarm_end_time_to_search_db+alarm_start_time_to_search_db).toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
		alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm_end_time_to_search_db, pendingIntent)
		logD("intent -->${intent.extras} ||||| and pending intent -->${pendingIntent}\n ${Calendar.getInstance().timeInMillis < alarm_end_time_to_search_db}")
	}

	private  fun getTimeInHumanReadableFormat(t:Long): String{
		return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
	}

	private  fun getDateOnly(t:Long): String{
		return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(t))
	}

	fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
		if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
		return SimpleDateFormat("h:mm:ss a yyyy-MM-dd", Locale.getDefault()).format(Date(t))
	}

	fun getDateForDisplay(a: Long):String{
		val calendar = Calendar.getInstance().apply { timeInMillis = a }
		return  calendar.time.toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDate()
			.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
	}

	private  fun  logD(msg: String): Unit{
		Log.d("AAAAAA", "[AlarmController] $msg")
	}
}