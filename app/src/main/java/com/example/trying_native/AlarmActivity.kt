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

    private fun logAvailableSounds() {
        val fields = R.raw::class.java.fields
        logD("Total number of resources in raw folder: ${fields.size}")

        val soundFields = fields.filter { it.name.endsWith("mp") }
        logD("Found ${soundFields.size} MP3 files:")

        soundFields.forEach { field ->
            logD("Sound file available: ${field.name}")
        }
    }

    private fun getRandomSoundResourceId(): Int {
        // Get all raw resources
        val fields = R.raw::class.java.fields
        // Filter only MP3 files
        val soundFields = fields.filter { it.name.endsWith("mp") }

        if (soundFields.isEmpty()) {
            logD("WARNING: No sound files found in raw folder!")
            throw IllegalStateException("No sound files found in raw folder")
        }

        // Pick a random sound
        val randomField = soundFields.random()
        logD("Selected sound file: ${randomField.name}")
        return randomField.getInt(null)
    }

    private fun playRandomSound() {
        try {
            // Stop and release any existing MediaPlayer
            mediaPlayer?.stop()
            mediaPlayer?.release()

            val resourceId = getRandomSoundResourceId()
            // Create new MediaPlayer with random sound
            mediaPlayer = MediaPlayer.create(this, resourceId).apply {
                isLooping = true
                start()
            }
            logD("Successfully started playing sound with resource ID: $resourceId")
        } catch (e: Exception) {
            logD("Error playing sound: ${e.message}")
            e.printStackTrace()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logD("AlarmActivity onCreate started")
        // Log all available sounds at startup
        logAvailableSounds()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmActivity::WakeLock"
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        wakeLock?.acquire(10601000L) // 10 minutes

        logD("Starting to play random sound")
        // Play random sound when activity starts
        playRandomSound()

        logD("Setting up UI content")
        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TimeDisplay {
                        logD("Stop button clicked, cleaning up MediaPlayer")
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        finish() // End the activity when the button is clicked
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logD("New Intent received in AlarmActivity")

        // Stop current sound and play a new random sound
        logD("Playing new random sound due to new intent")
        playRandomSound()

        // Handle the new intent
        finish()
        intent.let { startActivity(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        logD("AlarmActivity onDestroy called")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        wakeLock?.release()
        logD("MediaPlayer and WakeLock released")
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