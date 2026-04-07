package com.coolApps.MultipleAlarmClock.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivity
import com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService: Service() {
    companion object {
        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"
    }
    // if we receive more intents than we will use this; if the intent is from same alarm(see id) then we will replace it /not put it in / dismiss it as
    // it is same and no need to display same message again; if it is diff then we will put it in and when dismissed then we might need to display it
    val intentHashMap: LinkedHashMap<Int,Intent> = LinkedHashMap(20)
    val analytics by lazy { Analytics(this) }
    val playAlarm by lazy { PlayAlarm(this, analytics) }
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // play the audio here
        super.onStartCommand(intent, flags, startId)
        logD("in the AlarmService --: and the intent is $intent")
        if (intent == null) {
            // The service was killed and restarted by the system without the original intent.
            // Since we don't have the alarm data, we should just stop.
            analytics.captureEvent("error occurred", mapOf(
                "error" to "intent was null in AlarmService"
            ))
            stopSelf()
            return START_NOT_STICKY
        }
        logD("the alarm action in the service is ${intent.action} and he hashMap size is ${intentHashMap.size}")

        when (intent.action) {
            ACTION_START_ALARM -> {
                return handleStartAlarm(intent)
            }
            ACTION_DISMISS_ALARM -> {
                return handleDismissAlarm(intent)
            }
            else -> {
                logD("\n\n [ERROR] Unknown action: ${intent.action}")
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }


    /** launches the notification with full screen intent, plays the alarm sound , and puts intent in the hashMap if required*/
    private fun startPlayingAlarm(intent: Intent):Int{
        val res = buildNotification(this, intent).getOrElse { exception ->
            // log the error and then stop the service
            // ----------------------
            //      log the error
            // ----------------------
            logD(" Error building notification: ${exception.message} ")
            return problemSoStopTheService(" Error building notification: ${exception.message} ")
        }
        val notification: Notification = res.first
        val alarmIntentData: AlarmActivityIntentData = res.second
        intentHashMap.putIfAbsent(alarmIntentData.alarmIdInDb, intent)
        ServiceCompat.startForeground(this, alarmIntentData.alarmIdInDb, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        playAlarm.play()
        return START_REDELIVER_INTENT
    }

    private  fun handleStartAlarm(intent:Intent):Int{
        val intentData = intent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java) ?: return problemSoStopTheService("intentData parsed is null")
        val isFirstAlarm = intentHashMap.isEmpty()
        intentHashMap.putIfAbsent(intentData.alarmIdInDb, intent)

        coroutineScope.launch {
            analytics.captureEvent("alarm notification sent", mapOf(
                "intentData" to intentData.toString(),
                "isFirstAlarm" to isFirstAlarm,
                "areWePuttingTheAlarmIntoHashMapAndLetOtherPlay" to !isFirstAlarm,
                "class" to "AlarmService"
                )
            )
        }


        if (isFirstAlarm){
            startPlayingAlarm(intent)
        }else{
            logD("one alarm is already there so not-playing/ waiting on another one ")
        }
        return  START_REDELIVER_INTENT

    }

    private fun handleDismissAlarm(intent:Intent):Int{
        // remove this intent from the hashmap, and then if we have other in the hashMap then start playing those
        // assert that this intent is in the hashMap if not then we have a problem
        // 1. Remove the dismissed alarm from the queue
        val intentData = IntentCompat.getParcelableExtra(intent,"intentData", AlarmActivityIntentData::class.java) ?: return problemSoStopTheService("intentData parsed is null")
        intentHashMap.remove(intentData.alarmIdInDb)
        coroutineScope.launch {
            analytics.captureEvent("handling dismiss of alarm", mapOf(
                "intentData" to intentData.toString(),
                "isLastAlarm" to intentHashMap.isEmpty(),
                "areWePullingOtherAlarmFromHashMapAndPlayingThose" to intentHashMap.isNotEmpty(),
                "class" to "AlarmService"
                )
            )
        }

        when(intentHashMap.isEmpty()){
            true ->{
                // nothing in the hashMap so we can stop this activity
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                playAlarm.pause()
                playAlarm.destroy()
                return START_REDELIVER_INTENT
            }
            false ->{
             return   startPlayingAlarm(intentHashMap.entries.first().value)
            }
        }
    }

    private  fun problemSoStopTheService(errorMessage: String):Int{
            analytics.captureEvent("error occurred, so we are stopping the alarm(s)", mapOf(
                "error" to errorMessage
            ))
        stopSelf()
        return  START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching {
            playAlarm.destroy()
            super.onDestroy()
        }
    }

    private fun logD(message:String){
        Log.d("AAAAA", "[AlarmService] $message")
    }


    private fun buildNotification(context:Context, originalIntent: Intent):Result<Pair<Notification, AlarmActivityIntentData>>{
        return runCatching {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "alarm_channel_id"

            val intentData = IntentCompat.getParcelableExtra( originalIntent,"intentData", AlarmActivityIntentData::class.java)
                ?: return Result.failure(Exception("Expected to intent data to be in the intent but got it as null"))

            val channel = NotificationChannel(
                channelId,
                "Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH // Necessary for heads-up

            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setSound(null,null)
               setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)

            // 2. Create the Intent for your AlarmActivity
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtras(originalIntent) // Carry over your alarm data
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(

                context,
                intentData.alarmIdInDb,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtras(originalIntent) // Carry over your alarm data
            }
            val dismissPendingIntent = PendingIntent.getService(
                this,
                intentData.alarmIdInDb, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            // 3. Build the Notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Use your actual icon
                .setContentTitle("Alarm")
                .setContentText(if(intentData.message != "") intentData.message else "Alarm is ringing")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true) // THE KEY STEP
                .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(0)
                .setSound(null)
            return@runCatching Pair(builder.build(), intentData)
        }
    }
}