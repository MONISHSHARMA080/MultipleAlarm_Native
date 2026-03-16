package com.coolApps.MultipleAlarmClock

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.NavigationStack
import com.coolApps.MultipleAlarmClock.FirstLaunchAskForPermission.FirstLaunchAskForPermission
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  val analytics by lazy { Analytics(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()

    super.onCreate(savedInstanceState)
    val coroutineScope = CoroutineScope( Dispatchers.IO)

    coroutineScope.launch {
      launch {
        runCatching {
          NotificationHandler(this@MainActivity).createNotificationChannels()
          logD("created notification Channels")
        }

      }
      launch {
        FirstLaunchAskForPermission(this@MainActivity).checkIfWeHaveNotificationPermissionElseMarkitFalse()
      }
    }


    try {
      enableEdgeToEdge()
      setContent {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
            NavigationStack(this@MainActivity)
        }
      }
    } catch (e: Exception) {
      logD(" \n\n\n\n\n\n [FATAL] --> error occurred in the onCreate, and it is ${e}\n}")
      analytics.captureEvent("main activity class got error", mapOf(
        "error Exception" to e.toString()
      ))

    }
  }
}

fun logD(message: String): Unit {
  Log.d("AAAAA", message)
}

