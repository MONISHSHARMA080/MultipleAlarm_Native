package com.example.trying_native.AlarmLogic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.LastAlarmUpdateDBReceiver
import com.example.trying_native.assertWithException
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.getDateForDisplay
import com.example.trying_native.incrementTheStartCalenderTimeUntilItIsInFuture
import com.example.trying_native.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val ALARM_ACTION = "com.example.trying_native.ALARM_TRIGGERED"

class AlarmsController {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    private fun scheduleAlarm(startTime: Long, endTime:Long, alarmManager:AlarmManager, componentActivity: Context, receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java, startTimeForAlarmSeries: Long, alarmMessage: String= ""  ) {
        logD( "Clicked on the schedule alarm func")
//    val triggerTime_1 = startTime
        val intent = Intent(ALARM_ACTION) // Use the action string
        logD("++++++++ receiver class in the schedule alarm is -->${receiverClass.name} +++ $receiverClass")
        logD(" in the scheduleAlarm func and the message is ->$alarmMessage")
        intent.setClass(componentActivity, receiverClass)
        logD("the startTimeForReceiverToGetTheAlarmIs is $startTimeForAlarmSeries ")
        logD("the message in the startTime is $alarmMessage")

        intent.putExtra("startTimeForDb", startTimeForAlarmSeries)
        intent.putExtra("startTime", startTime)
        intent.putExtra("endTime", endTime)
        intent.putExtra("message", alarmMessage)
        logD(" in the scheduleAlarm func and the startTime is $startTime and the startTimeForDb is $startTimeForAlarmSeries  ")
        logD("\n\n++setting the pending intent of request code(startTime of alarm to int)->${startTime.toInt()} and it is in the human readable format is ${SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(startTime)) }++\n\n")
        val pendingIntent = PendingIntent.getBroadcast(componentActivity,
            startTime.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime, pendingIntent)
    }

    // this func is called only at the first time to schedule multiple alarms
    suspend fun scheduleMultipleAlarms(alarmManager: AlarmManager, selected_date_for_display:String, date_in_long: Long,
                                       calendar_for_start_time:Calendar, calendar_for_end_time:Calendar, freq_after_the_callback:Int, activity_context: Context, alarmDao:AlarmDao,
                                       receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java,messageForDB:String   ) :Exception? {
        try {
            // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
            var startTimeInMillis = calendar_for_start_time.timeInMillis
            val startTimeInMillisendForDb= startTimeInMillis
            val start_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar_for_start_time.time)
            val start_am_pm = SimpleDateFormat("a", Locale.getDefault()).format(calendar_for_start_time.time).trim()

            var endTimeInMillis = calendar_for_end_time.timeInMillis
            val endTimeInMillisendForDb= endTimeInMillis
            val end_time_for_display = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar_for_end_time.time)
            val end_am_pm =  SimpleDateFormat("a", Locale.getDefault()).format(calendar_for_start_time.time).trim()

            logD(" \n\n am_pm_start_time-->$start_time_for_display $start_am_pm ; endtime-->$end_time_for_display $end_am_pm")
            var freq_in_milli : Long
            freq_in_milli = freq_after_the_callback.toLong()
            val freq_in_min = freq_in_milli * 60000
            logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, equal?-->${startTimeInMillis==endTimeInMillis} ::--:: freq->$freq_in_min")
            var i=0

            // alright only set one alarm here and in the receiver class set the other one after the min
            logD("checking if the start time is < end time ")
            assertWithException(startTimeInMillis < endTimeInMillis," the value of the start time should be < end time , you made a mistake" )
            logD("the start time is < endtime ")
            logD("round $i")
            logD("setting the alarm and the startTime is $startTimeInMillis and the endTime is $endTimeInMillis")
            try {
                // since this is oru first time the startTimeForReceiverToGetTheAlarmIs->
             val a  =   scope.async {scheduleAlarm(startTimeInMillis, endTimeInMillis,alarmManager, activity_context,  receiverClass = receiverClass, startTimeForAlarmSeries = startTimeInMillisendForDb, alarmMessage = messageForDB )  }
                logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
                val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(startTimeInMillisendForDb, activity_context, alarmManager, "alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillisendForDb, LastAlarmUpdateDBReceiver())  }
                val c = scope.async {
                    val newAlarm = AlarmData(
                        first_value = startTimeInMillisendForDb,
                        second_value = endTimeInMillisendForDb,
                        freq_in_min = freq_in_min,
                        isReadyToUse = true, // we have just made a new alarm do duh!!
                        date_for_display = selected_date_for_display,
                        start_time_for_display = start_time_for_display,
                        end_time_for_display = end_time_for_display,
                        start_am_pm = start_am_pm,
                        end_am_pm = end_am_pm,
                        freq_in_min_to_display = (freq_in_min / 60000).toInt(),
                        date_in_long = date_in_long,
                        message = messageForDB,
                        freqGottenAfterCallback = freq_after_the_callback.toLong()
                    )
                    val insertedId = alarmDao.insert(newAlarm)
                    logD("Inserted alarm with ID: $insertedId")
                }

                // I will not know of the exception until I await so that's why
                awaitAll(a,b,c)
            }catch (e:Exception){
                logD("error occurred in the schedule multiple alarms-->${e}")
                logD("we are not able to set the alarm so we are going to cancel it all and return  ")
                // if we have gotten a error then we will need to cancel the alarm and return the exception and also delete the alarm
                try {
                    this.cancelAlarmByCancelingPendingIntent(startTimeInMillis, endTimeInMillis, freq_in_min, alarmDao, alarmManager, activity_context, true)
                }catch (e: Exception){}
                return e
            }
            // no error in the try catch of the async block
            return null
        }catch (e: Exception){
            logD("there is a  error in the scheduleMultipleAlarms func-->${e}")
            return  e
        }
    }

    /***
     * this func is there to deactivate the already there alarm
     */
    fun deactivateAlarm(alarmDao: AlarmDao, alarmData: AlarmData,deactivate: Boolean ){
        logD("about to deactivate the alarm")
        scope.launch {
            alarmDao.updateReadyToUse(
                firstValue = alarmData.first_value,
                secondValue = alarmData.second_value,
                freqInMin = alarmData.freq_in_min,
                dateInLong = alarmData.date_in_long,
                isReadyToUse = deactivate
            )
        }

    }


    // only used in the helper func for the resting the alarms
    suspend fun scheduleMultipleAlarms2(alarmManager: AlarmManager, selected_date_for_display:String, calendar_for_start_time:Calendar, calendar_for_end_time:Calendar,
                                        freq_after_the_callback:Long, activity_context:ComponentActivity, alarmDao:AlarmDao, alarmData:AlarmData,
                                        receiverClass:Class<out BroadcastReceiver> = AlarmReceiver::class.java,   ) :Exception? {
    try {
        // should probably make some checks like if the user ST->11:30 pm today and end time 1 am tomorrow (basically should be in a day)
        logD("in the ++scheduleMultipleAlarms2  ++ ")
        // we can't get it form the alarmData as this func is for the reset alarm and that could be only one
        var startTimeInMillis = calendar_for_start_time.timeInMillis
        val startTimeInMillisendForDb = startTimeInMillis

        var endTimeInMillis = calendar_for_end_time.timeInMillis
        val endTimeInMillisendForDb = endTimeInMillis

        logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, equal?-->${startTimeInMillis==endTimeInMillis} ::--:")
        logD("\n\n\n-IIIIIIIIII----------"+" freqAfterCallback ->${freq_after_the_callback} freqAfterCallback.toLong/freq_in_milli  ->${freq_after_the_callback.toLong()} freqInMin ->${freq_after_the_callback.toLong() *60000}"+"\n\n\n---------")

        var freq_in_milli: Long
        freq_in_milli = freq_after_the_callback
        val freq_in_min = freq_in_milli * 60000
        logD("startTimeInMillis --$startTimeInMillis, endTimeInMillis--$endTimeInMillis,, startTimeInMillis >= endTimeInMillis-->${startTimeInMillis >= endTimeInMillis} ::--:: freqInMin->$freq_in_min")
        logD("is startTime > endTime ${startTimeInMillis > endTimeInMillis}, this is a assertion")

//        startTimeInMillis = startTimeInMillis + freq_in_min
        // have to use the calander one here as this is the reset function and the ine form the alarmData could be old
        assertWithException(  startTimeInMillis < endTimeInMillis, "  the value of the start time should be < end time , you made a mistake ")
        logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
        logD("the current start time(of values going in the db) is ${startTimeInMillis} human readable  ${getTimeInHumanReadableFormat(startTimeInMillis)} and the end time ${endTimeInMillis} and human readable   ${getTimeInHumanReadableFormat(endTimeInMillis)} and the date for display that we got is $selected_date_for_display ")
        logD("the current start time(of values in the alarmData) is ${alarmData.first_value} human readable  ${getTimeInHumanReadableFormat(alarmData.first_value)} and the end time ${alarmData.second_value} and human readable   ${getTimeInHumanReadableFormat(alarmData.second_value)} ")
        try {
            val b = scope.async {this@AlarmsController.lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(startTimeInMillisendForDb, activity_context, alarmManager,"alarm_start_time_to_search_db", "alarm_end_time_to_search_db", endTimeInMillisendForDb, LastAlarmUpdateDBReceiver())}
            logD("about to set lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime ")
            val updatingDB = scope.async { alarmDao.updateAlarmForReset(id= alarmData.id, firstValue =startTimeInMillis, second_value = endTimeInMillis, date_for_display =  selected_date_for_display, isReadyToUse = true, ) }
            val alarmSchedule = scope.async {  scheduleAlarm(startTimeInMillis, alarmData.second_value, alarmManager, activity_context, receiverClass = receiverClass, startTimeInMillis, alarmMessage = alarmData.message)}
            b.await()
            updatingDB.await()
            alarmSchedule.await()
        } catch (e: Exception) {
            // if we have gotten a error then we will need to cancel the alarm and return the exception and also delete the alarm
            try {
                this.cancelAlarmByCancelingPendingIntent(startTimeInMillis, endTimeInMillis, freq_in_min, alarmDao, alarmManager, activity_context, true)
            }catch (e: Exception){}
            logD("error occurred in the schedule multiple alarms, so we are going to cancel the alarm whole, in scheduleAlarm2-->${e}")
            return e
        }
        // this line added the freq in the last pending intent and now to get time for the last time we
        return null
    }catch (e: Exception){
        logD("there is a  error in the scheduleMultipleAlarms2  and it is-->${e}")
        return  e
    }
    }


// in this function the end time will not
    /**
     * this function sets the next alarm, if the alarm is ending then we will
     * @param currentAlarmTime The time the alarm that triggered this call fired.
     * @param startTimeForAlarmSeries This parameter should represent the *original* start time of the alarm series (used for DB lookup).
     */
    fun scheduleNextAlarm(
        alarmManager: AlarmManager,
        activityContext: Context,
        alarmData: AlarmData,
        receiverClass: Class<out BroadcastReceiver> = AlarmReceiver::class.java,
        currentAlarmTime: Long,
        startTimeForAlarmSeries: Long // This is the original series start time
    ): Exception? {
        logD("in the ++scheduleNextAlarm ++ with currentAlarmTime=$currentAlarmTime and originalStartTimeForDb=$startTimeForAlarmSeries")

        try {
            // Calculate the start time for the *next* alarm
            val nextAlarmTimeInMillis = currentAlarmTime + alarmData.freqGottenAfterCallback.toLong() * 60000

            // Check if the next calculated time is past the series end time
            if (nextAlarmTimeInMillis >= alarmData.second_value) {
                // means that the alarm cycle has ended
                logD("scheduleNextAlarm: Next alarm time ($nextAlarmTimeInMillis) is at or past end time (${alarmData.second_value}). Ending series.")
                logD("scheduleNextAlarm: Setting last pending intent to update DB.")


                // we do not need to schedule the last pending intent for the db as it is already done for us


            } else {
                // Alarm cycle has not ended, schedule the next alarm
                logD("scheduleNextAlarm: Scheduling next alarm at $nextAlarmTimeInMillis. Original series start time for DB: $startTimeForAlarmSeries")

                scheduleAlarm(
                    startTime = nextAlarmTimeInMillis, // This is the time the next alarm will trigger
                    endTime = alarmData.second_value, // The series end time
                    alarmManager = alarmManager,
                    componentActivity = activityContext,
                    receiverClass = receiverClass,
                    // Pass the original series start time to the next intent
                    startTimeForAlarmSeries = startTimeForAlarmSeries,
                    alarmMessage = alarmData.message
                )
            }
        } catch (e: Exception) {
            logD("scheduleNextAlarm: Error occurred: ${e}")
            return e
        }
        return null
    }


    /**
     * @param startTime -  is the alarm original start time
     */
    suspend fun cancelAlarmByCancelingPendingIntent(startTime:Long, endTime:Long, frequency_in_min:Long, alarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity: Context, delete_the_alarm_from_db:Boolean) {

        // what am I going to do it ;  make the pending intent  and call cancel on it (of course in a loop)
        val calendar = Calendar.getInstance()
        var curent_Time = calendar.timeInMillis
        var startTime = startTime


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

        // if the user cancelled the alarm when one or more alarms in the start has fired (much more succinctly before the last is fired)
        // we will to go ahead and cancel all the alarms till the last one,

        // how do we protect from calling cancel on the start one , we take the startTime and move it by the freq till it is > current time
        // eg: 4:12 -> 6:00 , freq 1 and the user called it at 4:14,

        logD("StartTime ->$startTime --- curent_Time ->$curent_Time -- freq -> $frequency_in_min ")
        while (curent_Time >= startTime ){
            startTime = startTime + frequency_in_min
            curent_Time = calendar.timeInMillis
        }

        logD("\n after the while loop for cureent time StartTime ->$startTime --- curent_Time ->$curent_Time -- freq -> $frequency_in_min ")
        logD("Hopefully working --2")

//    var intent = Intent(context_of_activity, AlarmReceiver::class.java)
//    intent.action(ALARM_ACTION)
        val intent = Intent(ALARM_ACTION)
        intent.setClass(context_of_activity, AlarmReceiver::class.java )
        var pendingIntent:PendingIntent

        while (startTime <= endTime){
            // don't have to call the schedule alarm func , create pending intent yourself
            //            scheduleAlarm(startTime,alarmManager)
            intent.putExtra("triggerTime", startTime)
            pendingIntent = PendingIntent.getBroadcast(context_of_activity, startTime.toInt(), intent, PendingIntent.FLAG_IMMUTABLE )
            pendingIntent.let { alarmManager.cancel(it); it.cancel() }
//        alarmManager.cancel(pendingIntent)
//        cancelAPendingIntent(startTime,  context_of_activity, alarmManager)
            logD("cancelling the alarm at $startTime ")
            startTime = startTime + frequency_in_min

        }
    }


    suspend fun resetAlarms(alarmData:AlarmData, alarmManager: AlarmManager, activityContext: ComponentActivity, alarmDao: AlarmDao,
                            coroutineScope: CoroutineScope):Exception?{

        // -- should probably store the message in the db to use later && delete the alarm form the db too --

        logD("in the reset alarm func-+")
        val startTime = alarmData.first_value
        val endTime = alarmData.second_value
        val calendarInstance =Calendar.getInstance()
        val currentTime =  calendarInstance.timeInMillis
        val alarmFreqInInt = (alarmData.freq_in_min / 60000 ).toInt()
        logD("alarm freq in int is :${alarmFreqInInt} and alarmData.freq_in_min / 60000 ${alarmData.freq_in_min / 60000} and alarmData.freq_in_min = ${alarmData.freq_in_min} ")
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

//        --> incrementTheStartCalenderTimeUntilItIsInFuture <-- this is the problem as the end time is going forward
            // may be take the date of the

            logD("--++---++--++--|||")
            logD("the current start time(Without  incrementing) is $startTime human readable  ${getTimeInHumanReadableFormat(startTime)} and the end time is $endTime and human readable ${getTimeInHumanReadableFormat(endTime)} ")
            endCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(endCalendar, calendarInstance)
            var startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
            logD("start time in the startCalender is -> ${startCalendar.timeInMillis}")
            startCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(startCalendar = startCalendar, currentCalendar = calendarInstance)
            logD("the current start time( with incrementing) is ${startCalendar.timeInMillis} human readable  ${getTimeInHumanReadableFormat(startCalendar.timeInMillis)} and the end time ${endCalendar.timeInMillis} and human readable   ${getTimeInHumanReadableFormat(endCalendar.timeInMillis)} ")

            logD("start time in the startCalender is -> ${startCalendar.timeInMillis} and the statement that it is greater than the current time is ${startCalendar.timeInMillis >= currentTime}")
            logD("start time is ${startCalendar.time} and the end time is ${endCalendar.time} and the freq is ${alarmData.freq_in_min} and the frequency in int is $alarmFreqInInt  ")
            // now the start time is greater that the current time we can set the alarm
//        logD("is start time > end time  ")

            // --- the problem is frequency in min to int is giving 60000, that's why the alarm is low
            val exception = this.scheduleMultipleAlarms2(
                    alarmManager,
                    activity_context = activityContext,
                    alarmDao = alarmDao,
                    calendar_for_start_time = startCalendar,
                    calendar_for_end_time = endCalendar,
                    freq_after_the_callback = alarmData.freqGottenAfterCallback,
                    selected_date_for_display = getDateForDisplay(startCalendar),
                    alarmData = alarmData,
                )
            return exception
        }
        else if (currentTime <= startTime && currentTime <= endTime){
            //  alarm is in future still:-> here we will set the alarm as it was cause the user is asking us to reset the alarm
            //  that had not gone before, eg if I set the alarm 5 hour form now and hit remove it, and want to reset it again
            // do not increment as the time is not arrived
            logD("the freq is in current time <= start time and end time ${alarmData.freq_in_min.toInt()} ")
            val endCalendar = Calendar.getInstance().apply { timeInMillis = alarmData.second_value }
            val startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
            val exception =
              this.scheduleMultipleAlarms2(
                    alarmManager,
                    activity_context = activityContext,
                    alarmDao = alarmDao,
                    calendar_for_start_time = startCalendar,
                    calendar_for_end_time = endCalendar,
                    freq_after_the_callback = alarmData.freqGottenAfterCallback,
                    selected_date_for_display = getDateForDisplay(startCalendar),
                    alarmData = alarmData,
                )
            return exception
        }
        else if (currentTime in startTime..endTime){
            // here set the alarm after the start alarm till the end time
            logD("in the 3nd one")
            var startTimeOfTheAlarm  = startTime
            val currentTimeOfTheAlarm = Calendar.getInstance().timeInMillis
            // going to increment the start time until we get it to be greater or equal than the current time
            logD("the  alarm Data is $alarmData \n }")
            while (startTimeOfTheAlarm <= currentTimeOfTheAlarm){
                startTimeOfTheAlarm+=alarmData.freq_in_min
                logD("start time changed to ${startTimeOfTheAlarm}")
            }
            logD("start time changed to ${startTimeOfTheAlarm} -- outside the while loop")
            logD("is the startTime >= endTime ${ startTimeOfTheAlarm >= endTime}  -- startTime >= secondValue ${startTimeOfTheAlarm >= alarmData.second_value}-- stratTime - $startTimeOfTheAlarm, endTime: $endTime, secondTime:${alarmData.second_value} ")
            val endCalendar = Calendar.getInstance().apply { timeInMillis = alarmData.second_value }
            // as we have changed the alarm time to be the latest time
            val startCalendar = Calendar.getInstance().apply { timeInMillis = startTimeOfTheAlarm }
            val exception =
                this.scheduleMultipleAlarms2(
                    alarmManager,
                    activity_context = activityContext,
                    alarmDao = alarmDao,
                    calendar_for_start_time = startCalendar,
                    calendar_for_end_time = endCalendar,
                    freq_after_the_callback = alarmData.freqGottenAfterCallback,
                    selected_date_for_display = getDateForDisplay(startCalendar),
                    alarmData = alarmData,
                )
            if (exception != null){
                logD("there is a exception found and it is ${exception}")
                return exception
            }
            logD("((there is a exception found and it is ${exception}")
            return null
        }
        logD("last case about to return")
        return Exception(" did not hit any of the if condition  ")
    }

    // -------
    // update it to be just get the end time and use it
    // -------
    fun lastPendingIntentWithMessageForDbOperationsWillFireAtEndTime(alarm_start_time_to_search_db: Long, context_of_activity:Context, alarmManager:AlarmManager, message_name_for_start_time:String, message_name_for_end_time: String, alarm_end_time_to_search_db:Long, broadcastReceiverClass:BroadcastReceiver){

        var intent = Intent(context_of_activity, LastAlarmUpdateDBReceiver::class.java)

        // probably should hardcode message_name_for_start_time to be alarm_end_time_to_search_db and same for message_name_for_end_time

        intent.putExtra(message_name_for_start_time,alarm_start_time_to_search_db)
        intent.putExtra(message_name_for_end_time,alarm_end_time_to_search_db)
        val pendingIntent = PendingIntent.getBroadcast(context_of_activity,(alarm_end_time_to_search_db+alarm_start_time_to_search_db).toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            alarm_end_time_to_search_db,
            pendingIntent
        )
        logD("Pending intent set with start time: $alarm_start_time_to_search_db and end time: $alarm_end_time_to_search_db")
        logD("intent -->${intent.extras} ||||| and pending intent -->${pendingIntent}\n ${Calendar.getInstance().timeInMillis < alarm_end_time_to_search_db}")
    }

    private  fun getTimeInHumanReadableFormat(t:Long): String{
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }

}