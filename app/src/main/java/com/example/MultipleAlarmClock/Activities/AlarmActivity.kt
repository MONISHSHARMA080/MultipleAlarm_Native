package com.coolApps.MultipleAlarmClock.Activities

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.services.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCurrentTime(): String {
    return SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
}


class AlarmActivity : ComponentActivity() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var intentReceived: Intent
    private  val AUTO_FINISH_DELAY = 120000L // 2 sec is 120000
    private var dismissIntent : Intent? = null
    val analytics by lazy {Analytics(this)}

    override fun onCreate(savedInstanceState: Bundle?) {
         logD("about to create a new alarm")
        this.intentReceived = intent
        window.isNavigationBarContrastEnforced = false
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    analytics.captureEvent("alarm activity created", mapOf(
                        "intentData" to intentDataAccessed.toString(),
                        "class" to "AlarmActivity"
                    ))
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
        val parsedIntentData = IntentCompat.getParcelableExtra(intent, "intentData", AlarmActivityIntentData::class.java)
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
        analytics.captureEvent("receiver new intent in AlarmActivity", mapOf(
            "newIntent" to intent.toString(),
            "class" to "AlarmActivity"
        ))

        onDestroy()
        finish()
        startActivity(intent)
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
fun TimeDisplay(onFinish: () -> Unit, message: String, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(720)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeContent,
        containerColor = Color.Black,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (message.isEmpty()) screenHeight / 8 else 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onFinish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(49.dp),
                    modifier = Modifier.height(94.dp).width(327.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        modifier = Modifier.size(33.dp),
                        contentDescription = "Cancel"
                    )
                    Spacer(modifier = Modifier.width(13.dp)) // Space between icon and text
                    Text(text = "Stop", fontSize = 33.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { edgeToEdgePadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(edgeToEdgePadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (message.isEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            if (message.isEmpty()) {
                // Centered time when no message
                Text(
                    text = currentTime,
                    color = Color.Cyan,
                    fontSize = 53.sp,
                    softWrap = false,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // Original layout with message
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = currentTime,
                    color = Color.Cyan,
                    fontSize = 53.sp,
                    softWrap = false,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(40.dp))
                // Scrollable Message Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = message,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
//                            textAlign = TextAlign.Start,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black))
                            )
                    )
                }
            }
        }
    }
}
