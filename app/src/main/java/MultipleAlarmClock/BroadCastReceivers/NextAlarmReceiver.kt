package com.coolApps.MultipleAlarmClock.BroadCastReceivers

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import MultipleAlarmClock.alarmFeature.domain.getFreqInMillisecond
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.example.MultipleAlarmClock.BroadCastReceivers.AlarmReceiver
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@AndroidEntryPoint
class NextAlarmReceiver: BroadcastReceiver() {
    private lateinit var context: Context
    private val coroutineScope = CoroutineScope( Dispatchers.Default)
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    @Inject lateinit var alarmsController: AlarmsController
    val analytics by lazy {Analytics(context)}
    private  val receiverClass: Class<out BroadcastReceiver> = AlarmReceiver::class.java
	@Inject lateinit var alarmRepository: AlarmRepository

    val doWeWantToGoAsync =true

     override  fun onReceive(context: Context, intent: Intent) {
        logD("in the NextAlarmReceiver class and here is the intent --> $intent")
         val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
         this.context = context
         when (this.doWeWantToGoAsync) {
             true -> {
                 val pendingResult = goAsync() // Extends execution time
                 scope.launch {
                     try {
                         scheduleFutureAlarm(context, alarmManager, intent)
                     } catch (e: Exception) {
                         analytics.captureEvent("Error occurred", mapOf(
                             "exception occurred" to e.toString(),
                             "stack trace" to e.stackTraceToString(),
                             "cause" to (e.cause?.toString() ?: "No cause" ) ,
                             "exception" to e,
                             "class" to "NextAlarmReceiver"

                         ))

                     } finally {
                         // ALWAYS finish the pendingResult inside the coroutine
                         // so the process stays alive until the work is done.
                         pendingResult.finish()
                     }

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
        val parsedIntentData = IntentCompat.getParcelableExtra(oldIntent,"intentData", AlarmActivityIntentData::class.java) ?: return
        val currentTimeAlarmFired = parsedIntentData.startTime
        val alarmData: AlarmData? = alarmRepository.getAlarmById(parsedIntentData.alarmIdInDb)

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
               receiverClass = receiverClass,startTimeForAlarmSeries = alarmData.startTime,
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
                alarmRepository.updateAlarm(alarmData.copy(isReadyToUse = false))
            })
        }
    }
    private  fun logD(msg:String):Unit{
        Log.d("AAAAA", "[NextAlarmReceiver] $msg")
    }

}