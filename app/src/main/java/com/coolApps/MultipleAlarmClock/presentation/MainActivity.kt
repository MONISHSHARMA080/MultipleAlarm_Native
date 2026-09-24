@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.coolApps.MultipleAlarmClock.presentation.theme.AppTheme
import com.coolApps.MultipleAlarmClock.presentation.navigation.NavigationStack
import com.coolApps.MultipleAlarmClock.presentation.navigation.NavigationViewModel
import com.coolApps.MultipleAlarmClock.presentation.navigation.Screen
import com.coolApps.MultipleAlarmClock.data.background.EngagementScheduler
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.NotificationHandler
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private val navViewModel: NavigationViewModel by viewModels()
	@Inject lateinit var analytics: Analytics
	@Inject lateinit var notificationHandler: NotificationHandler
	private val coroutineScope = CoroutineScope(Dispatchers.Main)

	override fun onCreate(savedInstanceState: Bundle?) {
		val splashScreen = installSplashScreen()
		super.onCreate(savedInstanceState)
		val deepLinkScreen: Screen? = parseDeepLinkIntent(intent)

		coroutineScope.launch(Dispatchers.Main) {
			notificationHandler.createNotificationChannels()
			EngagementScheduler.ensureScheduled(this@MainActivity, analytics.getEngagementConfig())
		}

		try {
			enableEdgeToEdge()

			setContent {
				AppTheme {
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

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
	}

	fun parseDeepLinkIntent(intent: Intent?): Screen? {
		if (intent == null || intent.action != Intent.ACTION_VIEW) return null
		val data: Uri = intent.data ?: return null
		logD("Deep link is data:$data and intent.data:${intent.data} intent.action: ${intent.action} and intent:$intent")
		return when (data.scheme) {
			"alarmapp" if data.host == "home" -> Screen.AlarmContainer
			"alarmapp" if data.host == "settings" -> Screen.SettingsScreen
			"alarmapp" if data.host == "create" -> Screen.AlarmFlow(null)
			"alarmapp" if data.host == "onboarding" -> Screen.OnboardingScreen
			else -> null
		}
	}
}

fun logD(message: String): Unit {
	Log.d("AAAAA", message)
}
