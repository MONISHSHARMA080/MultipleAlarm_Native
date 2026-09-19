package com.coolApps.MultipleAlarmClock.presentation.onboarding

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep

enum class ButtonState {
	Enabled,
	Disabled,
	Hidden
}

enum class DisplaySate {
	Greeting,
	Problem,
	Permission,
	FirstAlarmIntro,
	CreateFirstAlarm,
	AlarmResult,
	OnboardingPaywall;

	override fun toString(): String {
		return when(this){
			Greeting -> "Greeting"
			Problem -> "Problem"
			Permission -> "Permission"
			FirstAlarmIntro -> "FirstAlarmIntro"
			CreateFirstAlarm -> "CreateFirstAlarm"
			AlarmResult -> "AlarmResult"
			OnboardingPaywall ->"OnboardingPaywall"
		}
	}
}


data class OnboardingUiState(
	val displaySate: DisplaySate = DisplaySate.Greeting,
	val askForNotificationPermission: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmData: AlarmData? = null,
	val	allCriticalGranted: Boolean = false
){
	override fun toString(): String {
		return "OnboardingUiState: DisplayState: $displaySate , askForNotificationPermission: $askForNotificationPermission, alarmData: $alarmData, missingSteps: $missingSteps, allCriticalGranted: $allCriticalGranted   "
	}
}