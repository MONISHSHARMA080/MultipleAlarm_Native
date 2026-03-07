package com.example.trying_native.BroadCastReceivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.trying_native.Activities.AlarmActivityIntentData
import com.example.trying_native.AlarmLogic.AlarmsController
import com.example.trying_native.AlarmReceiver
import com.example.trying_native.ErrorHandling.ErrorHandler
import com.example.trying_native.analytics.Analytics
import com.example.trying_native.dataBase.AlarmDao
import com.example.trying_native.dataBase.AlarmData
import com.example.trying_native.dataBase.AlarmDatabase
import com.example.trying_native.notification.NotificationHandler
import com.example.trying_native.utils.Result.Error
import com.example.trying_native.utils.Result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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