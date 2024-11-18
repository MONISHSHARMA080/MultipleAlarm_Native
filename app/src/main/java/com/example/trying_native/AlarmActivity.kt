package com.example.trying_native

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import java.io.File
import java.io.FileWriter
import java.lang.reflect.Field
class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        wakeLock?.acquire(4*60*1000L /*10 minutes*/)
        logD("in the alarm activity---")

        val rawFields: Array<Field> = R.raw::class.java.fields
        val rawResources = rawFields.map { field ->
            Pair(field.name, field.getInt(null))  // Store both name and ID
        }

        // Select a random resource from the list
        val randomSound = rawResources.random()
        var randomSoundName = randomSound.first
        if (randomSoundName == null){randomSoundName=" <--string is null--> "}
        val randomSoundResId = randomSound.second

        // Initialize MediaPlayer with the randomly selected sound
        try {
            mediaPlayer = MediaPlayer.create(this, randomSoundResId)
            mediaPlayer?.start()
            logD("$randomSoundResId")

            // Log the sound playing details to a file
            logSoundPlay(randomSoundName)

        } catch (e: Exception) {
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.renaissancemp)
                mediaPlayer?.start()

                // Log the fallback sound
                logSoundPlay("renaissancemp")

            } catch (e: Exception) {
                logD("Exception occurred in starting the fallback alarm \n--> $e <-- \n ")
                finish()
            }
            logD("Exception occurred in starting the alarm sound \n-->  $e  <-- \n")
        }

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TimeDisplay {
                        finish() // End the activity when the button is clicked
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
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
            FileWriter(file, true).use { writer ->
                writer.append(logEntry)
            }

            logD("Logged sound play: $logEntry")
        } catch (e: Exception) {
            logD("Failed to log sound play: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent != null) {
            super.onNewIntent(intent)
        }
        Log.d("AA", "New Intent received in AlarmActivity")

        // Finish the previous activity when a new intent is received
        finish()
        startActivity(intent) // Optionally, restart the activity with the new intent
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources when the activity is destroyed
        mediaPlayer?.release()
        mediaPlayer = null
        wakeLock?.release()
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
