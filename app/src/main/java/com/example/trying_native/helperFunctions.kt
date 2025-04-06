package com.example.trying_native

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.currentCompositionErrors
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms
import com.example.trying_native.components_for_ui_compose.scheduleMultipleAlarms2
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.log

suspend fun cancelAlarmByCancelingPendingIntent(startTime:Long, endTime:Long, frequency_in_min:Long, alarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity:ComponentActivity, delete_the_alarm_from_db:Boolean) {

    // what am I going to do it ;  make the pending intent  and call cancel on it (of course in a loop)
    val calendar = Calendar.getInstance()
    var curent_Time = calendar.timeInMillis
    var startTime = startTime

    val  coroutineScope = CoroutineScope(Dispatchers.Default)

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

fun cancelAPendingIntent(startTime:Long, context_of_activity:ComponentActivity, alarmManager:AlarmManager){

    var intent = Intent(context_of_activity, AlarmReceiver::class.java)
    intent.putExtra("triggerTime", startTime)
    alarmManager.cancel(
        PendingIntent.getBroadcast(context_of_activity, startTime.toInt(),
            intent, PendingIntent.FLAG_IMMUTABLE )
    )
}

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
        //        withContext(Dispatchers.Default){}
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
        endCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(endCalendar, calendarInstance)
        var startCalendar = Calendar.getInstance().apply { timeInMillis = startTime }
        logD("start time in the startCalender is -> ${startCalendar.timeInMillis}")
        startCalendar = incrementTheStartCalenderTimeUntilItIsInFuture(startCalendar = startCalendar, currentCalendar = calendarInstance)
        logD("start time in the startCalender is -> ${startCalendar.timeInMillis} and the statement that it is greater than the current time is ${startCalendar.timeInMillis >= currentTime}")
        logD("start time is ${startCalendar.time} and the end time is ${endCalendar.time} and the freq is ${alarmData.freq_in_min} and the frequency in int is $alarmFreqInInt  ")
        // now the start time is greater that the current time we can set the alarm

        // --- the problem is frequency in min to int is giving 60000, that's why the alarm is low
        val exception = scheduleMultipleAlarms2(alarmManager, activity_context = activityContext, alarmDao = alarmDao,
            calendar_for_start_time = startCalendar, calendar_for_end_time = endCalendar, freq_after_the_callback = alarmData.freqGottenAfterCallback,
            selected_date_for_display = getDateForDisplay(startCalendar),
              message = alarmData.message, alarmData = alarmData, i = 1
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
        val exception = scheduleMultipleAlarms2(alarmManager, activity_context = activityContext, alarmDao = alarmDao,
            calendar_for_start_time = startCalendar, calendar_for_end_time = endCalendar, freq_after_the_callback = alarmData.freqGottenAfterCallback,
            selected_date_for_display = getDateForDisplay(startCalendar),
            message = alarmData.message, alarmData = alarmData, i=2
        )
        return exception
    }
    else if (currentTime in startTime..endTime){
        // here set the alarm after the start alarm till the end time
        logD("in the 3nd one")
        var startTimeOfTheAlarm  = startTime
        val currentTimeOfTheAlarm = Calendar.getInstance().timeInMillis
        // going to increment the start time until we get it to be greater or equal than the current time
        logD("the alarm freq in alarm Data is ${alarmData.freq_in_min} and the frq in milisec is ${alarmFreqInInt}")
        while (startTimeOfTheAlarm <= currentTimeOfTheAlarm){
            startTimeOfTheAlarm+=alarmData.freq_in_min
            logD("start time changed to ${startTimeOfTheAlarm}")
        }
        logD("start time changed to ${startTimeOfTheAlarm} -- outside the while loop")
        val endCalendar = Calendar.getInstance().apply { timeInMillis = alarmData.second_value }
        // as we have changed the alarm time to be the latest time
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startTimeOfTheAlarm }
        val exception = scheduleMultipleAlarms2(alarmManager, activity_context = activityContext, alarmDao = alarmDao,
            calendar_for_start_time = startCalendar, calendar_for_end_time = endCalendar, freq_after_the_callback = alarmData.freqGottenAfterCallback,
            selected_date_for_display = getDateForDisplay(startCalendar),
            message = alarmData.message, alarmData = alarmData, i=2
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

fun incrementTheStartCalenderTimeUntilItIsInFuture(startCalendar: Calendar, currentCalendar:Calendar):Calendar{
    // cause we will start the comparison from the date today else the hard limit will not work
    startCalendar.set(Calendar.DATE, currentCalendar.get(Calendar.DATE) )

    for (i in 0..100){
        logD("the loop iteration in the incrementStartCalender is ->$i")
        if (startCalendar.timeInMillis >= currentCalendar.timeInMillis){
            break
        }else{
            startCalendar.set(Calendar.DATE, currentCalendar.get(Calendar.DATE) + 1 )
        }
    }
    return startCalendar
}
fun getDateForDisplay(calendar: Calendar):String{
    return  calendar.time.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}