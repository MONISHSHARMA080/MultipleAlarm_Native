package com.example.trying_native

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasBackgroundPlaying = false
//    private val mediaSessionManager by lazy {getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager }
    private var mediaControllerList: List<MediaController>? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var intentReceived: Intent
    private var ringtone: Ringtone? = null
    private  val AUTO_FINISH_DELAY = 120000L // 2 sec is 120000

//
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        logD("about to create a new alarm")
        this.intentReceived = intent
        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
                // state vals
                var messageVarToSet by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    messageVarToSet = this@AlarmActivity.parseTheIntentAndGetMessage()
                }
                LaunchedEffect(Unit) {
//                    this@AlarmActivity.playRandomSound()
                    this@AlarmActivity.playRandomSystemAlarm()
                }


                    Scaffold(modifier = Modifier.fillMaxSize()) {
                        TimeDisplay(
                            onFinish = {
                                mediaPlayer
                                    ?.release() // I can remove it as it is unnecessary and is
                                mediaPlayer = null
                                finishAndRemoveTask()
                            },
                            message = messageVarToSet,
                            isMessagePresent = true,
                        )
                }

            }

        }
//        lifecycleScope.launch(Dispatchers.Default) { launch { pauseBackgroundAudio() } }
        lifecycleScope.launch { delay(AUTO_FINISH_DELAY); this@AlarmActivity.finish() }
        lifecycleScope.launch(Dispatchers.IO) {keepScreenON()  }

    }


    private fun playRandomSystemAlarm(){
        runCatching {
            audioFocusRequest = audioFocusRequestBuilder()
            audioManager.requestAudioFocus(audioFocusRequest!!)
            val ringtoneManager =RingtoneManager(this)
            ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
            val ringtoneCursor =ringtoneManager.cursor
            val len =ringtoneCursor.count
            logD("the len is $len")
            val randomIndex =Random.nextInt(len )
            logD("the random index is $randomIndex")
            val ringtone =ringtoneManager.getRingtone(randomIndex)
            if (ringtone == null){
                logD("the ringtone is null")
            }
            logD("the ringtone randomly chosen  is $ringtone")
            this@AlarmActivity.ringtone = ringtone
            ringtone.isLooping = true
            ringtone.play()


        }.fold(onSuccess = {}, onFailure = {exception ->
            logD("there is a exception while launching random system alarm and it is ${exception.message}\n-->$exception")
        })
    }

    /** this  function will get the message form the intent and will set it on the mutable State  that is  passed in */
    private fun parseTheIntentAndGetMessage():String{
        val messageTemp = intent.getStringExtra("message")
        logD("the message present in the intent is $messageTemp")
        return if (messageTemp.isNullOrEmpty()) "" else messageTemp
    }

//    private fun pauseBackgroundAudio() {
//        try {
//            mediaControllerList = mediaSessionManager.getActiveSessions(null)
//            mediaControllerList?.forEach { controller -> controller.transportControls.pause() }
//        } catch (e: SecurityException) {
//            e.printStackTrace()
//        }
//    }

//    private fun resumeBackgroundAudio() {
//        try {
//            // Resume all active media controllers
//            mediaControllerList?.forEach { controller -> controller.transportControls.play() }
//        } catch (e: SecurityException) {
//            e.printStackTrace()
//            // Handle the exception if required permissions are not granted
//        }
//    }

    private fun audioFocusRequestBuilder(): AudioFocusRequest {
        // Create AudioFocusRequest
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                        AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build()
                )
                .build()
    }


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

    @SuppressLint("Wakelock")
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
//        resumeBackgroundAudio()
        audioFocusRequest?.let { request -> audioManager.abandonAudioFocusRequest(request) }
        try {
            if (wasBackgroundPlaying) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            logD("Failed to resume background audio: ${e.message}")
        }
        this.ringtone?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        finishAndRemoveTask()
        activityScope.cancel()
    }

    private fun keepScreenON() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

}

@Composable
fun TimeDisplay(onFinish: () -> Unit, message: String, isMessagePresent: Boolean) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    // Updates the time every second
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(600)
        }
    }
    // Display time in red on black background with a button to finish the activity
    Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(20.dp)) // Space between the time and the top
            Text(
                    text = currentTime,
                    color = Color.Red,
                    fontSize = 63.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(44.dp)) // Space between the time and the button

            if (isMessagePresent) {
                Text(
                        text =message + "\n",
                        color = Color.Cyan,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, // Center align the text
                        lineHeight = 60.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), // Add padding
                        softWrap = true // Enable text wrapping
                )
            }
        }

        Button(
            onClick = { onFinish() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xffab0c00), // Cyan/Teal color
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (screenHeight/9))
                .height(56.dp)
                .shadow(8.dp, shape = RoundedCornerShape(28.dp))
        ) {
            Text(text = "Cancel alarm")
        }
    }
}

// Helper function to get current time
fun getCurrentTime(): String {
    return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
}
