package com.coolApps.MultipleAlarmClock.AlarmLogic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import com.coolApps.MultipleAlarmClock.AlarmReceiver
import com.coolApps.MultipleAlarmClock.BroadCastReceivers.AlarmInfoNotification
import com.coolApps.MultipleAlarmClock.BroadCastReceivers.NextAlarmReceiver
import com.coolApps.MultipleAlarmClock.LastAlarmUpdateDBReceiver
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.jvm.java
import com.coolApps.MultipleAlarmClock.utils.Result.Result as ResultCustom

const val ALARM_ACTION = "com.coolApps.trying_native.ALARM_TRIGGERED"

interface  TimeProvider{
    fun getCurrentTime(): Long
}
class TimeProviderImpl : TimeProvider {
    override fun getCurrentTime() = System.currentTimeMillis()
}

class AlarmsController (private val timeProvider: TimeProvider = TimeProviderImpl()){
    var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val alarmInfoNotificationClass:Class<out BroadcastReceiver> = AlarmInfoNotification::class.java
    private val alarmReceiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java
     val nextAlarmReceiver:Class<out BroadcastReceiver> = NextAlarmReceiver::class.java
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

      suspend fun scheduleAlarm(startTime: Long, endTime:Long, alarmManager:AlarmManager, componentActivity: Context, receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, startTimeForAlarmSeries: Long, alarmMessage: String= "",
                                       alarmData: AlarmData
    ): ResultCustom<Unit, scheduleAlarmError>{
          return ResultCustom.runCatching(
              defaultErrorMessage = scheduleAlarmError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")
          ){
            logD("the message in the startTime is $alarmMessage")
            logD(" startTime:${getTimeInHumanReadableFormat(startTime)} and endTime:${getTimeInHumanReadableFormat(endTime)} \n")
            val currentCalTime = timeProvider.getCurrentTime()
            val currentTimeViaCalender = currentCalTime
            logD("\n fireTime of (upcoming) alarm is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)}")
            if (startTime >= endTime)return ResultCustom.Failure(scheduleAlarmError.ProgrammerError(), internalException = Exception("the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not > endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} "))
            if (currentTimeViaCalender > startTime ) return ResultCustom.Failure( errorMessageToDisplayUser = scheduleAlarmError.ProgrammerError(),internalException = Exception("the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not greater than the current time(from cal):${this.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeViaCalender)} ") )
            if ( alarmData.startTime != startTimeForAlarmSeries ) return ResultCustom.Failure( errorMessageToDisplayUser = scheduleAlarmError.ProgrammerError(), internalException = Exception("the SeriesStartTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTimeForAlarmSeries)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.startTime)} "))
            if (alarmData.endTime != endTime)return ResultCustom.Failure(errorMessageToDisplayUser = scheduleAlarmError.ProgrammerError(), internalException = Exception("the endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.endTime)} "))
            logD("\n++setting the pending intent of request code(startTime of alarm to int)->${startTime.toInt()} and it is in the human readable format is ${SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(startTime)) }++\n\n")
            val resultForAlarmOpr = scope.async {getPendingIntentForAlarm(receiverClass, componentActivity, startTimeForAlarmSeries, startTime, endTime, alarmMessage, alarmData.id)}
            val resultForSettingNextAlarmOpr = scope.async {getPendingIntentForAlarm(nextAlarmReceiver, componentActivity, startTimeForAlarmSeries, startTime, endTime, alarmMessage, alarmData.id, createIntentForAlarmMetaData = false)}
            val PIForAlarm = resultForAlarmOpr.await().fold(
                onSuccess = {PI -> PI},
                onError = {failureRes, exception-> return ResultCustom.Failure(errorMessageToDisplayUser = scheduleAlarmError.PendingIntentNotFound(failureRes.messageToDisplayUser), internalException = Exception(exception)) }
            )
            val PIForSettingNextAlarm = resultForSettingNextAlarmOpr.await().fold(
                onSuccess = {PI -> PI},
                onError = {failureRes, exception-> return ResultCustom.Failure(errorMessageToDisplayUser = scheduleAlarmError.PendingIntentNotFound(failureRes.messageToDisplayUser), internalException = Exception(exception)) }
            )
            if (PIForAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked != null ) return ResultCustom.Failure(internalException = Exception(" the pending intent for alarm's info. notification is null "), errorMessageToDisplayUser = scheduleAlarmError.PendingIntentNotFound("Unable to set alarm, Please try again"))
            if (PIForSettingNextAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked != null  ) return ResultCustom.Failure(errorMessageToDisplayUser = scheduleAlarmError.PendingIntentNotFound(" the pending intent for setting next alarm's notification info. notification is not null "))
            logD("\n\n\n [INFO] the pending Intent for the alarm is $PIForAlarm ")
            logD("\n\n\n [INFO] the pending Intent for setting the next alarm is $PIForSettingNextAlarm ")
            // here the PI for the alarm notification
            val alarmClockInfoObject = AlarmManager.AlarmClockInfo(startTime, PIForAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked)
            alarmManager.setAlarmClock(alarmClockInfoObject, PIForAlarm.pendingIntentForAlarm)
            logD("Alarm successfully scheduled.")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, PIForSettingNextAlarm.pendingIntentForAlarm)
            logD("set the next alarm successfully")

          }
    }

    fun getPendingIntentForAlarm(
        receiverClass:Class<out BroadcastReceiver>, context: Context, startTimeForAlarmSeries:Long,startTime:Long,
        endTime:Long, alarmMessage: String, alarmId:Int, createIntentForAlarmMetaData:Boolean = true
    ): ResultCustom<PendingIntentCreated, GetPendingIntentForAlarmError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = GetPendingIntentForAlarmError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")
        ){
            val intent = Intent(ALARM_ACTION)
            val intentData = AlarmActivityIntentData(
                startTimeForDb = startTimeForAlarmSeries,
                startTime = startTime,
                endTime = endTime,
                message = alarmMessage,
                alarmIdInDb = alarmId
            )
            intent.setClass(context, receiverClass)
            intent.putExtra("intentData", intentData)
            // this is for the alarm receiver
            var pendingIntentForAlarm = PendingIntent.getBroadcast(context,
                startTime.toInt(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            logD("is pendingIntent in the scheduleAlarm() null ${pendingIntentForAlarm == null}, and it is $pendingIntentForAlarm")
            if(pendingIntentForAlarm != null){
                return ResultCustom.Failure(errorMessageToDisplayUser = GetPendingIntentForAlarmError.PendingIntentAlreadyExist(), internalException = Exception( "Alarm on (${getTimeInHumanReadableFormat(startTime)}) already exists and you are trying to create new one"))
            }
            pendingIntentForAlarm = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // meaning that the pending intent does not exist, and it is safe to create one
            logD("PendingIntent does not exist. Creating a new one.")
            val intentForAlarmMetaData:Intent = intent.clone() as Intent
            intentForAlarmMetaData.setClass(context, alarmInfoNotificationClass)
            intent.putExtra("alarmIdInDb", alarmId)
            if (createIntentForAlarmMetaData){
                val pendingIntentForAlarmInfo = PendingIntent.getBroadcast(
                    context,
                    startTime.toInt(),
                    intentForAlarmMetaData,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                if(pendingIntentForAlarmInfo == null) return ResultCustom.Failure(errorMessageToDisplayUser = GetPendingIntentForAlarmError.PendingIntentAlreadyExist(), internalException = Exception( "Alarm on (${getTimeInHumanReadableFormat(startTime)}) already exists and you are trying to create new one"))

            }
            logD("the PI construction ran fine")
            return ResultCustom.Success(PendingIntentCreated(null, pendingIntentForAlarm))
        }
    }

    /**given an alarm Series(or alarm, or startTime->endTime, this function will start it. If the alarm startTime is greater than current startTime then we will iterate over it and set that alarm from that time */
    suspend fun startAlarmSeries(
        alarmData: AlarmData, alarmManager: AlarmManager, activityContext: Context, alarmDao: AlarmDao,
        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, incrementTheStartTimeIfLessThanCurrentTime: Boolean = true
    ): ResultCustom<Unit, StartAlarmSeriesError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = StartAlarmSeriesError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")
        ){
            val alarmValidationResult = alarmData.isValid()
            if (!alarmValidationResult.isValid) return ResultCustom.Failure(errorMessageToDisplayUser = StartAlarmSeriesError.ProgrammerError(alarmValidationResult.errorMessage), internalException = Exception(alarmValidationResult.errorMessage) )
            val currentTIme = Calendar.getInstance()
            if (alarmData.endTime < currentTIme.timeInMillis ){
                val msg = "Expected the endTIme to be less than current time but got endTIme:${getTimeInHumanReadableFormat(alarmData.endTime)}, currentTime:${getTimeInHumanReadableFormat(currentTIme.timeInMillis)}"
                return  ResultCustom.Failure(errorMessageToDisplayUser = StartAlarmSeriesError.ProgrammerError(msg), internalException = Exception(msg) )
            }
            val alarmIterator = alarmData.iteratorGeneric()
            var timeReturned = alarmIterator.next()
            while (timeReturned < currentTIme.timeInMillis && alarmIterator.hasNext()) {
                timeReturned = alarmIterator.next()
            }
            val scheduleAlarmHandler  =   scope.async {scheduleAlarm(timeReturned , alarmData.endTime, alarmManager, activityContext,  receiverClass = receiverClass,
                startTimeForAlarmSeries = alarmData.startTime, alarmMessage = alarmData.message, alarmData = alarmData )  }
            val lastIntentHandler = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(
                alarmData.startTime, activityContext, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", alarmData.endTime,
                LastAlarmUpdateDBReceiver()
            )  }

            scheduleAlarmHandler.await().fold(onSuccess = {}, onError = {errorMessageForUser, exception ->
                return ResultCustom.Failure(errorMessageToDisplayUser = StartAlarmSeriesError.ErrorSchedulingAlarm(errorMessageForUser.messageToDisplayUser), internalException = Exception(exception))
            })
            lastIntentHandler.await()
        }
    }

    sealed interface AlarmValueForAlarmSeries{
        /**this is used when the user tries to edit an existing alarm*/
        data class AlarmDataType(val alarmData: AlarmData): AlarmValueForAlarmSeries
        /**this is used when the user tries to set a new alarm*/
        data class AlarmObjectType(val alarmObject: AlarmObject): AlarmValueForAlarmSeries
    }
    /** handle setting the alarms and if fails then cancel it and update the DB state not running else running */
    suspend fun startAlarmSeriesHandler(
        alarm: AlarmValueForAlarmSeries, alarmManager: AlarmManager, activityContext: Context, alarmDao:AlarmDao,
        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, incrementTheStartTimeIfLessThanCurrentTime: Boolean = true
    ): ResultCustom<Unit, StartAlarmSeriesHandlerError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = StartAlarmSeriesHandlerError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")
        ) {
            var idOfAlarmInserted: Deferred<Long>? = null
            val alarmData: AlarmData = when (alarm) {
                is AlarmValueForAlarmSeries.AlarmDataType -> {
                    idOfAlarmInserted = scope.async { alarmDao.updateOrInsert(alarm.alarmData.copy(isReadyToUse = true)) }
                    alarm.alarmData.copy(isReadyToUse = true)
                }
                is AlarmValueForAlarmSeries.AlarmObjectType -> {
                    // here the ID = 0 cause it's generated by the DB and I need a value here to convert it to AlarmData(other db is also there but I prefer this)
                    val insertId = alarmDao.insert(alarm.alarmObject.toAlarmData(id = 0, isReadyToUse = true))
                    val insertedAlarm = alarmDao.getAlarmById(insertId.toInt())
                        ?: return ResultCustom.Failure(errorMessageToDisplayUser = StartAlarmSeriesHandlerError.FailureToInsertAlarmInDb(), internalException = Exception("Failed to insert alarmObject in DB , at AlarmValueForAlarmSeries.AlarmObjectType, the alarmObject was ${alarm.alarmObject} "))
                    insertedAlarm
                }
            }
            val alarmResult = this@AlarmsController.startAlarmSeries(
                alarmData,
                alarmManager,
                activityContext,
                alarmDao,
                receiverClass,
                incrementTheStartTimeIfLessThanCurrentTime
            )
            idOfAlarmInserted?.await()
            alarmResult.fold(
                onSuccess = {}, onError = { messageForUser, exception ->
                    this@AlarmsController.cancelAlarmHandler(alarmData, activityContext, alarmManager, alarmDao)
                    return ResultCustom.Failure(errorMessageToDisplayUser = StartAlarmSeriesHandlerError.ErrorSchedulingAlarm(messageForUser.messageToDisplayUser), internalException= exception )
                }
            )
        }
    }


    /** tries to cancel the alarm and update the Db state, if error  */
    suspend fun cancelAlarmHandler(
        alarmData: AlarmData, context: Context, alarmManager: AlarmManager, alarmDao: AlarmDao
    ): ResultCustom<Unit, CancelAlarmHandlerError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = CancelAlarmHandlerError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")
        ){
            // first update the Db as it is more visible to the user
            // the alarm was not in the DB so return
            // since the alarm is not in the DB we can't insert it and also we can't just inser it as it wasn't there and the user did not want it
             this@AlarmsController.updateAlarmStateInDb(alarmData.copy(isReadyToUse = false), alarmDao).fold(onSuccess = {}, onError = { messageToDisplayUser, exception ->
                 return ResultCustom.Failure(CancelAlarmHandlerError.ErrorDeletingAlarmFromDb(messageToDisplayUser.messageToDisplayUser), exception )
             })
            cancelAlarm(alarmData, context,alarmManager).fold(onSuccess = {}, onError = {errorToDisplayUser, exception ->
                // since we can't cancel it then we should just
                alarmDao.updateOrInsert(alarmData.copy(isReadyToUse = false))
                return ResultCustom.Failure(errorMessageToDisplayUser = CancelAlarmHandlerError.CancellingAlarmError(errorToDisplayUser.messageToDisplayUser), internalException = exception)
            })
        }
    }

    /** tries to delete the alarm and update the Db state, if error  */
    suspend fun deleteAlarmHandler(
        alarmData: AlarmData, context: Context, alarmDao: AlarmDao, alarmManager: AlarmManager
    ): ResultCustom<Unit, DeleteAlarmHandlerError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = DeleteAlarmHandlerError.GenericError("Sorry, an error occurred. Please try again")
        ){
            // first update the Db as it is more visible to the user, and there is a race condition here as we can't do it in parallel, as are accessing the db in failure in both
            // the alarm was not in the DB so return
            // since the alarm is not in the DB we can't insert it and also we can't just inser it as it wasn't there and the user did not wanted it
            val rowsAffected =alarmDao.deleteAlarm(alarmData)
            if (rowsAffected == 0) return ResultCustom.Failure(DeleteAlarmHandlerError.AlarmNotInDbToDelete("Sorry, an error occurred. Please try again"), internalException = Exception(" Exception No such alarm in the Db to delete, alarmData received was alarmData, got asked to delete alarm:$alarmData   but rowsAffected were:$rowsAffected") )
            cancelAlarm(alarmData, context,alarmManager).fold(onSuccess = {}, onError = {messageToDisplayUser, exception ->
                // since we can't delete it then we should just put it back and tell the user to try to cancel it again
                when(messageToDisplayUser){
                    is CancelAlarmError.AlarmNotInDbToDelete -> {}
                    is CancelAlarmError.GenericError -> {}
                    is CancelAlarmError.ProgrammerError ->{}
                }
                alarmDao.updateOrInsert(alarmData.copy(isReadyToUse = false))

                return ResultCustom.Failure(errorMessageToDisplayUser = DeleteAlarmHandlerError.AlarmNotInDbToDelete(messageToDisplayUser.messageToDisplayUser), internalException = exception)
            })
        }
    }

    /** cancels scheduled alarm*/
    suspend fun cancelAlarm(
        alarmData: AlarmData, context: Context, alarmManager: AlarmManager
    ): ResultCustom<Unit, CancelAlarmError > {
        return ResultCustom.runCatching(
            defaultErrorMessage = CancelAlarmError.GenericError("Sorry, an error occurred. Please try again")
        ){
            val cal = Calendar.getInstance()
            // cancel the alarm and also try to cancel a few more to be on safe side and not to encounter race conditions
            var extraSafetyAlarmsToCancel = 5
            val currentTime = cal.timeInMillis
            val alarmIterator = alarmData.iterator()
            val validationResult = alarmData.isValid()
            if (!validationResult.isValid) return ResultCustom.Failure(CancelAlarmError.ProgrammerError(validationResult.errorMessage), internalException = Exception(validationResult.errorMessage) )
            while (alarmIterator.hasNext()  && extraSafetyAlarmsToCancel >0 ) {
                val alarmIterVal = alarmIterator.next()
                logD("the time value gotten in iterating is ${getTimeInHumanReadableFormatProtectFrom0Included(alarmIterVal)}")
                if (alarmIterVal < currentTime) continue
                val intentData = AlarmActivityIntentData(
                    startTimeForDb = alarmData.startTime,
                    startTime = alarmIterVal,
                    endTime = alarmData.endTime,
                    message = alarmData.message,
                    alarmIdInDb = alarmData.id
                )
                val baseIntent = Intent(ALARM_ACTION)
                cancelPendingIntentReceiver(baseIntent, context, intentData, alarmReceiverClass, alarmManager, alarmData.id)
                cancelPendingIntentReceiver(baseIntent, context, intentData, nextAlarmReceiver, alarmManager, alarmData.id)
                cancelPendingIntentReceiver(baseIntent, context, intentData, alarmReceiverClass, alarmManager, alarmData.startTime.toInt())
                // have to cancel next alarm receiver, nextAlarmReceiver, AlarmInfoNotification
                extraSafetyAlarmsToCancel--
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

    private inline fun cancelPendingIntentReceiver(
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
        alarmData: AlarmData, alarmDao: AlarmDao
    ): ResultCustom<Unit, UpdateAlarmInDbError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = UpdateAlarmInDbError.GenericError("Sorry, an error occurred. Please try again")
        ){
            val rowsAffected = alarmDao.updateAlarm(alarmData)
            if (rowsAffected == 0) return ResultCustom.Failure(UpdateAlarmInDbError.NoAlarmUpdated(), Exception("No rows affected for alarmData:$alarmData"))
        }
    }

    /** try to delete the alarm fom the DB */
    suspend fun deleteAlarm(
        alarmData: AlarmData, context: Context, alarmDao: AlarmDao, alarmManager: AlarmManager
    ): ResultCustom<Unit, DeleteAlarmInDbError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = DeleteAlarmInDbError.GenericError("Sorry, an error occurred. Please try again")
        ){
            // try to delete the alarm(and cancel it) if error then put it back, and also reschedule it and display the message
            val rowsDeleted = alarmDao.deleteAlarm(alarmData.id)
            // since I have marked it unique that means the rows deleted will one or none(0) nothing else
            if (rowsDeleted == 0){
                return ResultCustom.Failure(DeleteAlarmInDbError.NoAlarmDeleted(), internalException = Exception("the alarm $alarmData was not found in the DB so we can't delete it"))
            }
        }
    }

    suspend fun rescheduleAlarm(
        alarmManager: AlarmManager,
        freqAfterCallback:Long, activityContext: Context, alarmDao:AlarmDao, alarmData:AlarmData,
        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, nextAlarmInfo: NextAlarmInfo
    ): ResultCustom<Unit, RescheduleAlarmError> {
        return ResultCustom.runCatching(defaultErrorMessage = RescheduleAlarmError.GenericError("Sorry unable to create alarm, an error occurred. Please try again")) {
            // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
            // we can't get it form the alarmData as this func is for the reset alarm and that could be only one
            logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
            // these are the new/incremented/updated time
            var alarmDataForDeleting: AlarmData = alarmData
            if (this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime) != this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)) return ResultCustom.Failure(RescheduleAlarmError.ProgrammerError(), internalException = Exception(" startDate from new startSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime)} and the end date from the new endSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)}") )
            // cause I want to see if there is a error and if there is then I want to react
            try {
                val res = scope.async {
                    val updatedAlarm =alarmData.copy(
                        isReadyToUse = true,
                        startTime = nextAlarmInfo.newSeriesStartTime,
//                        date = nextAlarmInfo.newSeriesStartTime ,
                        endTime = nextAlarmInfo.newSeriesEndTime
                    )
                    alarmDao.updateAlarmForReset(updatedAlarm)
                    return@async updatedAlarm
                }
                val newAlarm = res.await()
                alarmDataForDeleting = newAlarm
                val alarmSchedule = scope.async {
                    scheduleAlarm(nextAlarmInfo.nextAlarmTriggerTime, nextAlarmInfo.newSeriesEndTime, alarmManager, activityContext, receiverClass = receiverClass, startTimeForAlarmSeries = nextAlarmInfo.newSeriesStartTime , alarmMessage = alarmData.message, alarmData = newAlarm.copy(
                        startTime = nextAlarmInfo.newSeriesStartTime, endTime = nextAlarmInfo.newSeriesEndTime
                    ))
                }
                 alarmSchedule.await().fold(onSuccess = {}, onError = {errorToDisplayUser, exception ->
                    this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(alarmData.startTime, activityContext, alarmManager,"alarm_start_time_to_search_db", "alarm_end_time_to_search_db", alarmData.endTime, LastAlarmUpdateDBReceiver())
                    return  ResultCustom.Failure(errorMessageToDisplayUser = RescheduleAlarmError.AlarmScheduleError(errorToDisplayUser.messageToDisplayUser), internalException = Exception(exception))
                })
            } catch (e: Exception) {
                // if we have gotten an error then we will need to cancel the alarmcancelAlarmByCancelingPendingIntent and return the exception and also delete the alarm
                this.cancelAlarm(alarmDataForDeleting, activityContext, alarmManager,)
                logD("error occurred in the schedule multiple alarms, so we are going to cancel the alarm whole, in scheduleAlarm2-->${e}")
                logD("[UNEXPECTED ERROR] in reschedule alarm failed to account for exception/error (exception here) -> $e")
                return ResultCustom.Failure(errorMessageToDisplayUser = RescheduleAlarmError.GenericError(), internalException = e)
            }
        }
    }

    suspend fun resetAlarms(
        alarmData:AlarmData, alarmManager: AlarmManager, activityContext: Context, alarmDao: AlarmDao
    ): ResultCustom<Unit, ResetAlarmError> {
        return ResultCustom.runCatching(
            defaultErrorMessage = ResetAlarmError.GenericError("Sorry, an error occurred. Please try again")
        ){
            logD("in the reset alarm func-+")
            val res = this.calculateNextAlarmInfo(alarmData)
            print("\n\n----- the res from calculating the next alarm info is $res -----\n\n")
            val nextAlarmInfo = res.fold(onSuccess = {nextAlarmInfo-> nextAlarmInfo}, onError = {errorMessageToShowUser, throwable->
                return ResultCustom.Failure(ResetAlarmError.CalculateNextAlarmError(errorMessageToShowUser.messageToDisplayUser), internalException = Exception(throwable))
            })
            if (this.getDateOnly(nextAlarmInfo.newSeriesStartTime) != this.getDateOnly(nextAlarmInfo.newSeriesEndTime) ) return ResultCustom.Failure(errorMessageToDisplayUser = ResetAlarmError.ProgrammerError(), internalException = Exception( "startDate: ${this.getDateOnly(nextAlarmInfo.newSeriesStartTime) }   and EndTime: ${ this.getDateOnly(nextAlarmInfo.newSeriesEndTime) } of alarmSeries are not equal"))
            if (this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime) != this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)) return ResultCustom.Failure(ResetAlarmError.ProgrammerError(), internalException = Exception(" startDate from new startSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime)} and the end date from the new endSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)}") )
            val newAlarm = alarmData.copy(startTime = nextAlarmInfo.newSeriesStartTime, endTime = nextAlarmInfo.newSeriesEndTime, isReadyToUse = true,
//                date = nextAlarmInfo.newSeriesStartTime
            )

            val updatingAlarmStateJob = this.scope.async {this@AlarmsController.updateAlarmStateInDb(newAlarm, alarmDao)  }
            val scheduleAlarmJob = scope.async { scheduleAlarm(nextAlarmInfo.nextAlarmTriggerTime, nextAlarmInfo.newSeriesEndTime, alarmManager, activityContext, receiverClass = alarmReceiverClass, startTimeForAlarmSeries = nextAlarmInfo.newSeriesStartTime , alarmMessage = alarmData.message, alarmData = newAlarm) }
             scheduleAlarmJob.await().fold(onSuccess = {}, onError = { messageToDisplay, exception ->
                 logD("the scheduleAlarm() failed with exception ${exception.message} and message ${messageToDisplay.messageToDisplayUser} here cancelling and returning")
                 updatingAlarmStateJob.await()// don't care about the error
                 this.updateAlarmStateInDb(newAlarm.copy(isReadyToUse = false), alarmDao)
                 cancelAlarm(newAlarm, activityContext, alarmManager) // if this fails then god help us
                return ResultCustom.Failure(ResetAlarmError.SchedulingAlarmError(messageToDisplay.messageToDisplayUser), exception)
            })
            updatingAlarmStateJob.await()
        }
    }

    private fun calculateNextAlarmInfo(
        alarmData: AlarmData
    ): ResultCustom<NextAlarmInfo, CalculateNextAlarmInfo> {
        return ResultCustom.runCatching(
            defaultErrorMessage = CalculateNextAlarmInfo.GenericError("Sorry, an error occurred. Please try again")
        ){
            val originalSeriesStart = alarmData.startTime
            val originalSeriesEnd = alarmData.endTime
            val frequencyMillis = alarmData.getFreqInMillisecond()
            val calendarNow=Calendar.getInstance()
            val now = calendarNow.timeInMillis
            if (originalSeriesStart >= originalSeriesEnd) return  ResultCustom.Failure(CalculateNextAlarmInfo.ProgrammerError(), Exception("The startTime: ${this@AlarmsController.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)} is not less than endTime: ${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)} " ))
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
                    return ResultCustom.Failure(CalculateNextAlarmInfo.IllegalStateError() , Exception(
                        " in the Calculate Next alarm in the reset and reached the state where the alarm does not" +
                                " match any clause, now(${this.getTimeInHumanReadableFormatProtectFrom0Included(now)}) and originalSeriesStart(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)}) " +
                                " and originalSeriesEnd(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)})"
                    ))
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
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
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