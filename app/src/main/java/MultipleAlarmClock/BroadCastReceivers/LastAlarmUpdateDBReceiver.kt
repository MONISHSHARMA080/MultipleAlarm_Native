package com.example.MultipleAlarmClock.BroadCastReceivers

import MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.logD
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LastAlarmUpdateDBReceiver : BroadcastReceiver() {

	@Inject lateinit var alarmDao: AlarmDao

    override fun onReceive(context: Context, intent: Intent) {
		logD(
			"||||||+++||||||||In ----- the lastAlarmUpdateDBReceiver class and got intent extras: ${
				intent.getLongExtra(
					"alarm_start_time_to_search_db",
					0
				)
			}, ${intent.getLongExtra("alarm_end_time_to_search_db", 0L)}\n\n"
		)
    var alarm_start_time_to_search_db = intent.getLongExtra("alarm_start_time_to_search_db",0)
    var alarm_end_time_to_search_db =  intent.getLongExtra("alarm_end_time_to_search_db",0)
    if (alarm_start_time_to_search_db.toInt() ==0 && alarm_end_time_to_search_db.toInt() == 0 ){
		logD("both are zeroes  so exiting")
        return
    }
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmDao.updateReadyToUseInAlarm(alarm_start_time_to_search_db, alarm_end_time_to_search_db, false)
            }
            catch (e:Exception){
				logD("\n\n  +++++Error updating the alarm in the DB+++++ \n")
            }
			logD("\n----updated the db to be false----\n")
        }
    }
}