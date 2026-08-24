package com.coolApps.MultipleAlarmClock.alarmFeature.domain

import com.coolApps.MultipleAlarmClock.analytics.FeatureFlagsData


class InAppReviewEligibilityChecker {
	fun evaluate(
			nowEpochMs: Long = System.currentTimeMillis(),
			installEpochTimeMs: Long,
			alarmCount: Int,
			lastReviewAttemptEpochTimeMs: Long,
			config: FeatureFlagsData
	): Result {

		if (!config.inAppReviewEnabled) {
			return Result.NotEligible(
				Reason.FEATURE_DISABLED
			)
		}

		// this should not happen as we are initialising it
		if (installEpochTimeMs <= 0L) {
			return Result.NotEligible(Reason.INSTALL_DATE_UNKNOWN)
		}

		val daysSinceInstall = (nowEpochMs - installEpochTimeMs)
				.coerceAtLeast(0L)
				.toDouble() / MILLIS_PER_DAY

		if (daysSinceInstall < config.minDaysSinceInstall) {
			return Result.NotEligible(
				Reason.NOT_ENOUGH_DAYS_SINCE_INSTALL
			)
		}

		if (alarmCount < config.minAlarmsCreated) {
			return Result.NotEligible(
				Reason.NOT_ENOUGH_ALARMS
			)
		}

		if (lastReviewAttemptEpochTimeMs > 0L) {

			val daysSinceLastAttempt =
				(nowEpochMs - lastReviewAttemptEpochTimeMs)
					.coerceAtLeast(0L)
					.toDouble() /
						MILLIS_PER_DAY

			if (daysSinceLastAttempt < config.cooldownDays) {
				return Result.NotEligible(
					Reason.COOLDOWN_ACTIVE
				)
			}
		}

		return Result.Eligible
	}

	sealed interface Result {

		data object Eligible : Result

		data class NotEligible(
				val reason: Reason
		) : Result

	}

	enum class Reason {
		FEATURE_DISABLED,
		INSTALL_DATE_UNKNOWN,
		NOT_ENOUGH_DAYS_SINCE_INSTALL,
		NOT_ENOUGH_ALARMS,
		COOLDOWN_ACTIVE;

		override fun toString(): String {
			return when(this){
				FEATURE_DISABLED -> "FEATURE_DISABLED"
				INSTALL_DATE_UNKNOWN -> "INSTALL_DATE_UNKNOWN"
				NOT_ENOUGH_DAYS_SINCE_INSTALL -> "NOT_ENOUGH_DAYS_SINCE_INSTALL"
				NOT_ENOUGH_ALARMS -> "NOT_ENOUGH_ALARMS"
				COOLDOWN_ACTIVE -> "COOLDOWN_ACTIVE"
			}
		}

	}

	private companion object {
		const val MILLIS_PER_DAY =
			24L * 60L * 60L * 1000L
	}
}