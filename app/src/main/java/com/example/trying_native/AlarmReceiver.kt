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

    private suspend fun scheduleFutureAlarm(activityContext: Context, alarmManager: AlarmManager, oldIntent: Intent) {
        // Correctly retrieve the time the current alarm fired (from "startTime" extra)
        val currentTimeAlarmFired = oldIntent.getLongExtra("startTime", 0)
        // Correctly retrieve the original start time for DB lookup (from "startTimeForDb" extra)
        val startTimeForAlarmSeries = oldIntent.getLongExtra("startTimeForDb", 0)
        // Correctly retrieve the original end time for DB lookup (from "endTime" extra)
        val originalDbEndTime = oldIntent.getLongExtra("endTime", 0)

        if ((currentTimeAlarmFired <= 0L) || (startTimeForAlarmSeries <= 0L) || (originalDbEndTime <= 0L)) { // Added check for originalDbStartTime/EndTime too
            logD("\n ---- Invalid time values received in AlarmReceiver. currentAlarmTime: $currentTimeAlarmFired, originalDbStartTime: $startTimeForAlarmSeries, originalDbEndTime: $originalDbEndTime. Crashing. ----- \n")
            exitProcess(69)
        }

        logD("about to set the future alarm form the broadcast receiver.")
        logD("Current alarm fired at: $currentTimeAlarmFired")
        logD("Original DB Start Time: $startTimeForAlarmSeries")
        logD("Original DB End Time: $originalDbEndTime")


        // get this form the DB using the original DB start and end times
        val alarmDao = getAlarmDao(context)
        // Lookup using the original start and end times
        val alarmData = alarmDao.getAlarmByValues(startTimeForAlarmSeries, originalDbEndTime)
        if (alarmData == null) {
            logD("\n ---- the alarm is not found in the DB for start: $startTimeForAlarmSeries, end: $originalDbEndTime, which should not be possible, you messed up sp bad we are crashing----- \n")
            exitProcess(69)
        }
        logD("the alarmData of alarm form the DB is ${alarmData}")

        // Call scheduleNextAlarm with the correct values
        // currentAlarmTime = the time the current alarm fired (to calculate the next time)
        // startTimeForReceiverToGetTheAlarmIs = the original DB start time (to pass to the next receiver for DB lookup)
        val execption = scheduleNextAlarm(
            alarmManager,
            alarmData = alarmData,
            activityContext = activityContext,
            currentAlarmTime = currentTimeAlarmFired, // Pass the time the current alarm fired
            startTimeForAlarmSeries = startTimeForAlarmSeries // Pass the original DB start time
        )

        if (execption !== null) {
            logD("the alarm Exception is not null and it is ${execption.message}-----and it is ${execption} ")
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