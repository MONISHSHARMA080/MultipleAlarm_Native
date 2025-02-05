package com.example.trying_native

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.currentCompositionErrors
import com.example.trying_native.components_for_ui_compose.ALARM_ACTION
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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

fun resetAlarms(alarmData:AlarmData){

    // -- should probably store the message in the db to use later --

    val startTime = alarmData.first_value
    val endTime = alarmData.second_value
    val calendarInstance =Calendar.getInstance()
    val currentTime =  calendarInstance.timeInMillis

    if ( currentTime >= endTime && currentTime >= startTime ) {
        // here the user is asking us to reset the alarm for the next day on the same time of the day
    }
    else if (currentTime <= startTime && currentTime <= endTime){
        //  alarm is in future still:-> here we will set the alarm as it was cause the user is asking us to reset the alarm
        //  that had not gone before, eg if I set the alarm 5 hour form now and hit remove it, and want to reset it again
    }
    else if (currentTime in startTime..endTime){
        // here set the alarm after the start alarm till the end time
    }
}