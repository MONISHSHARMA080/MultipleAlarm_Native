package com.coolApps.MultipleAlarmClock.presentation.onboarding

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep

enum class ButtonState {
	Enabled,
	Disabled,
	Hidden
}

enum class DisplaySate {
	Problem,
	UserStruggles,
	Permission,
	FirstAlarmIntro,
	CreateFirstAlarm,
	AlarmResult,
	OnboardingPaywall;

	override fun toString(): String {
		return when(this){
			Problem -> "Problem"
			UserStruggles -> "UserStruggles"
			Permission -> "Permission"
			FirstAlarmIntro -> "FirstAlarmIntro"
			CreateFirstAlarm -> "CreateFirstAlarm"
			AlarmResult -> "AlarmResult"
			OnboardingPaywall ->"OnboardingPaywall"
		}
	}
}


data class OnboardingUiState(
	val displaySate: DisplaySate = DisplaySate.Problem,
	val askForNotificationPermission: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmData: AlarmData? = null,
	val	allCriticalGranted: Boolean = false
){
	override fun toString(): String {
		return "OnboardingUiState: DisplayState: $displaySate , askForNotificationPermission: $askForNotificationPermission, alarmData: $alarmData, missingSteps: $missingSteps, allCriticalGranted: $allCriticalGranted   "
	}
}