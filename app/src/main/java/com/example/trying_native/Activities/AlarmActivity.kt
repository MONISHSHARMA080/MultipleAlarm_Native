package com.example.trying_native.Activities

import android.R.attr.onClick
import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
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
import com.example.trying_native.logD
import com.example.trying_native.services.AlarmService
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var intentReceived: Intent
    private  val AUTO_FINISH_DELAY = 120000L // 2 sec is 120000
    private var dismissIntent : Intent? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
    logD("about to create a new alarm")
        this.intentReceived = intent
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
                var messageVarToSet by remember { mutableStateOf("") }
                var intentData by remember { mutableStateOf<AlarmActivityIntentData?>(null)  }

                LaunchedEffect(Unit) {
                    intentData = this@AlarmActivity.parseTheIntent()
                    val intentDataAccessed = intentData
                    logD("the intent data we got from parsing is $intentData")
                    if (intentDataAccessed !=  null ) {
                        messageVarToSet = intentDataAccessed.message
                    }
                    logD("the message from intent we got is $messageVarToSet ")
                }
                LaunchedEffect(Unit) {
                    if (dismissIntent == null) dismissIntent = makeDismissIntent()
                }
                TimeDisplay(
                    onFinish = { finishAndRemoveTask()},
                    message = messageVarToSet
                )
            }
        }
        lifecycleScope.launch { delay(AUTO_FINISH_DELAY); this@AlarmActivity.finish() }
        lifecycleScope.launch(Dispatchers.IO) {keepScreenON()  }
    }

    /** this  function will get the message form the intent and will set it on the mutable State  that is  passed in */
    private fun parseTheIntent(): AlarmActivityIntentData?{
        val parsedIntentData =intent.getParcelableExtra("intentData", AlarmActivityIntentData::class.java)
        logD("the message present in the intent is $parsedIntentData")
        return parsedIntentData
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logD("New Intent received in AlarmActivity")
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
        runCatching {
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
            dismissTheAlarm()
            finishAndRemoveTask()
            activityScope.cancel()
        }.fold(onSuccess = {}, onFailure = {exception ->
            logD("there is a exception while destroying the AlarmActivity ->  ${exception.message}\n-->$exception")
        })
    }
    private fun makeDismissIntent(): Intent{
        return this.intentReceived.apply {
            action = AlarmService.ACTION_DISMISS_ALARM
            setClass(this@AlarmActivity,AlarmService::class.java)
        }
    }
    private fun dismissTheAlarm(){
        if (dismissIntent == null) dismissIntent = makeDismissIntent()
        startService(dismissIntent)
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
fun TimeDisplay(onFinish: () -> Unit, message: String,  modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    logD("in the timeDisplay() and the message is $message")

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(500)
        }
    }
    logD("in message is empty ${message.isEmpty()}")


    Scaffold(
        modifier = Modifier.fillMaxSize().background(color = Color.Black),
        containerColor = Color.Black
    ) { edgeToEdgePadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(edgeToEdgePadding),
            contentAlignment = Alignment.Center
        ) {
            if (message.isNotEmpty()) {
                // Layout when message is present
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = currentTime,
                        color = Color.Red,
                        fontSize = 63.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(44.dp))

                    // Scrollable message area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = (screenHeight / 8) + 75.dp + 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = message,
                                color = Color.Cyan,
                                fontSize = 43.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 60.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Layout when no message - time centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = currentTime,
                        color = Color.Red,
                        fontSize = 63.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(65.dp))
            Button(
                onClick = { onFinish() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xffab0c00),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (screenHeight / 8))
                    .height(80.dp)
                    .width(270.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(28.dp))
            ) {
                Text(text = "Cancel alarm")
            }
        }
    }
}

// Helper function to get current time
fun getCurrentTime(): String {
    return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
}
