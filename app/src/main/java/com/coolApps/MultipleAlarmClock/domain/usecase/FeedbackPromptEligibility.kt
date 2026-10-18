package com.coolApps.MultipleAlarmClock.domain.usecase

import com.coolApps.MultipleAlarmClock.util.FeedbackPromptConfig

class FeedbackPromptEligibilityChecker {
	fun isEligible(
		installEpochTimeMs: Long,
		lastPromptAttemptEpochTimeMs: Long,
		config: FeedbackPromptConfig,
		nowEpochMs: Long = System.currentTimeMillis(),
	): Boolean {
		if (!config.enabled || installEpochTimeMs <= 0L) return false

		val daysSinceInstall = (nowEpochMs - installEpochTimeMs).coerceAtLeast(0L) / MILLIS_PER_DAY
		if (daysSinceInstall < config.startDay) return false

		val daysSinceLastPrompt = (nowEpochMs - lastPromptAttemptEpochTimeMs).coerceAtLeast(0L) / MILLIS_PER_DAY
		return lastPromptAttemptEpochTimeMs <= 0L || daysSinceLastPrompt >= config.cooldownDays
	}

	private companion object {
		const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
	}
}
