package com.example.trying_native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LastAlarmUpdateDBReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
    logD("||||||+++||||||||In ----- the lastAlarmUpdateDBReceiver class and got intent extras: ${intent.getLongExtra("alarm_start_time_to_search_db",0)}, ${intent.getLongExtra("alarm_end_time_to_search_db",0L)}\n\n")
    var alarm_start_time_to_search_db = intent.getLongExtra("alarm_start_time_to_search_db",0)
    var alarm_end_time_to_search_db =  intent.getLongExtra("alarm_end_time_to_search_db",0)
    if (alarm_start_time_to_search_db.toInt() ==0 && alarm_end_time_to_search_db.toInt() == 0 ){
        logD("both are zeroes  so exiting")
        return
    }

        CoroutineScope(Dispatchers.Default).launch {
            val alarmDao = Room.databaseBuilder(
                context.applicationContext,
                AlarmDatabase::class.java, "alarm-database"
            ).build().alarmDao()

            alarmDao.updateReadyToUseInAlarm(alarm_start_time_to_search_db, alarm_end_time_to_search_db, false)
            logD("\n----updated the db to be false----\n")
        }
    }
}