package com.example.MultipleAlarmClock.Ui.Navigation

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable
sealed interface Screen : NavKey {
	@Serializable
	data object OnboardingScreen : Screen

	@Serializable
	data object AlarmContainer : Screen

	@Serializable
	data class AlarmFlow(
		val alarmData: AlarmData? = null
	) : Screen

	@Serializable
	data object SettingsScreen : Screen
}

@Serializable
 sealed interface AlarmFlowRoute : NavKey {
	@Serializable
	data object AlarmPicker : AlarmFlowRoute

	@Serializable
	data object AlarmSoundListScreen : AlarmFlowRoute
}
