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
import java.lang.reflect.Field
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

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private val AUTO_FINISH_DELAY = 120000L
    private var previousAudioVolume = 1
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasBackgroundPlaying = false
    private lateinit var mediaSessionManager: MediaSessionManager
    private var mediaControllerList: List<MediaController>? = null
    private val activityScope =
            CoroutineScope(
                    SupervisorJob() +
                            Dispatchers.Main +
                            CoroutineExceptionHandler { _, throwable ->
                                logD("Coroutine exception: ${throwable.message}")
                            }
            )

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
        super.onCreate(savedInstanceState)
        audioFocusRequest = audioFocusRequestBuilder()
        //        pauseBackgroundAudio()
        //        keepScreenON()
        lifecycleScope.launch(Dispatchers.IO) {
            launch { pauseBackgroundAudio() }
            launch { keepScreenON() }
        }
        //        activityScope.launch { pauseBackgroundAudio() }
        //        activityScope.launch {keepScreenON()   }

        val rawFields: Array<Field> = R.raw::class.java.fields
        val rawResources = rawFields.map { field -> Pair(field.name, field.getInt(null)) }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Get the maximum volume for alarm stream
        previousAudioVolume =
                audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: previousAudioVolume
        // val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
        // Set volume to maximum for alarm
        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousAudioVolume, 0)
        // if the BG audio is active then pause it
        val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
        // call it no matter what, but would prefer to pause the resource
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            activityScope.launch { playAlarmWithRandomSound(rawResources) }
        } else {
            activityScope.launch { playAlarmWithRandomSound(rawResources) }
        }

        var isMessagePresent = intent.getBooleanExtra("isMessagePresent", false)
        var message = ""
        if (isMessagePresent) {
            val messagetemp = intent.getStringExtra("message")
            logD("the meessage form the intent in the AlarmActivity is  ->${messagetemp}<-")
            if (messagetemp == null) {
                isMessagePresent = false
            } else {
                message = messagetemp
            }
        }
        logD("is the message is present is -->${isMessagePresent}")

        setContent {
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
                            message = message,
                            isMessagePresent = isMessagePresent
                    )
                }
            }
        }
    }

    private fun logSoundPlay(soundName: String) {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "$soundName played at $now\n"

            // Get the app's external files directory
            val file = File(getExternalFilesDir(null), "sound_log.txt")

            // Append the log entry to the file
            FileWriter(file, true).use { writer -> writer.append(logEntry) }

            logD("Logged sound play: $logEntry")
        } catch (e: Exception) {
            logD("Failed to log sound play: ${e.message}")
        }
    }

    private fun pauseBackgroundAudio() {
        //        if (mediaPlayer?.isPlaying() == true){
        //            mediaPlayer.pause()
        //            wasBackgroundPlaying = true
        //        }
        try {
            // Initialize MediaSessionManager
            mediaSessionManager =
                    getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
            logSoundPlay(randomSoundName)
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
                logSoundPlay("renaissance")
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
        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousAudioVolume, 0)
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
