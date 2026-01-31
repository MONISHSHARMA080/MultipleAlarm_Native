package com.example.trying_native.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.trying_native.Activities.AlarmActivity
import com.example.trying_native.Activities.AlarmActivityIntentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.getOrElse
import kotlin.math.log
import kotlin.random.Random

class AlarmService: Service() {
    companion object {
        const val ACTION_START_ALARM = "ACTION_START_ALARM"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"
        const val CHANNEL_ID = "alarm_channel"
    }
    //---------------------------------------
    // issue : if multiple alarms then they wont turn off the prev one
    //
    //      we can check weather the queueForNextIntent is null or not ; if null then we are first and if not then we need to put one in and during the dismiss time try to
    //      take one out and start it
    //---------------------------------------

    // if we receiver more intents then we will use this; if the intent is from same alarm(see id) then we will replace it /not put it in / dismiss it as
    // it is same and no need to display same message again; if it is diff then we will put it in and when dismissed then we might need to display it
    val intentHashMap: LinkedHashMap<Int,Intent> = LinkedHashMap(50)
    private var ringtone: Ringtone? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    val coroutineScope = CoroutineScope(Dispatchers.IO)


    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // play the audio here
        super.onStartCommand(intent, flags, startId)
        println("in the AlarmService --: and the intent is $intent")
        if (intent == null) {
            // The service was killed and restarted by the system without the original intent.
            // Since we don't have the alarm data, we should just stop.
            stopSelf()
            return START_NOT_STICKY
        }
        logD("the alarm action in the service is ${intent.action}")
        when (intent.action) {
            ACTION_START_ALARM -> {
                return startAlarm(intent)
            }
            ACTION_DISMISS_ALARM -> {
                stopRingtoneAndRemoveAudioFocus()
                stopSelf()
                return START_REDELIVER_INTENT
            }
            else -> {
                logD("\n\n [ERROR] Unknown action: ${intent.action}")
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private  fun startAlarm(intent:Intent):Int{
        when(intentHashMap.isEmpty()){
            true ->{
                // we are the first so start the alarm activity
                coroutineScope.launch {
                    // if there is an alarm then stop it and start playing the next
                    stopRingtoneAndRemoveAudioFocus()
                    playRandomSystemAlarm()
                }
                val res = buildNotification(this, intent).getOrElse { exception ->
                    // log the error and then stop the service
                    // ----------------------
                    //      log the error
                    // ----------------------
                    logD(" Error building notification: ${exception.message} ")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification: Notification = res.first
                val alarmIntentData: AlarmActivityIntentData = res.second
                ServiceCompat.startForeground(this, intent.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                intentHashMap.putIfAbsent(alarmIntentData.alarmIdInDb, intent)
                return START_REDELIVER_INTENT
            }
            false -> {
                // check if the alarm is in the hashMap and if not the do the normal start and if it is then do nothing and return
                val intentData = intent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
                if (intentData == null) {
                    // reason being that we have an assumption about the world where we will receive intents from our
                    // app only and we will include intentData field if not then we have fundamentally f'ed up
                    logD("[ERROR FATAl] intent data is not present in the intent , $intent ")
                    stopSelf()
                    return  START_NOT_STICKY
                }
                val intentInHashMap =intentHashMap.get(intentData.alarmIdInDb)
                if (intentInHashMap == null){
                    intentHashMap.putIfAbsent(intentData.alarmIdInDb, intent)
                }
                // either way startSticky as we have other intents we want to execute
                return  START_REDELIVER_INTENT
            }
        }
    }

    override fun onDestroy() {
        runCatching {
            this@AlarmService.stopRingtoneAndRemoveAudioFocus()
            super.onDestroy()
        }
    }

    private fun logD(message:String){
        Log.d("AAAAA", "[AlarmService]$message")
    }

    private  fun stopRingtoneAndRemoveAudioFocus(): Result<Unit>{
        return runCatching {
            this.ringtone?.stop()
            audioFocusRequest?.let { request -> audioManager.abandonAudioFocusRequest(request) }
        }
    }

    private fun buildNotification(context:Context, originalIntent: Intent):Result<Pair<Notification, AlarmActivityIntentData>>{
        return runCatching {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "alarm_channel_id"

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

            logD("\n\n\n ------- +++ in the alarm receiver func and about to launch Full screen activity intent here is the intent --> $originalIntent")
            // 2. Create the Intent for your AlarmActivity
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtras(originalIntent) // Carry over your alarm data
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            val dismissPendingIntent = PendingIntent.getService(
                this, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val intentData = originalIntent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
                    ?: return Result.failure(Exception("Expected to intent data to be in the intent but got it as null"))

            // 3. Build the Notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Use your actual icon
                .setContentTitle("Alarm")
                .setContentText(if(intentData.message != "") intentData.message else "Alarm is ringing")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true) // THE KEY STEP
                .setOngoing(true) // User can't swipe it away
                .addAction(android.R.drawable.ic_delete, "Dismiss", dismissPendingIntent)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            return@runCatching Pair(builder.build(), intentData)
        }
    }

    private fun playRandomSystemAlarm(){
        runCatching {
            audioFocusRequest = audioFocusRequestBuilder()
            audioManager.requestAudioFocus(audioFocusRequest!!)
            val ringtoneManager = RingtoneManager(this)
            ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
            val ringtoneCursor =ringtoneManager.cursor
            val len =ringtoneCursor.count
            logD("the len is $len")
            val randomIndex =Random.nextInt(len )
            logD("the random index is $randomIndex")
            val ringtone =ringtoneManager.getRingtone(randomIndex)
            logD("the ringtone randomly chosen  is $ringtone")
            this.ringtone = ringtone
            ringtone.isLooping = true
            ringtone.play()
        }.fold(onSuccess = {}, onFailure = {exception ->
            logD("there is a exception while launching random system alarm and it is ${exception.message}\n-->$exception")
        })
    }

    private fun audioFocusRequestBuilder(): AudioFocusRequest {
        // Create AudioFocusRequest
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            ).build()
    }
}