package com.example.trying_native
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess



class AlarmReceiver : BroadcastReceiver() {
    private lateinit var context: Context
    private val coroutineScopeThatDoesNotCancel = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coroutineScope = CoroutineScope( Dispatchers.IO)
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    private  val alarmsController = AlarmsController()
    private  val logFile = File(context.getExternalFilesDir(null), "Alarm_Receiver_logs.txt")


    override fun onReceive(context: Context, intent: Intent) {
        logD("in the alarm receiver func and here is the intent --> $intent")
        this.context = context
        runBlocking {
            scheduleFutureAlarm(context, alarmManager, intent)
        }
        coroutineScope.launch {
            launchAlarmActivity(intent)
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
            logSoundPlay("\n ---- Invalid time values received in AlarmReceiver. currentAlarmTime: $currentTimeAlarmFired, originalDbStartTime: $startTimeForAlarmSeries, originalDbEndTime: $originalDbEndTime. Crashing. ----- \n")
            exitProcess(69)
        }

        logSoundPlay("about to set the future alarm form the broadcast receiver.")
        logSoundPlay("Current alarm fired at: $currentTimeAlarmFired")
        logSoundPlay("Original DB Start Time: $startTimeForAlarmSeries")
        logSoundPlay("Original DB End Time: $originalDbEndTime")


        // get this form the DB using the original DB start and end times
        val alarmDao = getAlarmDao(context)
        // Lookup using the original start and end times
        val alarmData = alarmDao.getAlarmByValues(startTimeForAlarmSeries, originalDbEndTime)
        if (alarmData == null) {
            logSoundPlay("\n ---- the alarm is not found in the DB for start: $startTimeForAlarmSeries, end: $originalDbEndTime, which should not be possible, you messed up sp bad we are crashing----- \n")
            exitProcess(69)
        }
        logSoundPlay("the alarmData of alarm form the DB is ${alarmData}")

        // Call scheduleNextAlarm with the correct values
        // currentAlarmTime = the time the current alarm fired (to calculate the next time)
        // startTimeForReceiverToGetTheAlarmIs = the original DB start time (to pass to the next receiver for DB lookup)
        val execption = alarmsController.scheduleNextAlarm(
            alarmManager,
            alarmData = alarmData,
            activityContext = activityContext,
            currentAlarmTime = currentTimeAlarmFired, // Pass the time the current alarm fired
            startTimeForAlarmSeries = startTimeForAlarmSeries // Pass the original DB start time
        )

        if (execption !== null) {
            logSoundPlay("the alarm Exception is not null and it is ${execption.message}-----and it is ${execption} ")
        }else{
            logSoundPlay("there was no exception in scheduling the future alarm \n")
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

    private fun logSoundPlay(logMessage: String) {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "AlarmReceiver ($now) : --$logMessage --\n"
            // Append the log entry to the file
            FileWriter(logFile, true).use { writer -> writer.append(logEntry) }
            logD("Logged from AlarmReceiver: $logEntry")
        } catch (e: Exception) {
            logD("Failed to log from AlarmReceiver: ${e.message}")
        }
    }

}