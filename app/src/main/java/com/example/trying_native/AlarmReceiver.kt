package com.example.trying_native
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.trying_native.services.AlarmService
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
