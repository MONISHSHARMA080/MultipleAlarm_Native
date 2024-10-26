package com.example.trying_native

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.ui.theme.Trying_nativeTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import java.lang.reflect.Field
import kotlin.random.Random


class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // List of sound resources dynamically loaded
    private val soundResourceIds by lazy {
        loadSoundResources()
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up power settings
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmActivity::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes

        // Configure window flags for alarm display
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Play a random sound
        playRandomSound()

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TimeDisplay {
                        stopAndReleaseMediaPlayer()
                        finish() // End activity when button clicked
                    }
                }
            }
        }
    }

    // Handles new intents to restart the alarm
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("AA", "New Intent received in AlarmActivity")

        stopAndReleaseMediaPlayer()
        playRandomSound()
    }

    // Releases resources on activity destruction
    override fun onDestroy() {
        super.onDestroy()
        stopAndReleaseMediaPlayer()
        wakeLock?.release()
    }

    // Load all sound resources from the raw folder
    private fun loadSoundResources(): List<Int> {
        val fields = R.raw::class.java.fields
        return fields.mapNotNull { field ->
            resources.getIdentifier(field.name, "raw", packageName).takeIf { it != 0 }
        }
    }

    // Plays a random sound from the loaded resources
    private fun playRandomSound() {
        if (soundResourceIds.isNotEmpty()) {
            val randomSoundId = soundResourceIds[Random.nextInt(soundResourceIds.size)]
            mediaPlayer = MediaPlayer.create(this, randomSoundId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    // Stops and releases the media player
    private fun stopAndReleaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun TimeDisplay(onFinish: () -> Unit) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    // Updates the time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(900)
        }
    }

    // Display time in red on black background with a button to finish the activity
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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

            Spacer(modifier = Modifier.height(28.dp)) // Space between the time and the button

            // Button to finish the activity
            Button(onClick = { onFinish() }) {
                Text(text = "Cancel alarm")
            }
        }
    }
}

// Helper function to get current time
@SuppressLint("SimpleDateFormat")
fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss")
    return dateFormat.format(Date())
}