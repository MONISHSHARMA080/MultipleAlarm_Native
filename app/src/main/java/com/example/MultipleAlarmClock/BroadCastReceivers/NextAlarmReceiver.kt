package com.coolApps.MultipleAlarmClock.BroadCastReceivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.AlarmReceiver
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDatabase
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Error
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

sealed interface NextAlarmReceiverError: Error {
    data class GenericError(override val messageToDisplayUser: String): NextAlarmReceiverError
}

class NextAlarmReceiver: BroadcastReceiver() {
    private lateinit var context: Context
    private val coroutineScopeThatDoesNotCancel = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coroutineScope = CoroutineScope( Dispatchers.Default)
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var alarmsController = AlarmsController()
    val analytics by lazy {Analytics(context)}
    private  val receiverClass: Class<out BroadcastReceiver> = AlarmReceiver::class.java
    var alarmDao: AlarmDao? = null
    val doWeWantToGoAsync =true

     override  fun onReceive(context: Context, intent: Intent) {
        logD("in the NextAlarmReceiver class and here is the intent --> $intent")
         this.context = context
         when (this.doWeWantToGoAsync) {
             true -> {
                 val pendingResult = goAsync() // Extends execution time
                 runBlocking {
                     scheduleFutureAlarm(context, alarmManager, intent)
                     pendingResult.finish()
                 }
             }
             false ->{
                 // we are in the Test
                 runBlocking {
                     scheduleFutureAlarm(context, alarmManager, intent)
                 }

             }
         }
        }

    private suspend fun scheduleFutureAlarm(activityContext: Context, alarmManager: AlarmManager, oldIntent: Intent) {
        val parsedIntentData = oldIntent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java) ?: return
        val currentTimeAlarmFired = parsedIntentData.startTime
        val startTimeForAlarmSeries = parsedIntentData.startTimeForDb
        val originalDbEndTime = parsedIntentData.endTime

        if ((currentTimeAlarmFired <= 0L) || (startTimeForAlarmSeries <= 0L) || (originalDbEndTime <= 0L)) { // Added check for originalDbStartTime/EndTime too
//            logSoundPlay(alarmData = null, alarmSeriesStartTime= startTimeForAlarmSeries, alarmStartTime = currentTimeAlarmFired, alarmEndTime = originalDbEndTime, alarmScheduleMessage = "---- Invalid time values received in AlarmReceiver. currentAlarmTime: $currentTimeAlarmFired, originalDbStartTime: $startTimeForAlarmSeries, originalDbEndTime: $originalDbEndTime. Crashing. ----- ")
        }
        val alarmDaoImpl = this.alarmDao?:getAlarmDao(activityContext )
        val alarmData: AlarmData? = alarmDaoImpl.getAlarmByValues(startTimeForAlarmSeries, originalDbEndTime)
        if (alarmData == null) {
            analytics.captureEvent("alarmData delivered from intent not found in DB", mapOf(
                "alarmData parsed from intent" to parsedIntentData.toString(),
                "alarmDaoImpl.getAlarmByValues(startTimeForAlarmSeries, originalDbEndTime) returned" to "null"
            ))
            return
        }
        val nextAlarmTime = currentTimeAlarmFired + alarmData.getFreqInMillisecond()
        logD(" currentTimeAlarmFired is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeAlarmFired)}      the nextAlarmTime is ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTime)}  and the alarmData.getFreqInMillisecond -->  ${alarmData.getFreqInMillisecond()} \n and alarmData is --> ${alarmData.toString()} and the freq is ${alarmData.frequencyInMin} ")

        if (nextAlarmTime < alarmData.endTime){
           val res = coroutineScope.async{
               alarmsController.scheduleAlarm (
               startTime = nextAlarmTime, // This is the time the next alarm will trigger
               endTime = alarmData.endTime, // The series end time
               alarmManager = alarmManager,componentActivity = activityContext,
               receiverClass = receiverClass,startTimeForAlarmSeries = startTimeForAlarmSeries,
               alarmData = alarmData, alarmMessage = alarmData.message
               )
           }.await()

            res.fold(onSuccess = {
                analytics.captureEvent("scheduled next alarm successfully", mapOf(
                    "next alarmData" to alarmData.copy(startTime = nextAlarmTime).toString()
                ))
            }, onError = {messageToDisplayUser, exception->
                val errorHandler = ErrorHandler(NotificationHandler(context), analytics)
                errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "error returned in scheduling upcoming alarm " )
                alarmDaoImpl.updateAlarmForReset(alarmData.copy(isReadyToUse = false))
            })
        }
    }

    private  fun getAlarmDao(context: Context): AlarmDao{
        val db = Room.databaseBuilder(
            context,
            AlarmDatabase::class.java, "alarm-database"
        ).build()
        return db.alarmDao()

    }
    private  fun getTimeInHumanReadableFormat(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }
    private  fun logD(msg:String):Unit{
        Log.d("AAAAA", "[NextAlarmReceiver] $msg")
    }

}