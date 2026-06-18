package MultipleAlarmClock.alarmFeature.receiver

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
import com.coolApps.MultipleAlarmClock.services.AlarmService
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmsController: AlarmsController

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        logD("onReceive: intent action = ${intent.action}")
        val pendingResult = goAsync()

        coroutineScope.launch {
            try {
                // 1. Start the AlarmService to play the alarm
                startAlarmService(context, intent)

                // 2. Schedule the next alarm instance if applicable
                scheduleNextAlarm(context, intent)
            } catch (e: Exception) {
                logD("Error in onReceive: ${e.message}")
                Analytics(context).captureEvent("Error in AlarmReceiver", mapOf(
                    "exception" to e.toString(),
                    "stackTrace" to e.stackTraceToString()
                ))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startAlarmService(context: Context, intent: Intent) {
        try {
            logD("Starting AlarmService")
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtras(intent)
                action = AlarmService.ACTION_START_ALARM
            }
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            logD("Failed to start AlarmService: $e")
        }
    }

    private suspend fun scheduleNextAlarm(context: Context, intent: Intent) {
        val intentData = IntentCompat.getParcelableExtra(intent, "intentData", AlarmActivityIntentData::class.java)
            ?: return

        val currentTimeAlarmFired = intentData.startTime
        val alarmData: AlarmData? = alarmRepository.getAlarmById(intentData.alarmIdInDb)

        if (alarmData == null) {
            logD("AlarmData not found for ID: ${intentData.alarmIdInDb}")
            return
        }

        val nextAlarmTime = currentTimeAlarmFired + alarmData.getFreqInMillisecond()
        logD("Current: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(currentTimeAlarmFired)}, Next: ${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(nextAlarmTime)}")

        if (nextAlarmTime < alarmData.endTime) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val res = alarmsController.scheduleAlarm(
                startTime = nextAlarmTime,
                endTime = alarmData.endTime,
                alarmManager = alarmManager,
                componentActivity = context,
                receiverClass = AlarmReceiver::class.java,
                startTimeForAlarmSeries = alarmData.startTime,
                alarmData = alarmData,
                alarmMessage = alarmData.message
            )

            res.fold(
                onSuccess = {
                    logD("Scheduled next alarm successfully")
                },
                onError = { message, exception ->
                    logD("Error scheduling next alarm: ${message.messageToDisplayUser}")
                    val errorHandler = ErrorHandler(NotificationHandler(context), Analytics(context))
                    errorHandler.handleError(Result.Failure(message, exception), "Error scheduling next alarm")
                    alarmRepository.updateAlarm(alarmData.copy(isReadyToUse = false))
                }
            )
        } else {
            logD("No more instances to schedule for this alarm series")
        }
    }

    private fun logD(message: String) {
        Log.d("AAAAA", "[AlarmReceiver] $message")
    }
}