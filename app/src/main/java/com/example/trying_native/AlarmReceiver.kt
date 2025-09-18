package com.example.trying_native
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.trying_native.AlarmLogic.AlarmsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AlarmReceiver : BroadcastReceiver() {
    private lateinit var context: Context

    override fun onReceive(context: Context, intent: Intent) {
        logD("\n\n\n ------- +++ in the alarm receiver func and here is the intent --> $intent")
        this.context = context
//  //       val pendingResult = goAsync() // Extends execution time
        logD("\n\n trying to launch the alarm activity +++++++++ \n\n")
            launchAlarmActivity(intent)
    }

    private  fun launchAlarmActivity(oldIntent: Intent){
        val newIntent = Intent(context, AlarmActivity::class.java)
        newIntent.putExtras(oldIntent)
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(newIntent)
    }
}