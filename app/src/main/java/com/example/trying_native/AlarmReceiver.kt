package com.example.trying_native
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Sanitizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.IntentSanitizer
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.services.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.jvm.java

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            logD("\n\n\n ------- +++ in the alarm receiver func and here is the intent --> $intent")
            logD("\n\n trying to launch the alarm activity +++++++++ \n\n")
            val newIntent = Intent(context, AlarmService::class.java).apply {
                putExtras(intent)
                action = AlarmService.ACTION_START_ALARM
            }
            context.startForegroundService(newIntent)
        }catch (e: Exception){
            logD(" failed to launch the service and got the error $e")
            // ---------------
            // handle error by sending it to postHog etc
            // ---------------
        }
    }
    private fun logD(message:String){
        Log.d("AAAAA", "[AlarmReceiver]$message")
    }

}

// -----------------------------------------
// 1) getting the alarm message; put it in the alarm Notification as message
// 2) playing alarm ringtone; in the notification and also in alarmActivity; also if the user clicks on alarm notification then we don't want the alarm ringtone to change
//
// maybe try to have a serializable data class that will decode the intent and give me the alarm data easily, solve 1 and now we need to solve 2nd
// -----------------------------------------
