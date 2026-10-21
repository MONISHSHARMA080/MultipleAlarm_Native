package com.coolApps.MultipleAlarmClock.presentation.onboarding

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep

enum class DisplaySate2 {
	Problem,
	UserStruggles,
	UserProfile,
	Permission,
	FirstAlarmIntro,
	CreateFirstAlarm,
	AlarmResult,
	PrePaywall, // prepare screen for hard paywall
	OnboardingPaywall;

	override fun toString(): String {
		return when(this){
			Problem -> "Problem"
			UserStruggles -> "UserStruggles"
			UserProfile -> "UserProfile"
			Permission -> "Permission"
			FirstAlarmIntro -> "FirstAlarmIntro"
			CreateFirstAlarm -> "CreateFirstAlarm"
			AlarmResult -> "AlarmResult"
			PrePaywall -> "PrePaywall"
			OnboardingPaywall ->"OnboardingPaywall"
		}
	}
}


data class OnboardingUiState2(
	val displaySate: DisplaySate2 = DisplaySate2.Problem,
	val askForNotificationPermission: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmData: AlarmData? = null,
	val allCriticalGranted: Boolean = false,
	val onboardingFlow: OnboardingFlow = OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED
){
	override fun toString(): String {
		return "OnboardingUiState2: DisplayState: $displaySate , askForNotificationPermission: $askForNotificationPermission, alarmData: $alarmData, missingSteps: $missingSteps, allCriticalGranted: $allCriticalGranted, onboardingFlow: $onboardingFlow"
	}
}
