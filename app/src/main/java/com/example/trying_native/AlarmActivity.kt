package com.example.trying_native

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private val AUTO_FINISH_DELAY = 120000L
//    private var previousAudioVolume = 1
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasBackgroundPlaying = false
    private val mediaSessionManager by lazy {getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
    private var mediaControllerList: List<MediaController>? = null
    private val activityScope =
            CoroutineScope(
                    SupervisorJob() +
                            Dispatchers.Main +
                            CoroutineExceptionHandler { _, throwable ->
                                logD("Coroutine exception: ${throwable.message}")
                            }
            )
    private lateinit var intentReceived: Intent

    // Add AudioFocus callback
    private val audioFocusChangeListener =
            AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // We don't want to handle audio focus loss since we're an alarm
                        // The alarm should continue playing
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // We already have focus, no need to handle this
                    }
                }
            }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        logD("about to create a new alarm")
        this.intentReceived = intent

        super.onCreate(savedInstanceState)
        setContent {
            // state vals
            var messageVarToSet by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                messageVarToSet = this@AlarmActivity.parseTheIntentAndGetMessage()
            }
           LaunchedEffect(Unit) {
                this@AlarmActivity.playRandomSound()
            }


            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TimeDisplay(
                            onFinish = {
                                mediaPlayer
                                        ?.release() // I can remove it as it is unnecessary and is
                                // there in the onDestroy()
                                mediaPlayer = null
                                finishAndRemoveTask()
                            },
                            message = messageVarToSet,
                            isMessagePresent = true,
                    )
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Default) {
            launch { pauseBackgroundAudio() }
            launch { keepScreenON() }
        }

    }
    private  fun playRandomSound(){
        audioFocusRequest = audioFocusRequestBuilder()
        val rawFields =R.raw::class.java.fields
        val rawResources = rawFields.map { field -> Pair(field.name, field.getInt(null)) }
        val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
        // call it no matter what, but would prefer to pause the resource
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            activityScope.launch { playAlarmWithRandomSound(rawResources) }
        } else {
            activityScope.launch { playAlarmWithRandomSound(rawResources) }
        }

    }

    private fun logSoundPlay(soundName: String, intent: Intent) {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val alarmSeriesStartTime = intent.getLongExtra( "startTimeForDb", 0)
            val alarmStartTime = intent.getLongExtra( "startTime", 0)
            val alarmEndTime = intent.getLongExtra( "endTime", 0)

            val logEntry = " \n ------- \n alarm series start time:${getTimeInHumanReadableFormat(alarmSeriesStartTime)}  \n alarm expected fire time:${getTimeInHumanReadableFormat(alarmStartTime)}  \n alarm end time:${getTimeInHumanReadableFormat(alarmEndTime)} \n soundName:$soundName played at $now \n ------- \n\n\n"

            // Get the app's external files directory
            val file = File(getExternalFilesDir(null), "sound_log.txt")

            // Append the log entry to the file
            FileWriter(file, true).use { writer -> writer.append(logEntry) }

            logD(logEntry)
        } catch (e: Exception) {
            logD("Failed to log sound play: ${e.message}")
        }
    }

    /** this  function will get the message form the intent and will set it on the mutable State  that is  passed in */
    private fun parseTheIntentAndGetMessage():String{
        val messageTemp = intent.getStringExtra("message")
        logD("the message present in the intent is $messageTemp")
        return if (messageTemp.isNullOrEmpty()) "" else messageTemp
    }

    private fun pauseBackgroundAudio() {
        //        if (mediaPlayer?.isPlaying() == true){
        //            mediaPlayer.pause()
        //            wasBackgroundPlaying = true
        //        }
        try {
            // Initialize MediaSessionManager
            // Get a list of active media controllers
            mediaControllerList = mediaSessionManager.getActiveSessions(null)

            // Pause all active media controllers
            mediaControllerList?.forEach { controller -> controller.transportControls.pause() }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Handle the exception if required permissions are not granted
        }
    }

    private fun resumeBackgroundAudio() {
        try {
            // Resume all active media controllers
            mediaControllerList?.forEach { controller -> controller.transportControls.play() }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Handle the exception if required permissions are not granted
        }
    }

    private fun audioFocusRequestBuilder(): AudioFocusRequest {
        // Create AudioFocusRequest
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                        AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        // Request audio focus before playing alarm
    }

    private fun playAlarmWithRandomSound(rawResources: List<Pair<String, Int>>) {
        val randomSound = rawResources.random()
        var randomSoundName = randomSound.first
        if (randomSoundName == null) {
            randomSoundName = " <--string is null--> "
        }
        val randomSoundResId = randomSound.second
        try {
            mediaPlayer =
                    MediaPlayer().apply {
                        setAudioAttributes(
                                AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .setUsage(AudioAttributes.USAGE_ALARM)
                                        .build()
                        )
                        setDataSource(resources.openRawResourceFd(randomSoundResId))
                        prepare()
                        isLooping = true // Make the alarm loop until dismissed
                        start()
                        Timer().schedule(timerTask { finish() }, AUTO_FINISH_DELAY)
                    }
            logD("Playing alarm sound: $randomSoundResId")
            logSoundPlay(randomSoundName, this.intentReceived)
        } catch (e: Exception) {
            try {
                // Fallback sound with alarm stream
                mediaPlayer =
                        MediaPlayer().apply {
                            setAudioAttributes(
                                    AudioAttributes.Builder()
                                            .setContentType(
                                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                                            )
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .build()
                            )
                            setDataSource(resources.openRawResourceFd(R.raw.renaissancemp))
                            prepare()
                            isLooping = true
                            start()
                        }
                logSoundPlay("renaissance", this.intentReceived)
            } catch (e: Exception) {
                logD("Exception occurred in starting the fallback alarm \n--> $e <-- \n ")
                finish()
            }
            logD("Exception occurred in starting the alarm sound \n-->  $e  <-- \n")
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logD("New Intent received in AlarmActivity")
        // Finish the previous activity when a new intent is received
        // finish()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            logD("Error releasing WakeLock: ${e.message}")
        }

        onDestroy()
        finish()
        startActivity(intent) // Optionally, restart the activity with the new intent
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            logD("Error releasing WakeLock in onDestroy: ${e.message}")
        }
        resumeBackgroundAudio()
        // Release MediaPlayer resources when the activity is destroyed
        audioFocusRequest?.let { request -> audioManager?.abandonAudioFocusRequest(request) }
//        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousAudioVolume, 0)
        try {
            if (wasBackgroundPlaying) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            logD("Failed to resume background audio: ${e.message}")
        }
        mediaPlayer?.release()
        mediaPlayer = null
        finishAndRemoveTask()
        activityScope.cancel()
    }

    private fun keepScreenON() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
                powerManager.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                "AlarmActivity::WakeLock"
                        )
                        .apply { setReferenceCounted(false) }
        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        wakeLock?.acquire(4 * 60 * 1000L /*10 minutes*/)
    }
    private  fun getTimeInHumanReadableFormat(t:Long): String{
        if (t == 0L) return "--the time here(probablyFromTheIntent) is 0--"
        return SimpleDateFormat("yyyy-MM-dd h:mm:ss a", Locale.getDefault()).format(Date(t))
    }
}

@Composable
fun TimeDisplay(onFinish: () -> Unit, message: String, isMessagePresent: Boolean) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    // Updates the time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(999)
        }
    }
    // Display time in red on black background with a button to finish the activity
    Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = currentTime,
                    color = Color.Red,
                    fontSize = 63.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(44.dp)) // Space between the time and the button

            // Button to finish the activity
            Button(
                    onClick = { onFinish() },
                    modifier = Modifier.height(56.dp).padding(horizontal = 16.dp)
            ) { Text(text = "Cancel alarm") }

            Spacer(modifier = Modifier.height(34.dp)) // Space between the time and the button

            if (isMessagePresent) {
                Text(
                        text = message,
                        color = Color.Cyan,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, // Center align the text
                        lineHeight = 60.sp,
                        modifier =
                                Modifier.fillMaxWidth() // Take full width
                                        .padding(horizontal = 8.dp), // Add padding
                        softWrap = true // Enable text wrapping
                )
            }
        }
    }
}

// Helper function to get current time
fun getCurrentTime(): String {
    return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
}
