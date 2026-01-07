package com.example.trying_native.AlarmLogic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.BroadCastReceivers.AlarmInfoNotification
import com.example.trying_native.BroadCastReceivers.NextAlarmReceiver
import com.example.trying_native.LastAlarmUpdateDBReceiver
import com.example.trying_native.assertWithException
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.incrementTheStartCalenderTimeUntilItIsInFuture
import com.example.trying_native.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

const val ALARM_ACTION = "com.example.trying_native.ALARM_TRIGGERED"

class AlarmsController {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val alarmInfoNotificationClass:Class<out BroadcastReceiver> = AlarmInfoNotification::class.java
    private val alarmReceiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java
    private val nextAlarmReceiver:Class<out BroadcastReceiver> = NextAlarmReceiver::class.java
    /**
     * A simple data class to hold the result of calculating the next alarm trigger.
     * @param nextAlarmTriggerTime The exact epoch milliseconds for the next alarm.
     * @param newSeriesStartTime The calculated start time for the next series instance (e.g., tomorrow at 9 AM).
     * @param newSeriesEndTime The calculated end time for the next series instance (e.g., tomorrow at 5 PM).
     */
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
    /**
     * data class for the return value of the pending intent
     * [pendingIntentForAlarmNotificationInfo] - this will be there(not null) when the pending intent is for the alarm and not for the
     * [pendingIntentForAlarm] - pending intent for the upcoming alarm
     */
    data class PendingIntentCreated(
        val pendingIntentForAlarmNotificationInfo: PendingIntent?,
        val pendingIntentForAlarm: PendingIntent
    ){
        override fun toString(): String {
            return "pendingIntentForAlarmNotificationInfo--$pendingIntentForAlarmNotificationInfo --  pendingIntentForAlarm--$pendingIntentForAlarm"
        }
    }

    public suspend fun scheduleAlarm(startTime: Long, endTime:Long, alarmManager:AlarmManager, componentActivity: Context, receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, startTimeForAlarmSeries: Long, alarmMessage: String= "",
                                      alarmData: AlarmData
    ): Result<Unit> {
        return runCatching {
            logD("the message in the startTime is $alarmMessage")
            logD(" startTime:${getTimeInHumanReadableFormat(startTime)} and endTime:${getTimeInHumanReadableFormat(endTime)} \n")
            val removeSecForAccuracy = 222 * 60 * 1000L // min in millisec
            val currentCalTime = Calendar.getInstance().timeInMillis
            // removing milliseconds cause when we try to schedule the next alarm it will get a bit behind
            // also convert this to a switch statement
            val currentTimeViaCalender = currentCalTime - removeSecForAccuracy
            logD("the current time via calender is: ${this.getTimeInHumanReadableFormatProtectFrom0Included(currentCalTime)}" +
                    " \n and Current time(adjusted for some min diff) is: ${this.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeViaCalender)}  " +
                    "\n startTime of alarm is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)}")
            require(startTime <  endTime) { "the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not > endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} " }
            check(currentTimeViaCalender< startTime ){"the startTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTime)} is not greater than the current time(from cal):${this.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeViaCalender)} "}
            check( alarmData.startTime == startTimeForAlarmSeries ){"the SeriesStartTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(startTimeForAlarmSeries)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.startTime)} "}
            check(alarmData.endTime == endTime, {"the endTime:${this.getTimeInHumanReadableFormatProtectFrom0Included(endTime)} is not same as the one from the alarmData(DB):${this.getTimeInHumanReadableFormatProtectFrom0Included(alarmData.endTime)} "})
            logD(" in the scheduleAlarm func and the startTime is $startTime and the startTimeForDb is $startTimeForAlarmSeries  ")
            logD("\n\n++setting the pending intent of request code(startTime of alarm to int)->${startTime.toInt()} and it is in the human readable format is ${SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(startTime)) }++\n\n")

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
            check(PIForAlarm.pendingIntentForAlarmNotificationInfo != null  , {" the pending intent for alarm's info. notification is null "}  )
            check(PIForSettingNextAlarm.pendingIntentForAlarmNotificationInfo == null ,{" the pending intent for setting next alarm's notification info. notification is not null "} )
            logD("\n\n\n [INFO] the pending Intent for the alarm is $PIForAlarm ")
            logD("\n\n\n [INFO] the pending Intent for setting the next alarm is $PIForSettingNextAlarm ")

            val alarmClockInfoObject = AlarmManager.AlarmClockInfo(startTime, PIForAlarm.pendingIntentForAlarmNotificationInfo)
            alarmManager.setAlarmClock(alarmClockInfoObject, PIForAlarm.pendingIntentForAlarm)
            logD("Alarm successfully scheduled.")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, PIForSettingNextAlarm.pendingIntentForAlarm)
            logD("set the next alarm successfully")

        }
    }
    /**
     * returns the Pending Intent that will be delivered to a receiver. The PendingIntent has the same properties(key-value)
     * @param receiverClass class that will get the pending intent
     * @param newSeriesStartTime The calculated start time for the next series instance (e.g., tomorrow at 9 AM).
     * @param newSeriesEndTime The calculated end time for the next series instance (e.g., tomorrow at 5 PM).
     */
    fun getPendingIntentForAlarm(receiverClass:Class<out BroadcastReceiver>, context: Context,
                                 startTimeForAlarmSeries:Long,startTime:Long, endTime:Long, alarmMessage: String, alarmId:Int, createIntentForAlarmMetaData:Boolean = true ): Result<PendingIntentCreated> {
        return runCatching {
            val intent = Intent(ALARM_ACTION) // Use the action string
            intent.setClass(context, receiverClass)
            intent.putExtra("startTimeForDb", startTimeForAlarmSeries)
            intent.putExtra("startTime", startTime)
            intent.putExtra("endTime", endTime)
            intent.putExtra("message", alarmMessage)
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
    suspend fun scheduleMultipleAlarms(alarmManager: AlarmManager,  dateInLong: Long,
                                       calendarForStartTime:Calendar, calendarForEndTime:Calendar, freqAfterTheCallback:Int, activityContext: Context, alarmDao:AlarmDao,
                                       receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, messageForDB:String   ) : Result<Unit>{
    return runCatching {
        // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
        var startTimeInMillis = calendarForStartTime.timeInMillis
        val startTimeInMillisendForDb= startTimeInMillis
        val endTimeInMillis = calendarForEndTime.timeInMillis
        val endTimeInMillisecondForDb= endTimeInMillis
        val freq = freqAfterTheCallback.toLong() * 60000
        logD("checking if the start time is < end time ")

        assertWithException(startTimeInMillis < endTimeInMillis," the value of the start time should be < end time , you made a mistake" )
        logD("the start time is < endtime ")
        logD("setting the alarm and the startTime is $startTimeInMillis and the endTime is $endTimeInMillis")
        var alarmDataForDeleting: AlarmData? = null
        try {
            // since this is our first time the startTimeForReceiverToGetTheAlarmIs->
            logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
            val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(startTimeInMillisendForDb, activityContext, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillisecondForDb, LastAlarmUpdateDBReceiver())  }
            val c = scope.async {
                val newAlarm = AlarmData(
                    startTime = startTimeInMillisendForDb,
                    endTime = endTimeInMillisecondForDb,
                    isReadyToUse = true,
                    date = dateInLong,
                    message = messageForDB,
                    freqGottenAfterCallback = freqAfterTheCallback.toLong()
                )
                check(newAlarm.getDateFormatted(calendarForStartTime.timeInMillis) == newAlarm.getDateFormatted(calendarForEndTime.timeInMillis)){ "expected the date produced by startTime and endTime to be same, we got startDate -> ${newAlarm.getDateFormatted(newAlarm.startTime)} , endDate -> ${newAlarm.getDateFormatted(newAlarm.endTime) }"}

                val insertedId = alarmDao.insert(newAlarm)
                logD("Inserted alarm with ID: $insertedId")
                return@async newAlarm
            }

            // I will not know of the exception until I await so that's why
            b.await()
            val alarm = c.await()
            alarmDataForDeleting= alarm
            val a  =   scope.async {scheduleAlarm(startTimeInMillis, endTimeInMillis,alarmManager, activityContext,  receiverClass = receiverClass, startTimeForAlarmSeries = startTimeInMillisendForDb, alarmMessage = messageForDB, alarmData = alarm )  }
            val exception = a.await()
            exception.fold(onFailure = { excp ->
                this.cancelAlarmByCancelingPendingIntent(startTimeInMillis, endTimeInMillis, freq, alarmDao, alarmManager, activityContext, false, alarmData = alarm)
                throw  Exception(excp.message)
            }, onSuccess = {}
            )
        }catch (e:Exception){
            logD("error occurred in the schedule multiple alarms-->${e}")
            logD("we are not able to set the alarm so we are going to cancel it all and return  ")
            // if we have gotten a error then we will need to cancel the alarm and return the exception and also delete the alarm
            try {
                if (alarmDataForDeleting!= null){
                    this.cancelAlarmByCancelingPendingIntent(startTimeInMillis, endTimeInMillis, freq, alarmDao, alarmManager, activityContext, false, alarmData = alarmDataForDeleting)
                }
            }catch (e: Exception){ // lord help us!
            }
            throw  e
        }
        // no error in the try catch of the async block
    }
    }


    suspend fun scheduleMultipleAlarms2(alarmManager: AlarmManager, calendarForStartTime:Calendar, calendarForEndTimer:Calendar,
                                        freqAfterCallback:Long, activityContext:ComponentActivity, alarmDao:AlarmDao, alarmData:AlarmData,
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

                //TO-DO: why 2 db calls when we can accomplish this in one
                val res = scope.async {
                    alarmDao.updateAlarmForReset(alarmData.copy(isReadyToUse = true))
                    return@async alarmData.copy(isReadyToUse = true)
                }
                val newAlarm = res.await()
                alarmDataForDeleting = newAlarm
                val alarmSchedule = scope.async {  scheduleAlarm(nextAlarmInfo.nextAlarmTriggerTime, nextAlarmInfo.newSeriesEndTime, alarmManager, activityContext, receiverClass = receiverClass, startTimeForAlarmSeries = nextAlarmInfo.newSeriesStartTime , alarmMessage = alarmData.message, alarmData = newAlarm)}
                val result = alarmSchedule.await()
                if (result.isFailure){
                    val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(originalSeriesStartTime, activityContext, alarmManager,"alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillis, LastAlarmUpdateDBReceiver())}
                    b.await()
                    throw Exception("Error in scheduling the alarm")
                }

            } catch (e: Exception) {
                // if we have gotten a error then we will need to cancel the alarm and return the exception and also delete the alarm
                try {
                    this.cancelAlarmByCancelingPendingIntent(startTimeInMillis, endTimeInMillis, freq, alarmDao, alarmManager, activityContext, false, alarmData = alarmDataForDeleting)
                }catch (e: Exception){}
                logD("error occurred in the schedule multiple alarms, so we are going to cancel the alarm whole, in scheduleAlarm2-->${e}")
                throw e
            }

        }
    }


    /**
     * @param startTime -  is the alarm original start time
     * @param alarmData - is required for the providing the ID of the alarm in the DB
     */
    suspend fun cancelAlarmByCancelingPendingIntent(startTime:Long, endTime:Long, frequency_in_min:Long, alarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity: Context, delete_the_alarm_from_db:Boolean, alarmData: AlarmData) {

        val calendar = Calendar.getInstance()
        var currentTime = calendar.timeInMillis
        var currentStartTime = startTime

        // Update database first
        withContext(Dispatchers.IO) {
            try {
                if (delete_the_alarm_from_db) {
                    alarmDao.deleteAlarmByValues(firstValue = startTime, secondValue = endTime)
                } else {
                    alarmDao.updateReadyToUseInAlarm(firstValue = startTime, second_value = endTime, isReadyToUse = false)
                }
            } catch (e: Exception) {
                logD("Error updating the database: $e")
            }
        }

        logD("StartTime ->$currentStartTime --- currentTime ->$currentTime -- freq -> $frequency_in_min ")

        // Advance startTime to the next pending alarm (skip already fired alarms)
        while (currentTime >= currentStartTime ){
            currentStartTime = currentStartTime + frequency_in_min
            currentTime = calendar.timeInMillis
        }

        logD("After adjustment - StartTime ->$currentStartTime --- currentTime ->$currentTime")

        // Cancel all remaining AlarmReceiver PendingIntents
        val alarmReceiverIntent = Intent(ALARM_ACTION)
        alarmReceiverIntent.setClass(context_of_activity, alarmReceiverClass)

        var tempStartTime = currentStartTime
        while (tempStartTime <= endTime) {
            // Add the same extras as when creating the alarm
            alarmReceiverIntent.putExtra("startTimeForDb", startTime) // Original series start time
            alarmReceiverIntent.putExtra("startTime", tempStartTime)
            alarmReceiverIntent.putExtra("endTime", endTime)
            alarmReceiverIntent.putExtra("message", alarmData.message)
            alarmReceiverIntent.putExtra("alarmIdInDb", alarmData.id)

            // Use the same request code as when creating: startTime.toInt() for AlarmReceiver
            val alarmReceiverPI = PendingIntent.getBroadcast(
                context_of_activity,
                alarmData.id, // This matches the request code used in getPendingIntentForAlarm
                alarmReceiverIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            if (alarmReceiverPI != null) {
                alarmManager.cancel(alarmReceiverPI)
                alarmReceiverPI.cancel()
                logD("Cancelled AlarmReceiver alarm at $tempStartTime")
            }

            tempStartTime += frequency_in_min
        }

        // Cancel all remaining NextAlarmReceiver PendingIntents
        val nextAlarmReceiverIntent = Intent(ALARM_ACTION)
        nextAlarmReceiverIntent.setClass(context_of_activity, nextAlarmReceiver)

        tempStartTime = currentStartTime
        while (tempStartTime <= endTime) {
            // Add the same extras as when creating the next alarm
            nextAlarmReceiverIntent.putExtra("startTimeForDb", startTime) // Original series start time
            nextAlarmReceiverIntent.putExtra("startTime", tempStartTime)
            nextAlarmReceiverIntent.putExtra("endTime", endTime)
            nextAlarmReceiverIntent.putExtra("message", alarmData.message)
            nextAlarmReceiverIntent.putExtra("alarmIdInDb", alarmData.id)

            // Use alarmData.id as request code for NextAlarmReceiver (as seen in your code)
            val nextAlarmPI = PendingIntent.getBroadcast(
                context_of_activity,
                alarmData.id,
                nextAlarmReceiverIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            if (nextAlarmPI != null) {
                alarmManager.cancel(nextAlarmPI)
                nextAlarmPI.cancel()
                logD("Cancelled NextAlarmReceiver alarm at $tempStartTime")
            }

            tempStartTime += frequency_in_min
        }

        // Also cancel the AlarmClockInfo notification PendingIntents
        val alarmInfoIntent = Intent(ALARM_ACTION)
        alarmInfoIntent.setClass(context_of_activity, alarmInfoNotificationClass)

        tempStartTime = currentStartTime
        while (tempStartTime <= endTime) {
            alarmInfoIntent.putExtra("startTimeForDb", startTime)
            alarmInfoIntent.putExtra("startTime", tempStartTime)
            alarmInfoIntent.putExtra("endTime", endTime)
            alarmInfoIntent.putExtra("message", alarmData.message)
            alarmInfoIntent.putExtra("alarmIdInDb", alarmData.id)

            // Use startTime.toInt() as request code for alarm info notifications
            val alarmInfoPI = PendingIntent.getBroadcast(
                context_of_activity,
                tempStartTime.toInt(),
                alarmInfoIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            if (alarmInfoPI != null) {
                alarmManager.cancel(alarmInfoPI)
                alarmInfoPI.cancel()
                logD("Cancelled AlarmInfo notification at $tempStartTime")
            }

            tempStartTime += frequency_in_min
        }

        logD("Finished cancelling all alarm PendingIntents")
    }

    suspend fun resetAlarms(alarmData:AlarmData, alarmManager: AlarmManager, activityContext: ComponentActivity, alarmDao: AlarmDao): Result<Unit>{
        return runCatching {

            logD("in the reset alarm func-+")
            val startTime = alarmData.startTime
            val endTime = alarmData.endTime
            val calendarInstance =Calendar.getInstance()
            val currentTime =  calendarInstance.timeInMillis
            // get the date time form the start time as I set the date in the calender instance when setting it for the startTime
            if ( currentTime >= endTime && currentTime >= startTime ) {
                logD("in the current time  greater than the start time and the end time")

                // here the user is asking us to reset the alarm for the next day on the same time of the day
                // also the date_in_Long on the alarmData is right, do I need to set the date to be on the
                // also imagine alarm was on tuesday form 12:00 --> 14:00 and now the time is 18:00 of the same day, here I
                // can set the date to be the next day
                // or the time is 10:00 of the next day, here I can't set the date to be next day
                // or the time is 13:00 of the next day, here I can set the alarm o
                // how are we telling the diff b/w them ->

                // get the date of the alarm, and the start and the end time; now see if
                // see if the current date is more or less than the date of the alarm,
                // the start time of the day is behind

                // -- or -- get the current time form incrementing the date of the calender, if the prev_time >= currentTime set
                // the alarm if <= current time then increment the date to be plus one and if still not then exit
                var endCalendar = Calendar.getInstance().apply { timeInMillis = endTime }
                var startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }

                logD("(-before incrementing-)the start time  is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis)} and the end time is ${this.getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)} ")

                endCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(endCalendar, calendarInstance)
                startCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(startCalendar = startCalendar, currentCalendar = calendarInstance)

                logD("(-after incrementing-)the start time  is ${this.getTimeInHumanReadableFormatProtectFrom0Included(startCalendar.timeInMillis)} and the end time is ${this.getTimeInHumanReadableFormatProtectFrom0Included(endCalendar.timeInMillis)} ")


                // -- here what I need to do is to provide the scheduleAlarm2 with the new series start/end time and the nextAlarmFireTime for
                // -- each if block in the reset alarm function
                val res = this.calculateNextAlarmInfo(alarmData)
                val nextAlarmInfo = res.getOrThrow()
                val exception = this.scheduleMultipleAlarms2(
                    alarmManager,
                    activityContext = activityContext,
                    alarmDao = alarmDao,
                    calendarForStartTime = startCalendar,
                    calendarForEndTimer = endCalendar,
                    freqAfterCallback = alarmData.freqGottenAfterCallback,
                    alarmData = alarmData,
                    nextAlarmInfo =  nextAlarmInfo,
                )
                exception.fold(onFailure = { throwable->
                    throw throwable}, onSuccess = {})
            }
            else if (currentTime <= startTime && currentTime <= endTime){
                //  alarm is in future still:-> here we will set the alarm as it was cause the user is asking us to reset the alarm
                //  that had not gone before, eg if I set the alarm 5 hour form now and hit remove it, and want to reset it again
                // do not increment as the time is not arrived
                logD("the freq is in current time <= start time and end time ${alarmData.freqGottenAfterCallback.toInt()} ")
                val endCalendar = Calendar.getInstance().apply { timeInMillis = alarmData.endTime }
                val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }

                val res = this.calculateNextAlarmInfo(alarmData)
                val nextAlarmInfo = res.getOrThrow()

                val exception = this.scheduleMultipleAlarms2(
                    alarmManager,
                    activityContext = activityContext,
                    alarmDao = alarmDao,
                    calendarForStartTime = startCalendar,
                    calendarForEndTimer = endCalendar,
                    freqAfterCallback = alarmData.freqGottenAfterCallback,
                    alarmData = alarmData,
                    nextAlarmInfo =  nextAlarmInfo,
                )
                exception.fold(onFailure = { throwable->
                    throw throwable}, onSuccess = {})
            }
            else if (currentTime in startTime..endTime){
                // here set the alarm after the start alarm till the end time
                logD("in the 3nd one")
                var startTimeOfTheAlarm  = startTime
                val currentTimeOfTheAlarm = Calendar.getInstance().timeInMillis
                // going to increment the start time until we get it to be greater or equal than the current time
                logD("the  alarm Data is $alarmData \n }")
                while (startTimeOfTheAlarm <= currentTimeOfTheAlarm){
                    startTimeOfTheAlarm+=alarmData.getFreqInMillisecond()
                    logD("start time changed to ${startTimeOfTheAlarm}")
                }
                logD("start time changed to ${startTimeOfTheAlarm} -- outside the while loop")
                logD("is the startTime >= endTime ${ startTimeOfTheAlarm >= endTime}  -- startTime >= secondValue ${startTimeOfTheAlarm >= alarmData.endTime}-- stratTime - $startTimeOfTheAlarm, endTime: $endTime, secondTime:${alarmData.endTime} ")
                val endCalendar = Calendar.getInstance().apply { timeInMillis = alarmData.endTime }
                // as we have changed the alarm time to be the latest time
                val startCalendar = Calendar.getInstance().apply { timeInMillis = startTimeOfTheAlarm }

                val res = this.calculateNextAlarmInfo(alarmData)
                val nextAlarmInfo = res.getOrThrow()

                val exception = this.scheduleMultipleAlarms2(
                        alarmManager,
                        activityContext = activityContext,
                        alarmDao = alarmDao,
                        calendarForStartTime = startCalendar,
                        calendarForEndTimer = endCalendar,
                        freqAfterCallback = alarmData.freqGottenAfterCallback, alarmData = alarmData,
                        nextAlarmInfo =  nextAlarmInfo
                    )
                exception.fold(onFailure = { throwable->
                    throw throwable}, onSuccess = {})
            }else{
                throw Exception(" did not hit any of the if condition ")
            }

        }
    }
    fun lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(alarm_start_time_to_search_db: Long, context_of_activity:Context, alarmManager:AlarmManager, message_name_for_start_time:String, message_name_for_end_time: String, alarm_end_time_to_search_db:Long, broadcastReceiverClass:BroadcastReceiver){

        var intent = Intent(context_of_activity, LastAlarmUpdateDBReceiver::class.java)

        // probably should hardcode message_name_for_start_time to be alarm_end_time_to_search_db and same for message_name_for_end_time

        intent.putExtra(message_name_for_start_time,alarm_start_time_to_search_db)
        intent.putExtra(message_name_for_end_time,alarm_end_time_to_search_db)
        val pendingIntent = PendingIntent.getBroadcast(context_of_activity,(alarm_end_time_to_search_db+alarm_start_time_to_search_db).toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm_end_time_to_search_db, pendingIntent)
        logD("Pending intent set with start time: $alarm_start_time_to_search_db and end time: $alarm_end_time_to_search_db")
        logD("intent -->${intent.extras} ||||| and pending intent -->${pendingIntent}\n ${Calendar.getInstance().timeInMillis < alarm_end_time_to_search_db}")
    }

    private  fun getTimeInHumanReadableFormat(t:Long): String{
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }
    private  fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }

    /** this func returns the time that is in human readable format  and for display, eg 10:50 ***WITHOUT*** am or pm*/
    private  fun getDisplayTimeWithoutAMPM(cal: Calendar ): String{
       return SimpleDateFormat("hh:mm", Locale.getDefault()).format(cal.time)
    }
    /** this func returns the time that is in human readable format  and for display, eg 10:50 ***WITHOUT*** am or pm
     *  where [cal] is the Long that will be converted into Calender of [cal] time in millis..  internally
     */
    public  fun getDisplayTimeWithoutAMPM(cal: Long ): String{
        val cal = Calendar.getInstance().apply { timeInMillis = cal }
        return SimpleDateFormat("hh:mm", Locale.getDefault()).format(cal.time)
    }

    private fun getDateForDisplay(calendar: Calendar):String{
        return  calendar.time.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
    fun getDateForDisplay(a: Long):String{
        val calendar = Calendar.getInstance().apply { timeInMillis = a }
        return  calendar.time.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }


    /**
     * Calculates the next valid trigger time and series bounds for a given alarm definition.
     * This is a pure function; it does not modify any state or database.
     *
     * @param alarmData The alarm's immutable definition from the database.
     * @return A [NextAlarmInfo] object containing the next trigger time and the bounds for that
     * series instance, or null if no future alarms can be scheduled.
     */
    private fun calculateNextAlarmInfo(alarmData: AlarmData): Result<NextAlarmInfo> {
        return runCatching {
            val originalSeriesStart = alarmData.startTime
            val originalSeriesEnd = alarmData.endTime
            val frequencyMillis = alarmData.getFreqInMillisecond()
            val now = Calendar.getInstance().timeInMillis

            when {
                now < originalSeriesStart && now < originalSeriesEnd -> {
                    // the alarm is still in the future
                    logD("Calculator: Series is in the future. First trigger will be the original start time. and the NextAlarmInfo is ${NextAlarmInfo(nextAlarmTriggerTime = originalSeriesStart, newSeriesStartTime = originalSeriesStart, newSeriesEndTime = originalSeriesEnd, this@AlarmsController)}")
                    return@runCatching NextAlarmInfo(
                        nextAlarmTriggerTime = originalSeriesStart,
                        newSeriesStartTime = originalSeriesStart,
                        newSeriesEndTime = originalSeriesEnd,
                        this@AlarmsController
                    )
                }
                now >= originalSeriesStart && now < originalSeriesEnd -> {
                    var nextTrigger = originalSeriesStart
                    while (nextTrigger <= now) {
                        nextTrigger += frequencyMillis
                    }
                    logD("now>= originalSeriesStart && now < originalSeriesEnd : Found next trigger at ${getTimeInHumanReadableFormat(nextTrigger)}")
                    logD("the NextAlarmInfo is ${NextAlarmInfo(nextAlarmTriggerTime = nextTrigger, newSeriesStartTime = originalSeriesStart, newSeriesEndTime = originalSeriesEnd, this@AlarmsController)}")
                    return@runCatching NextAlarmInfo(
                        nextAlarmTriggerTime = nextTrigger,
                        newSeriesStartTime = originalSeriesStart, // The series bounds DO NOT change
                        newSeriesEndTime = originalSeriesEnd,
                        this@AlarmsController
                    )
                }
                now > originalSeriesEnd -> {
                    // --- Scenario 3: Series is entirely in the past (or no slots were left in Scenario 2) ---
                    logD("Calculator: Series is in the past. Projecting to the next valid day.")
                    val startCalendar = Calendar.getInstance().apply { timeInMillis = originalSeriesStart }

                    // Keep adding one day until the start time is in the future
                    while (startCalendar.timeInMillis <= now) {
                        startCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val newStartTime = startCalendar.timeInMillis
                    val duration = originalSeriesEnd - originalSeriesStart
                    val newEndTime = newStartTime + duration
                    logD("the NextAlarmInfo is ${NextAlarmInfo(nextAlarmTriggerTime = newStartTime, newSeriesStartTime = newStartTime, newSeriesEndTime = newEndTime, this@AlarmsController)}")

                    return@runCatching NextAlarmInfo(
                        nextAlarmTriggerTime = newStartTime,
                        newSeriesStartTime = newStartTime, // The series bounds DO NOT change
                        newSeriesEndTime = newEndTime,
                        this@AlarmsController
                    )
                }
                else ->{
                    throw IllegalStateException(" in the Calculate Next alarm in the reset and reached the state where the alarm does not" +
                            "match any clause, now(${this.getTimeInHumanReadableFormatProtectFrom0Included(now)}) and originalSeriesStart(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesStart)}) " +
                            " and originalSeriesEnd(${this.getTimeInHumanReadableFormatProtectFrom0Included(originalSeriesEnd)})")
                }
            }
        }
    }

}





