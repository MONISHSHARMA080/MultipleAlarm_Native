package com.example.trying_native.AlarmLogic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.trying_native.Activities.AlarmActivityIntentData
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.BroadCastReceivers.AlarmInfoNotification
import com.example.trying_native.BroadCastReceivers.NextAlarmReceiver
import com.example.trying_native.LastAlarmUpdateDBReceiver
import com.example.trying_native.assertWithException
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

const val ALARM_ACTION = "com.example.trying_native.ALARM_TRIGGERED"

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
    ): Result<Unit> {
        return runCatching {
            logD("the message in the startTime is $alarmMessage")
            logD(" startTime:${getTimeInHumanReadableFormat(startTime)} and endTime:${getTimeInHumanReadableFormat(endTime)} \n")
//            val removeSecForAccuracy = 222 * 60 * 1000L // min in millisec
            val currentCalTime = timeProvider.getCurrentTime()
            val currentTimeViaCalender = currentCalTime
            logD("\n fireTime of (upcoming) alarm is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)}")
            require(startTime <  endTime) { "the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not > endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} " }
            check(currentTimeViaCalender< startTime ){"the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not greater than the current time(from cal):${this.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeViaCalender)} "}
            check( alarmData.startTime == startTimeForAlarmSeries ){"the SeriesStartTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTimeForAlarmSeries)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.startTime)} "}
            check(alarmData.endTime == endTime, {"the endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.endTime)} "})
            logD("\n++setting the pending intent of request code(startTime of alarm to int)->${startTime.toInt()} and it is in the human readable format is ${SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(startTime)) }++\n\n")

            // PI for the alarm receiver
            val resultForAlarmOpr = scope.async {getPendingIntentForAlarm(receiverClass, componentActivity, startTimeForAlarmSeries, startTime, endTime, alarmMessage, alarmData.id)}
            val resultForSettingNextAlarmOpr = scope.async {getPendingIntentForAlarm(nextAlarmReceiver, componentActivity, startTimeForAlarmSeries, startTime, endTime, alarmMessage, alarmData.id, createIntentForAlarmMetaData = false)}

            val PIForAlarm = resultForAlarmOpr.await().fold(

                onSuccess = {PI -> PI},
                onFailure = {failureRes-> throw Exception(failureRes) }
            )
            val PIForSettingNextAlarm = resultForSettingNextAlarmOpr.await().fold(
                onSuccess = {PI -> PI},
                onFailure = {failureRes-> throw Exception(failureRes) }
            )
            check(PIForAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked != null  , {" the pending intent for alarm's info. notification is null "}  )
            check(PIForSettingNextAlarm.pendingIntentToGiveUserUpcommingAlarmInfoWhenAsked == null ,{" the pending intent for setting next alarm's notification info. notification is not null "} )
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
    fun getPendingIntentForAlarm(receiverClass:Class<out BroadcastReceiver>, context: Context,
                                 startTimeForAlarmSeries:Long,startTime:Long, endTime:Long, alarmMessage: String, alarmId:Int, createIntentForAlarmMetaData:Boolean = true ): Result<PendingIntentCreated> {
        return runCatching {
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
                return Result.failure(Exception("Alarm on (${getTimeInHumanReadableFormat(startTime)}) already exists and you are trying to create new one"))
            }
            pendingIntentForAlarm = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // meaning that the pending intent does not exist and it is safe to create one
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
                if(pendingIntentForAlarmInfo == null){
                    return Result.failure(Exception("Alarm on (${getTimeInHumanReadableFormat(startTime)}) already exists and you are trying to create new one error occurred while creating the Pending intent for alarm Info "))
                }
                return Result.success(PendingIntentCreated(pendingIntentForAlarmInfo, pendingIntentForAlarm))
            }
            logD("the PI construction ran fine")
            return Result.success(PendingIntentCreated(null, pendingIntentForAlarm))
        }
    }

    // this func is called only at the first time to schedule multiple alarms
    suspend fun scheduleMultipleAlarms(alarmManager: AlarmManager,  dateInLong: Long, calendarForStartTime:Calendar, calendarForEndTime:Calendar, freqAfterTheCallback:Int, activityContext: Context, alarmDao:AlarmDao,
                                       receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, messageForDB:String, insertInDbToo: Boolean = true   ) : Result<Unit>{
    return runCatching {
        // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
        val startTimeInMillis = calendarForStartTime.timeInMillis
        val endTimeInMillis = calendarForEndTime.timeInMillis
        val freq = freqAfterTheCallback.toLong() * 60000

        assertWithException(startTimeInMillis < endTimeInMillis," the value of the start time should be < end time , you made a mistake" )
        var alarmDataForDeleting: AlarmData? = null
        try {
            // since this is our first time the startTimeForReceiverToGetTheAlarmIs->
            val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(
                startTimeInMillis, activityContext, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillis, LastAlarmUpdateDBReceiver())  }
            val c = scope.async {
                val newAlarm = AlarmData(
                    startTime = startTimeInMillis, endTime = endTimeInMillis, isReadyToUse = true, date = dateInLong,
                    message = messageForDB, frequencyInMin = freqAfterTheCallback.toLong())
                check(newAlarm.getDateFormatted(calendarForStartTime.timeInMillis) == newAlarm.getDateFormatted(calendarForEndTime.timeInMillis)){ "expected the date produced by startTime and endTime to be same, we got startDate -> ${newAlarm.getDateFormatted(newAlarm.startTime)} , endDate -> ${newAlarm.getDateFormatted(newAlarm.endTime) }"}
                if (insertInDbToo){
                    val insertedId = alarmDao.updateOrInsert(newAlarm)
                    logD("Inserted alarm with ID: $insertedId")
                }
                return@async newAlarm
            }
            b.await()
            val alarm = c.await()
            alarmDataForDeleting= alarm
            val a  =   scope.async {scheduleAlarm(startTimeInMillis, endTimeInMillis,alarmManager, activityContext,  receiverClass = receiverClass,
                startTimeForAlarmSeries = startTimeInMillis, alarmMessage = messageForDB, alarmData = alarm )  }
            val exception = a.await()
            exception.fold(onFailure = { excp ->
                this.cancelAlarm(alarm, activityContext, alarmManager)
                throw  Exception(excp.message)
            }, onSuccess = {}
            )
        }catch (e:Exception){
            logD("error occurred in the schedule multiple alarms-->${e}")
            logD("we are not able to set the alarm so we are going to cancel it all and return  ")
            // if we have gotten a error then we will need to cancel the alarm and return the exception and also delete the alarm
            try {
                if (alarmDataForDeleting!= null){
                    this.cancelAlarm(alarmDataForDeleting, activityContext, alarmManager)
                }
            }catch (e: Exception){} // lord help us!
            throw  e
        }
    }
    }

    /**given an alarm Series(or alarm, or startTime->endTime, this function will start it. If the alarm startTime is greater than current startTime then we will iterate over it and set that alarm from that time */
    suspend fun startAlarmSeries(
        alarmData: AlarmData,alarmManager: AlarmManager, activityContext: Context, alarmDao:AlarmDao,
        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, incrementTheStartTimeIfLessThanCurrentTime: Boolean = true
    ): Result<Unit>{
        return runCatching {
            val isValid =alarmData.isValid()
            if (!isValid.isValid) return Result.failure(Exception(isValid.errorMessage))
            val currentTIme = Calendar.getInstance()
            if (alarmData.endTime > currentTIme.timeInMillis ) throw Exception("Expected the endTIme to be less than current time but got endTIme:${getTimeInHumanReadableFormat(alarmData.endTime)}, currentTime:${getTimeInHumanReadableFormat(currentTIme.timeInMillis)}")
            val alarmIterator = alarmData.iteratorGeneric()
            var timeReturned = alarmIterator.next()
            while (timeReturned < currentTIme.timeInMillis && alarmIterator.hasNext()) {
                timeReturned = alarmIterator.next()
            }

            val scheduleAlarmHandler  =   scope.async {scheduleAlarm(timeReturned , alarmData.endTime, alarmManager, activityContext,  receiverClass = receiverClass,
                startTimeForAlarmSeries = alarmData.startTime, alarmMessage = alarmData.message, alarmData = alarmData )  }
            val lastIntentHandler = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(
                alarmData.startTime, activityContext, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", alarmData.endTime, LastAlarmUpdateDBReceiver())  }

            scheduleAlarmHandler.await().getOrThrow()
            lastIntentHandler.await()
        }
    }

    /** handle setting the alarms and if fails then cancel it and update the DB state not running else running */
    suspend fun startAlarmSeriesHandler(
        alarmData: AlarmData,alarmManager: AlarmManager, activityContext: Context, alarmDao:AlarmDao,
        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, incrementTheStartTimeIfLessThanCurrentTime: Boolean = true
    ): Result<Unit>{
        return runCatching {
            val result1 =scope.async {
                this@AlarmsController.startAlarmSeries(alarmData, alarmManager, activityContext, alarmDao, receiverClass, incrementTheStartTimeIfLessThanCurrentTime)
            }
            val rowsAffected = scope.async { alarmDao.updateOrInsert(alarmData.copy(isReadyToUse = true)) }
           val alarmResult = result1.await()
            rowsAffected.await() // idk
            alarmResult.fold(onSuccess = {}, onFailure = {throwable->
                this@AlarmsController.cancelAlarmHandler(alarmData, activityContext, alarmManager, alarmDao)
                throw throwable
            })
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun rescheduleAlarm(alarmManager: AlarmManager, calendarForStartTime:Calendar, calendarForEndTimer:Calendar, freqAfterCallback:Long, activityContext: Context, alarmDao:AlarmDao, alarmData:AlarmData,
                                receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, nextAlarmInfo: NextAlarmInfo ) : Result<Unit> {
        return runCatching {

            // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
            logD("in the ++scheduleMultipleAlarms2  ++ ")
            // we can't get it form the alarmData as this func is for the reset alarm and that could be only one
            val freq = freqAfterCallback * 60000
            logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
            // these are the new/incremented/updated time
            val startTimeInMillis = calendarForStartTime.timeInMillis
            val endTimeInMillis = calendarForEndTimer.timeInMillis

            val originalSeriesStartTime = alarmData.startTime
            val originalSeriesEndTime = alarmData.endTime

            var alarmDataForDeleting: AlarmData = alarmData

            val dateForDisplay =getDateForDisplay(nextAlarmInfo.newSeriesStartTime)
            assertWithException(this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime) == this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime), " startDate from " +
                    "new startSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesStartTime)} and the end date from the new endSeries time is ${this.getDateForDisplay(nextAlarmInfo.newSeriesEndTime)}")

            // cause I want to see if there is a error and if there is then I want to react
            try {
                logD("the next alarm fire time is   ${getTimeInHumanReadableFormat(startTimeInMillis)} and the end time  ${getTimeInHumanReadableFormat(endTimeInMillis)} and the date for display that we got is $dateForDisplay ")
                logD("the series start time is ${getTimeInHumanReadableFormat(originalSeriesStartTime)} and the end time  ${getTimeInHumanReadableFormat(originalSeriesEndTime)} ")

                logD("Effective Start(next/upcoming alarm): ${getTimeInHumanReadableFormat(startTimeInMillis)}, Series End: ${getTimeInHumanReadableFormat(endTimeInMillis)}")
                logD("Original Series Start: ${getTimeInHumanReadableFormat(originalSeriesStartTime)}, Original Series End: ${getTimeInHumanReadableFormat(originalSeriesEndTime)}")

                logD("(-updating DB with new time-)the new start time is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startTimeInMillis)} and the end time is ${this.getTimeInHumanReadableFormatProtectFrom0Included(endTimeInMillis)} ")
                val res = scope.async {
                    val updatedAlarm =alarmData.copy(
                        isReadyToUse = true,
                        startTime = nextAlarmInfo.newSeriesStartTime,
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
                val result = alarmSchedule.await()
                if (result.isFailure){
                    val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(originalSeriesStartTime, activityContext, alarmManager,"alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillis, LastAlarmUpdateDBReceiver())}
                    b.await()
                    throw Exception(res.getCompletionExceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                // if we have gotten an error then we will need to cancel the alarmcancelAlarmByCancelingPendingIntent and return the exception and also delete the alarm
                    this.cancelAlarm(alarmDataForDeleting, activityContext, alarmManager,)
                logD("error occurred in the schedule multiple alarms, so we are going to cancel the alarm whole, in scheduleAlarm2-->${e}")
                throw e
            }

        }
    }

    /** tries to cancel the alarm and update the Db state, if error  */
    suspend fun cancelAlarmHandler(alarmData: AlarmData, context: Context, alarmManager: AlarmManager, alarmDao: AlarmDao):Result<Unit>{
        return runCatching {
            // first update the Db as it is more visible to the user

            // the alarm was not in the DB so return
            // since the alarm is not in the DB we can't insert it and also we can't just inser it as it wasn't there and the user did not want it
             this@AlarmsController.updateAlarmStateInDb(alarmData.copy(isReadyToUse = false), alarmDao).getOrThrow()
            cancelAlarm(alarmData, context,alarmManager).fold(onSuccess = {}, onFailure = {exception ->
                // since we can't cancel it then we should just
                alarmDao.updateOrInsert(alarmData.copy(isReadyToUse = false))
                throw exception
            })
        }
    }

    /** tries to delete the alarm and update the Db state, if error  */
    suspend fun deleteAlarmHandler(alarmData: AlarmData, context: Context, alarmDao: AlarmDao, alarmManager: AlarmManager ):Result<Unit>{
        return runCatching {
            // first update the Db as it is more visible to the user, and there is a race condition here as we can't do it in parallel, as are accessing the db in failure in both
            // the alarm was not in the DB so return
            // since the alarm is not in the DB we can't insert it and also we can't just inser it as it wasn't there and the user did not wanted it
//             this@AlarmsController.updateAlarmStateInDb().getOrThrow()
            val rowsAffected =alarmDao.deleteAlarm(alarmData)
            if (rowsAffected == 0) return Result.failure(Exception("No such alarm in the Db to delete, alarmData received was $alarmData "))
            cancelAlarm(alarmData, context,alarmManager).fold(onSuccess = {}, onFailure = {exception ->
                // since we can't delete it then we should just put it back and tell the user to try to cancel it again
                alarmDao.updateOrInsert(alarmData.copy(isReadyToUse = false))
                throw exception
            })
        }
    }

    /** cancels scheduled alarm*/
    suspend fun cancelAlarm(alarmData: AlarmData, context: Context, alarmManager: AlarmManager): Result<Unit> {
        return runCatching {
            val cal = Calendar.getInstance()
            // cancel the alarm and also try to cancel a few more to be on safe side and not to encounter race conditions
            var extraSafetyAlarmsToCancel = 5
            val currentTime = cal.timeInMillis
            val alarmIterator = alarmData.iterator()
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

    private inline fun cancelPendingIntentReceiver(baseIntent: Intent, context: Context, intentData: AlarmActivityIntentData, alarmReceiverClass:Class<out BroadcastReceiver>, alarmManager: AlarmManager, intentRequestCode: Int){
        baseIntent.apply {
            setClass(context, alarmReceiverClass)
            putExtra("intentData", intentData)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, intentRequestCode, baseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    suspend fun updateAlarmStateInDb(alarmData: AlarmData, alarmDao: AlarmDao): Result<Unit>{
        return  runCatching {
            val rowsAffected = alarmDao.updateAlarm(alarmData)
            if (rowsAffected == 0) return Result.failure(Exception("No rows affected for alarmData:$alarmData"))
        }
    }

    /** try to delete the alarm fom the DB */
    suspend fun deleteAlarm(alarmData: AlarmData, context: Context, alarmDao: AlarmDao, alarmManager: AlarmManager): Result<Unit> {
        // try to delete the alarm(and cancel it) if error then put it back, and also reschedule it and display the message
        return runCatching {
            val rowsDeleted = alarmDao.deleteAlarm(alarmData.id)
            // since I have marked it unique that means the rows deleted will one or none(0) nothing else
            if (rowsDeleted == 0){
                return Result.failure(Exception("the alarm $alarmData was not found in the DB so we can't delete it"))
            }
        }
    }


    suspend fun resetAlarms(alarmData:AlarmData, alarmManager: AlarmManager, activityContext: Context, alarmDao: AlarmDao): Result<Unit>{
        return runCatching {
            logD("in the reset alarm func-+")
            val startTime = alarmData.startTime
            val endTime = alarmData.endTime
            // get the date time form the start time as I set the date in the calender instance when setting it for the startTime
            val res = this.calculateNextAlarmInfo(alarmData)
            print("\n\n----- the res from calculating the next alarm info is $res -----\n\n")
            val nextAlarmInfo = res.getOrThrow()
            check(this.getDateOnly(nextAlarmInfo.newSeriesStartTime) == this.getDateOnly(nextAlarmInfo.newSeriesEndTime)) { "startDate: ${this.getDateOnly(nextAlarmInfo.newSeriesStartTime) }   and EndTime: ${ this.getDateOnly(nextAlarmInfo.newSeriesEndTime) } of alarmSeries are not equal"}
            val endCalendar = Calendar.getInstance().apply { timeInMillis = endTime }
            val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
            val exception = this.rescheduleAlarm(
                alarmManager, activityContext = activityContext, alarmDao = alarmDao, calendarForStartTime = startCalendar,
                freqAfterCallback = alarmData.frequencyInMin,
                alarmData = alarmData, nextAlarmInfo =  nextAlarmInfo,calendarForEndTimer = endCalendar,
            )
            exception.fold(onFailure = { throwable->
                print("\n\n[EXCEPTION] got a exception in scheduling future alarms and that is -> $throwable \n\n")
                throw throwable}, onSuccess = {})
        }
    }

    private fun calculateNextAlarmInfo(alarmData: AlarmData): Result<NextAlarmInfo> {
        return runCatching {
            val originalSeriesStart = alarmData.startTime
            val originalSeriesEnd = alarmData.endTime
            val frequencyMillis = alarmData.getFreqInMillisecond()
            val now = Calendar.getInstance().timeInMillis
            check(originalSeriesStart < originalSeriesEnd) { "The startTime: ${this@AlarmsController.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)} is not less than endTime: ${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)} " }

            when {
                now > originalSeriesStart && now > originalSeriesEnd -> {
                    val alarmSeriesDuration = originalSeriesEnd - originalSeriesStart
                    val originalCal = Calendar.getInstance().apply { timeInMillis = originalSeriesStart }
                    val hour = originalCal.get(Calendar.HOUR_OF_DAY)
                    val minute = originalCal.get(Calendar.MINUTE)
                    val second = originalCal.get(Calendar.SECOND)
                    val millisecond = originalCal.get(Calendar.MILLISECOND)

                    // Create new series start with today's date but original time
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, second)
                        set(Calendar.MILLISECOND, millisecond)
                    }

                    var nextTrigger = todayCal.timeInMillis


                    // now get the time
                    // now here we will need to increment both the start time and the end time (series) as we are ahead of the alarm
                    while (nextTrigger <= now) {
                        nextTrigger += frequencyMillis
                    }
                    val nextAlarm = NextAlarmInfo(
                        nextAlarmTriggerTime = nextTrigger,
                        newSeriesStartTime = todayCal.timeInMillis,
                        newSeriesEndTime = todayCal.timeInMillis + alarmSeriesDuration, // adding the original duration
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
                    throw IllegalStateException(" in the Calculate Next alarm in the reset and reached the state where the alarm does not" +
                            "match any clause, now(${this.getTimeInHumanReadableFormatProtectFrom0Included(now)}) and originalSeriesStart(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)}) " +
                            " and originalSeriesEnd(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)})")
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