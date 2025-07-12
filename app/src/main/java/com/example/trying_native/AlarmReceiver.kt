package com.example.trying_native
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
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
    private  val logFile : File by lazy {
        File(context.getExternalFilesDir(null), "Alarm_Receiver_logs.txt")
    }


    override fun onReceive(context: Context, intent: Intent) {
        logD("in the alarm receiver func and here is the intent --> $intent")
        this.context = context
        coroutineScopeThatDoesNotCancel.launch {
            launchAlarmActivity(intent)
        }
        coroutineScopeThatDoesNotCancel.launch {
            scheduleFutureAlarm(context, alarmManager, intent)
        }
        return
    }


    private suspend fun scheduleFutureAlarm(activityContext: Context, alarmManager: AlarmManager, oldIntent: Intent) {
        val currentTimeAlarmFired = oldIntent.getLongExtra("startTime", 0)
        val startTimeForAlarmSeries = oldIntent.getLongExtra("startTimeForDb", 0)
        val originalDbEndTime = oldIntent.getLongExtra("endTime", 0)

        if ((currentTimeAlarmFired <= 0L) || (startTimeForAlarmSeries <= 0L) || (originalDbEndTime <= 0L)) { // Added check for originalDbStartTime/EndTime too
            logSoundPlay(alarmData = null, alarmSeriesStartTime= startTimeForAlarmSeries, alarmStartTime = currentTimeAlarmFired, alarmEndTime = originalDbEndTime, alarmScheduleMessage = "---- Invalid time values received in AlarmReceiver. currentAlarmTime: $currentTimeAlarmFired, originalDbStartTime: $startTimeForAlarmSeries, originalDbEndTime: $originalDbEndTime. Crashing. ----- ")
            exitProcess(69)
        }
        val alarmDao = getAlarmDao(context)
        // Lookup using the original start and end times
        val alarmData = alarmDao.getAlarmByValues(startTimeForAlarmSeries, originalDbEndTime)
        if (alarmData == null) {
            logSoundPlay(alarmData = alarmData, alarmSeriesStartTime= startTimeForAlarmSeries, alarmStartTime = currentTimeAlarmFired, alarmEndTime = originalDbEndTime, alarmScheduleMessage = "we were not able to find the alarmData in the DB")
            exitProcess(69)
            return
        }
        val execption = alarmsController.scheduleNextAlarm(
            alarmManager,
            alarmData = alarmData,
            activityContext = activityContext,
            currentAlarmTime = currentTimeAlarmFired, // Pass the time the current alarm fired
            startTimeForAlarmSeries = startTimeForAlarmSeries // Pass the original DB start time
        )

        if (execption !== null) {
            logSoundPlay(alarmData = alarmData, alarmSeriesStartTime= startTimeForAlarmSeries, alarmStartTime = currentTimeAlarmFired, alarmEndTime = originalDbEndTime, alarmScheduleMessage = "the alarm Exception is not null and it is ${execption.message}-----and it is ${execption} ")
        }else{
            logSoundPlay(alarmData = alarmData, alarmSeriesStartTime= startTimeForAlarmSeries, alarmStartTime = currentTimeAlarmFired, alarmEndTime = originalDbEndTime, alarmScheduleMessage = "there was no exception in scheduling the future alarm ")
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
    private fun logSoundPlay(alarmData: AlarmData?,alarmSeriesStartTime: Long,alarmStartTime: Long, alarmEndTime: Long, alarmScheduleMessage: String ) {
        try {
            val now = getTimeInHumanReadableFormat(Date().time)
            val logEntry = " \n\n\n --------(at $now)--------- \n  alarm series start time:${getTimeInHumanReadableFormat(alarmSeriesStartTime)}  \n alarm start time(time for the alarm to be received):${getTimeInHumanReadableFormat(alarmStartTime)}  \n alarm end time:${getTimeInHumanReadableFormat(alarmEndTime)} \n alarmData ->\n $alarmData \n<--- \n alarmScheduleMessage: $alarmScheduleMessage  \n ------- \n\n\n"
            FileWriter(logFile, true).use { writer -> writer.append(logEntry) }
            logD("Logged from AlarmReceiver: $logEntry")
        } catch (e: Exception) {
            logD("Failed to log from AlarmReceiver: ${e.message}")
        }
    }
    private  fun getTimeInHumanReadableFormat(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }
}