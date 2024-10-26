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
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
        logD( "in the alarm activity---")

        try {
            val rawFields: Array<Field> = R.raw::class.java.fields
            val rawResources = rawFields.map { field ->
                field.getInt(null)  // Get resource ID
                logD("${field}")
            }
            rawFields.forEachIndexed { index, field ->
                try {
                    val resourceId = field.getInt(null)
                    val resourceName = field.name
                    logD("Resource $index: Name=$resourceName, ID=$resourceId")
                } catch (e: Exception) {
                    logD("Error accessing resource $index: ${e.message}")
                }
            }

            // Check if we have any sound resources
            if (rawResources.isEmpty()) {
                logD("rawResources.isEmpty")
                // Fallback to a default sound if no custom sounds are available
                mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                mediaPlayer?.start()
            } else {
                logD("rawResources.is not Empty")
                // Select a random resource from the list
                val randomSoundResId = rawResources.random()
                mediaPlayer = MediaPlayer.create(this, randomSoundResId)
                mediaPlayer = MediaPlayer.create(this, randomSoundResId)
                mediaPlayer?.start()
            }


//            // Configure MediaPlayer
//            mediaPlayer?.apply {
//                isLooping = true
//                start()
//            }
        } catch (e: Exception) {
            logD( "Error initializing sound: ${e.message}")
            // Fallback to system default alarm sound
            mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            mediaPlayer?.start()
        }

        setContent {
            Trying_nativeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TimeDisplay {
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
        Log.d("AA", "New Intent received in AlarmActivity")

        // Stop and release the current MediaPlayer
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Finish the previous activity when a new intent is received
        finish()
        intent.let { startActivity(it) } // Restart the activity with the new intent if available
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources when the activity is destroyed
        mediaPlayer?.stop()
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