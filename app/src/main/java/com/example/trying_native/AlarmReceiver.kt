package com.example.trying_native
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.trying_native.components_for_ui_compose.scheduleNextAlarm
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class AlarmReceiver : BroadcastReceiver() {
    private lateinit var context: Context
    private val coroutineScopeThatDoesNotCancel = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coroutineScope = CoroutineScope( Dispatchers.IO)
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }


    override fun onReceive(context: Context, intent: Intent) {
        logD("in the alarm receiver func and here is the intent --> $intent")
        this.context = context
        coroutineScope.launch {
            launchAlarmActivity(intent)
        }
        coroutineScopeThatDoesNotCancel.launch {
            scheduleFutureAlarm(context, alarmManager, intent)
        }
    }

    private suspend fun  scheduleFutureAlarm( activityContext: Context, alarmManager: AlarmManager, oldIntent: Intent ){
        val startTime = oldIntent.getLongExtra("startTimeForDb",0)
        val firstStartTimeOfDBObject = oldIntent.getLongExtra("startTime",0)
        val endTIme = oldIntent.getLongExtra("endTime",0)
        if ( (firstStartTimeOfDBObject+endTIme )  <= 0L){
            logD("\n ---- the startTime($startTime) or endTime($endTIme) is not valid - as they are either 0 or less that that , which should not be possible, you messed up sp bad we are crashing-----  \n")
            exitProcess(69)
        }
        logD("about to set the future alarm form the broadcast receiverthe startTime is $startTime and the endTime is $endTIme")

        // get this form the DB
        val alarmDao = getAlarmDao(context)
        val alarmData =   alarmDao.getAlarmByValues(firstStartTimeOfDBObject, endTIme)
        if (alarmData == null){
            logD("\n ---- the alarm is not found in the DB, which should not be possible, you messed up sp bad we are crashing-----  \n")
            exitProcess(69)
        }
        val execption = scheduleNextAlarm(alarmManager,  alarmData = alarmData,   activityContext = activityContext, currentAlarmTime =  startTime)
        if (execption !== null){
            logD("the alarm Execption is not null and it is ${execption.message}-----and it is ${execption} ")
        }
    }
    private  fun launchAlarmActivity(oldIntent: Intent){
        val newIntent = Intent(context, AlarmActivity::class.java)
        newIntent.putExtras(oldIntent)
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(newIntent)
    }
    private  fun getAlarmDao(context: Context): AlarmDao{
        val db = Room.databaseBuilder(
            context,
            AlarmDatabase::class.java, "alarm-database"
        ).build()
        return db.alarmDao()

    }

}