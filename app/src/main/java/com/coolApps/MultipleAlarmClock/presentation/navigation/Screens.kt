package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import kotlinx.serialization.Serializable


@Serializable
sealed interface Screen : NavKey {
	val screenName: String

	@Serializable
	data object OnboardingScreen : Screen {
		override val screenName = "OnboardingScreen"
	}

	@Serializable
	data object AlarmContainer : Screen {
		override val screenName = "AlarmContainer"
	}

	@Serializable
	data class AlarmFlow(
			val alarmData: AlarmData? = null
	) : Screen {
		override val screenName = "AlarmFlow"
	}

	@Serializable
	data object SettingsScreen : Screen {
		override val screenName = "SettingsScreen"
	}

	@Serializable
	data object Paywall : Screen {
		override val screenName = "Paywall"
	}

	@Serializable
	data object CustomerCenter : Screen {
		override val screenName = "CustomerCenter"
	}
}

@Serializable
 sealed interface AlarmFlowRoute : NavKey {
	val screenName: String
	@Serializable
	data object AlarmPicker : AlarmFlowRoute{
		override val screenName ="AlarmPicker"
	}

	@Serializable
	data object AlarmSoundListScreen : AlarmFlowRoute{
		override val screenName ="AlarmSoundListScreen"
	}
}
