package com.coolApps.MultipleAlarmClock.util

data class FeatureFlagsData(
		val minDaysSinceInstall: Int = 3,
		val minAlarmsCreated: Int = 1,
		val cooldownDays: Int = 30,
		val inAppReviewEnabled: Boolean = true,
		val feedbackPromptConfig: FeedbackPromptConfig = FeedbackPromptConfig(),
		val isHardPaywallEnabled: Boolean = false,
		val isWeeklyRepeatPremium: Boolean = true,
		val isSnoozePremium: Boolean = false,
		val isSoundPickerPremium: Boolean = true,
		val isCustomAlarmVolumePremium: Boolean = false,
		val isBannerAdsEnabled: Boolean = false,
		val isInterstitialAdsEnabled: Boolean = false,
)

data class FeedbackPromptConfig(
		val enabled: Boolean = true,
		val startDay: Int = 14,
		val cooldownDays: Int = 14,
)

data class EngagementConfig(
		val enabled: Boolean,
		val inactiveDays: Long,
		val checkIntervalHours: Long,
		val notificationTimeSlot: OfflineNotificationTimeSlot
) {
	override fun toString(): String {
		return "EngagementConfig(enabled=$enabled, inactiveDays=$inactiveDays, checkIntervalHours=$checkIntervalHours, notificationTimeSlot=$notificationTimeSlot)"
	}
}
