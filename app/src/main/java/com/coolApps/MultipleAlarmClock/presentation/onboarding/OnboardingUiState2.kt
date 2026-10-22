package com.coolApps.MultipleAlarmClock.presentation.onboarding

import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep

enum class DisplaySate2 {
	Problem,
	UserStruggles,
	UserProfile,
	Permission,
	PrePaywall,
	OnboardingPaywall;

	override fun toString(): String {
		return when(this){
			Problem -> "Problem"
			UserStruggles -> "UserStruggles"
			UserProfile -> "UserProfile"
			Permission -> "Permission"
			PrePaywall -> "PrePaywall"
			OnboardingPaywall ->"OnboardingPaywall"
		}
	}
}


data class OnboardingUiState2(
	val displaySate: DisplaySate2 = DisplaySate2.Problem,
	val askForNotificationPermission: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val allCriticalGranted: Boolean = false,
	val onboardingFlow: OnboardingFlow = OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED
){
	override fun toString(): String {
		return "OnboardingUiState2: DisplayState: $displaySate , askForNotificationPermission: $askForNotificationPermission, missingSteps: $missingSteps, allCriticalGranted: $allCriticalGranted, onboardingFlow: $onboardingFlow"
	}
}
