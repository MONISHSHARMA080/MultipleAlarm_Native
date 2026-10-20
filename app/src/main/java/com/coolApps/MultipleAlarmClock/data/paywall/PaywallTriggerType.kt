package com.coolApps.MultipleAlarmClock.data.paywall

sealed interface PaywallTriggerType {
	val placementId: String

	data object OnboardingEnd : PaywallTriggerType {
		override val placementId: String = "onboarding_end"
	}
	data object PremiumFeature : PaywallTriggerType {
		override val placementId: String = "premium_feature"
	}
}
