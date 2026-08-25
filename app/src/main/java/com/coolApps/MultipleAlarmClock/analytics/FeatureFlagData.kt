package com.coolApps.MultipleAlarmClock.analytics

data class FeatureFlagsData(
		val minDaysSinceInstall: Int = 7,
		val minAlarmsCreated: Int = 3,
		val cooldownDays: Int = 8,
		val inAppReviewEnabled: Boolean = true,
)

data class EngagementConfig(
		val enabled: Boolean,
		val inactiveDays: Long,
		val cooldownDays: Long,
		val checkIntervalHours: Long,
)