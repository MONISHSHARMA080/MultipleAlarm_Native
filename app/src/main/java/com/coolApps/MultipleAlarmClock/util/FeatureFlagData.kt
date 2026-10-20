package com.coolApps.MultipleAlarmClock.util

data class FeatureFlagsData(
		val minDaysSinceInstall: Int = 7,
		val minAlarmsCreated: Int = 3,
		val cooldownDays: Int = 8,
		val inAppReviewEnabled: Boolean = true,
		val feedbackPromptConfig: FeedbackPromptConfig = FeedbackPromptConfig(),
		val isHardPaywallEnabled: Boolean = false,
		val isWeeklyRepeatPremium: Boolean = true,
)

data class FeedbackPromptConfig(
		val enabled: Boolean = true,
		val startDay: Int = 2,
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
