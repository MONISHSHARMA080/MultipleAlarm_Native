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
import kotlinx.coroutines.delay
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
    // if we receiver more intents then we will use this; if the intent is from same alarm(see id) then we will replace it /not put it in / dismiss it as
    // it is same and no need to display same message again; if it is diff then we will put it in and when dismissed then we might need to display it
    val intentHashMap: LinkedHashMap<Int,Intent> = LinkedHashMap(50)
    private var ringtone: Ringtone? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    var coroutineScope = CoroutineScope(Dispatchers.IO)


    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // play the audio here
        super.onStartCommand(intent, flags, startId)
        logD("in the AlarmService --: and the intent is $intent")
        if (intent == null) {
            // The service was killed and restarted by the system without the original intent.
            // Since we don't have the alarm data, we should just stop.
            stopSelf()
            return START_NOT_STICKY
        }
        logD("the alarm action in the service is ${intent.action} and he hashMap size is ${intentHashMap.size}")

        when (intent.action) {
            ACTION_START_ALARM -> {
                val returnCode = handleStartAlarm(intent)
                return returnCode

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
            return problemSoStopTheService()
        }
        val notification: Notification = res.first
        val alarmIntentData: AlarmActivityIntentData = res.second
         ServiceCompat.startForeground(this, intent.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        intentHashMap.putIfAbsent(alarmIntentData.alarmIdInDb, intent)
        playRandomSystemAlarm()

        return START_REDELIVER_INTENT

    }

    private  fun handleStartAlarm(intent:Intent):Int{
        when(intentHashMap.isEmpty()){
            true ->{
                return  startPlayingAlarm(intent)
            }
            false -> {
                // check if the alarm is in the hashMap and if not the do the normal start and if it is then do nothing and return
                val intentData = intent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
                if (intentData == null) {
                    // reason being that we have an assumption about the world where we will receive intents from our
                    // app only and we will include intentData field if not then we have fundamentally f'ed up
                    logD("[ERROR FATAl] intent data is not present in the intent , $intent ")
                    return problemSoStopTheService()
                }
                val intentInHashMap = intentHashMap[intentData.alarmIdInDb]
                if (intentInHashMap == null){
                    intentHashMap.putIfAbsent(intentData.alarmIdInDb, intent)
                }
                // either way startSticky as we have other intents we want to execute
                return  START_REDELIVER_INTENT
            }
        }
    }

    private fun handleDismissAlarm(intent:Intent):Int{
        // remove this intent from the hashmap, and then if we have other in the hashMap then start playing those
        // assert that this intent is in the hashMap if not then we have a problem
        // 1. Remove the dismissed alarm from the queue
        val intentData = intent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
        if (intentData == null) {
            logD("[ERROR FATAl] intent data is not present in the intent , $intent ")
            return problemSoStopTheService()
        }
        val isIntentInHashMap = intentHashMap.remove(intentData.alarmIdInDb) != null
        when(intentHashMap.isEmpty()){
            true ->{
                // nothing in the hashMap so we can stop this activity
                stopRingtoneAndRemoveAudioFocus()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_REDELIVER_INTENT
            }
            false ->{
                if (!isIntentInHashMap){
                    logD("[ERROR FATAl] expected the intent to delete the alarm to be in the hashMap, $intent but didn't found it there ")
                    return problemSoStopTheService()
                }
//             return   startPlayingAlarm(intent)
             return   startPlayingAlarm(intentHashMap.entries.first().value)
            }
        }
    }

    private  fun problemSoStopTheService():Int{
        stopSelf()
        return  START_NOT_STICKY
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
            val intentData = originalIntent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
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
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtras(originalIntent) // Carry over your alarm data
            }
            val dismissPendingIntent = PendingIntent.getService(
                this, 0, dismissIntent,
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

    private fun playRandomSystemAlarm(){
        runCatching {
            logD("in playRandomSystemAlarm it is ${audioFocusRequest == null} audioFocusRequest null  and it is $audioFocusRequest")
            val audioFocus = audioFocusRequestBuilder()
            audioFocusRequest = audioFocus
            val audioFocusReq =audioManager.requestAudioFocus(audioFocus)
            logD(if (audioFocusReq == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "Audio focus Req is granted $audioFocusReq" else "Audio Focus req was not granted $audioFocusReq")
            if (audioFocusReq == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                logD("Android audio focus req was granted and it is $audioFocusReq")
                startRandomRingtonePlaying()
            }else {
                logD("Audio Focus req was not granted and we are not playing, we got $audioFocusReq did it failed ->${audioFocusReq == AudioManager.AUDIOFOCUS_REQUEST_FAILED} !!")
                startRandomRingtonePlaying()

            }
        }.fold(onSuccess = {}, onFailure = {exception ->
            logD("there is a exception while launching random system alarm and it is ${exception.message}\n-->$exception")
        })
    }
    private fun startRandomRingtonePlaying(){
        val ringtoneManager = RingtoneManager(this)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
        val ringtoneCursor =ringtoneManager.cursor
        val len =ringtoneCursor.count
        val randomIndex =Random.nextInt(len )
        val ringtone =ringtoneManager.getRingtone(randomIndex)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        this.ringtone = ringtone
        ringtone.isLooping = true
        ringtone.play()
    }

    private fun audioFocusRequestBuilder(): AudioFocusRequest {
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // Add this
                .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { change ->audioFocusChangeListener(change) }
            .build()
    }

    private fun audioFocusChangeListener(focusChange: Int) {
        logD("Audio focus change: $focusChange")
        when(focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                logD("Lost audio focus permanently")
                stopRingtoneAndRemoveAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                logD("Lost audio focus temporarily")
                ringtone?.stop()  // Use pause() instead of stop()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                logD("Gained audio focus")
                // If we don't have a ringtone yet, this is a delayed grant
                if (ringtone == null) {
                    startRandomRingtonePlaying()
                } else if (ringtone?.isPlaying == false) {
                    ringtone?.play()
                }
            }
        }
    }


}