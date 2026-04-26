package com.coolApps.MultipleAlarmClock

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.NavigationStack
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.Screen
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.example.MultipleAlarmClock.Ui.Navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val navViewModel: NavigationViewModel by viewModels()
  @Inject lateinit var analytics: Analytics

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
//    splashScreen.setKeepOnScreenCondition{  navViewModel.isFirstLaunch.value == null}
    val coroutineScope = CoroutineScope( Dispatchers.IO)
    val deepLinkScreen: Screen? = parseDeepLinkIntent(intent)

    try {
      enableEdgeToEdge()
      setContent {
        MaterialTheme(colorScheme = dynamicDarkColorScheme(this)) {
            NavigationStack(
              navViewModel = navViewModel,
              deepLinkScreen = deepLinkScreen
			)
        }
      }
    } catch (e: Exception) {
      logD(" \n\n\n\n\n\n [FATAL] --> error occurred in the onCreate, and it is ${e}\n}")
      analytics.captureEvent("main activity class got error", mapOf(
        "error Exception" to e.toString()
      ))
    }
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent, caller)
    setIntent(intent)
  }

  fun parseDeepLinkIntent(intent: Intent?): Screen?{
    if (intent == null || intent.action != Intent.ACTION_VIEW) return null
    val data: Uri = intent.data ?: return null
    logD("Deep link is data:$data and  intent.data:${intent.data} intent.action: ${intent.action} and intent:$intent ")
    return when {
      data.scheme == "alarmapp" && data.host == "home" -> Screen.AlarmContainer
      else -> null
    }
  }

}

fun logD(message: String): Unit {
  Log.d("AAAAA", message)
}

