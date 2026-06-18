package com.coolApps.MultipleAlarmClock.BroadCastReceivers

import MultipleAlarmClock.alarmFeature.data.local.AlarmDao
import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationChannelType
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Error
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AlarmInfoNotificationError: Error{
    data class GenericError(override val messageToDisplayUser: String): AlarmInfoNotificationError
}

@AndroidEntryPoint
class AlarmInfoNotification: BroadcastReceiver()  {
    private val coroutineScope = CoroutineScope( Dispatchers.IO)
    private lateinit var context: Context
    val analytics by lazy {Analytics(context)}
	@Inject lateinit var alarmDao: AlarmDao


    private fun displayAlarmsMetadataInNotification(alarmData: AlarmData){
        val notificationHandler =NotificationHandler(context)
        val title = "Upcoming alarm info"
        val notificationText ="Alarm:${alarmData.id} will go from  ${getTimeInHumanReadableFormatProtectFrom0Included(alarmData.startTime)} --->  ${getTimeInHumanReadableFormatProtectFrom0Included(alarmData.endTime)} after every ${alarmData.frequencyInMin} min  "
        val notification = notificationHandler.build(NotificationChannelType.GeneralNotification,title, notificationText)
        notificationHandler.show(notification)
        analytics.captureEvent("user asked for upcoming notification meta-data notification", mapOf(
            "notificationTitle" to title,
            "notificationText" to notificationText,
            "alarmData" to alarmData.toString(),
            "class" to "AlarmInfoNotification"
        ))
    }

    /**
     * if there is a error then this function writes it to a file and also notify the user
     * */
    private fun errorOccurred(error: String){
        coroutineScope.launch {
            val notificationHandler =NotificationHandler(context)
            val errorHandler = ErrorHandler(notificationHandler, analytics )
            errorHandler.handleError(Result.Failure(AlarmInfoNotificationError.GenericError("there was an error in displaying the alarm info "), Exception(error)))
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
        val alarmData = runBlocking {alarmDao.getAlarmById(alarmId)}
        if (alarmData == null){
            // assertion failed as the intent does not have anything, logging it to a file and also displaying it in a notification
            this.errorOccurred("there alarm data from the DB is null, the alarmId was $alarmId")
            return
        }
        // now display a notification
        this.displayAlarmsMetadataInNotification(alarmData)
    }

    private  fun getTimeInHumanReadableFormatProtectFrom0Included(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }
}