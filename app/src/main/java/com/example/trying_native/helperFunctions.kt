package com.example.trying_native

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.dataBase.AlarmDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

fun cancelAlarmByCancelingPendingIntent(startTime:Long, endTime:Long, frequency_in_min:Long, alarmDao: AlarmDao, alarmManager: AlarmManager, context_of_activity:Context, delete_the_alarm_from_db:Boolean) {

    // what am I going to do it ;  make the pending intent  and call cancel on it (of course in a loop)
    val calendar = Calendar.getInstance()
    var curent_Time = calendar.timeInMillis
    var startTime = startTime

    val  coroutineScope = CoroutineScope(Dispatchers.Default)

    coroutineScope.launch(Dispatchers.IO) {

        try {
            if (delete_the_alarm_from_db == true)
            {
                alarmDao.deleteAlarmByValues(
                    firstValue = startTime,
                    secondValue = endTime
                )
            }
            else{
                alarmDao.updateReadyToUseInAlarm(
                    firstValue = startTime,
                    second_value = endTime,
                    isReadyToUse = false
                )
            }
        }
        catch (e:Exception){
            logD("error updating the isReadyToUse -->$e ")
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

    while (startTime <= endTime){
        // don't have to call the schedule alarm func , create pending intent yourself
        //            scheduleAlarm(startTime,alarmManager)
        logD("Hopefully working --in  the beginning")
        var intent = Intent(context_of_activity, AlarmReceiver::class.java)
        logD("Hopefully working --3")
        intent.putExtra("triggerTime", startTime)
        logD("Hopefully working --4")
        val pendingIntent = PendingIntent.getBroadcast(context_of_activity, startTime.toInt(), intent, PendingIntent.FLAG_IMMUTABLE )
        logD("Hopefully working --5")
        logD(" null -->${pendingIntent == null};--intent ->${intent == null}")
        alarmManager.cancel(pendingIntent)
        logD("Hopefully working --6")
        startTime = startTime + frequency_in_min
        logD("Hopefully working --7")

    }

}