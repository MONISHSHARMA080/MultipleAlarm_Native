package com.example.trying_native.BroadCastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.notification.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmInfoNotification: BroadcastReceiver()  {
    private val coroutineScope = CoroutineScope( Dispatchers.IO)
    private lateinit var context: Context
    private  val logFile : File by lazy {
        File(context.getExternalFilesDir(null), "alarmMetadataInfo.txt")
    }
    private fun DisplayAlarmsMetadataInNotification(alarmData: AlarmData){
        NotificationBuilder(context, title = "Upcoming alarm info",
            notificationText = "Alarm:${alarmData.id} will go from  ${getTimeInHumanReadableFormatProtectFrom0Included(alarmData.first_value)}" +
                    " --->  ${getTimeInHumanReadableFormatProtectFrom0Included(alarmData.second_value)}" +
                    " after every ${alarmData.freq_in_min_to_display} min  "
        )
    }

    /**
     * if there is a error then this function writes it to a file and also notify the user
     * */
    private fun errorOccurred(error: String){
         coroutineScope.launch {
             FileWriter(logFile, true).use {
                "\n\n -------\n" +
                        "error occurred in the AlarmInfoNotificationClass and it is --${error}" +
                        "----------\n\n\n"
             }
         }
        coroutineScope.launch {
            NotificationBuilder(context, title = "there was an error in displaying the alarm info ",
                notificationText = "error->${error}"
            )

        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
     val alarmId = intent.getIntExtra("alarmIdInDb", -1)
     if (alarmId == -1){
         // assertion failed as the intent does not have anything, logging it to a file and also displaying it in a notification
         this.errorOccurred("the alarmId is -1 or not there in the intent so we can't fetch the alarmFrom the Db")
         return
     }
        val alarmDao = this.getAlarmDao(context)
        val alarmData = runBlocking {alarmDao.getAlarmById(alarmId)}
        if (alarmData == null){
            // assertion failed as the intent does not have anything, logging it to a file and also displaying it in a notification
            this.errorOccurred("there alarm data from the DB is null, the alarmId was $alarmId")
            return
        }
        // now display a notification
        this.DisplayAlarmsMetadataInNotification(alarmData)
    }

    private  fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }

    private  fun getAlarmDao(context: Context): AlarmDao {
        val db = Room.databaseBuilder(
            context,
            AlarmDatabase::class.java, "alarm-database"
        ).build()
        return db.alarmDao()
    }
}