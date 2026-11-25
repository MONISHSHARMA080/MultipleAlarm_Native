package com.coolApps.MultipleAlarmClock.presentation.onboarding.v4

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep
import kotlinx.serialization.Serializable

@Serializable
sealed interface Onboarding4Route : NavKey {
	val stepName: String
	val progress: Float

	@Serializable
	data object UserStruggles : Onboarding4Route {
		override val stepName = "UserStruggles"
		override val progress = 1f / 7f
	}

	@Serializable
	data object UserProfile : Onboarding4Route {
		override val stepName = "UserProfile"
		override val progress = 2f / 7f
	}

	@Serializable
	data object WakeUpTime : Onboarding4Route {
		override val stepName = "WakeUpTime"
		override val progress = 3f / 7f
	}

	@Serializable
	data object Permission : Onboarding4Route {
		override val stepName = "Permission"
		override val progress = 4f / 7f
	}

	@Serializable
	data object CreateFirstAlarm : Onboarding4Route {
		override val stepName = "CreateFirstAlarm"
		override val progress = 5f / 7f
	}

	@Serializable
	data object AlarmPreview : Onboarding4Route {
		override val stepName = "AlarmPreview"
		override val progress = 6f / 7f
	}

	@Serializable
	data object PrePaywall : Onboarding4Route {
		override val stepName = "PrePaywall"
		override val progress = 6f / 7f
	}

	@Serializable
	data object OnboardingPaywall : Onboarding4Route {
		override val stepName = "OnboardingPaywall"
		override val progress = 7f / 7f
	}
}

enum class OnboardingStruggle(
	val analyticsKey: String,
	val priority: Int,
) {
	WakingUpOnTime("Waking up on time", 1),
	HeavySleeper("Heavy sleeper", 2),
	IgnoringAlarms("Ignoring alarms", 3),
	BeingOnTime("Being on time", 4),
	ForgettingThings("Forgetting things", 5);

	companion object {
		fun resolvePrimary(selectedStruggles: Set<String>): OnboardingStruggle? {
			return entries
				.filter { it.analyticsKey in selectedStruggles }
				.minByOrNull { it.priority }
		}
	}
}

data class OnboardingPersonalization(
	@param:StringRes val wakeUpTitleRes: Int,
	@param:StringRes val defaultAlarmMessageRes: Int,
	@param:StringRes val lockScreenTitleRes: Int,
	@param:StringRes val lockScreenSubtitleRes: Int,
)

fun OnboardingStruggle?.toPersonalization(): OnboardingPersonalization {
	return when (this ?: OnboardingStruggle.WakingUpOnTime) {
		OnboardingStruggle.WakingUpOnTime -> OnboardingPersonalization(
			wakeUpTitleRes = R.string.onboarding_v4_wakeup_title_waking_up,
			defaultAlarmMessageRes = R.string.onboarding_v4_default_msg_waking_up,
			lockScreenTitleRes = R.string.onboarding_v4_preview_title_waking_up,
			lockScreenSubtitleRes = R.string.onboarding_v4_preview_subtitle_waking_up,
		)
		OnboardingStruggle.HeavySleeper -> OnboardingPersonalization(
			wakeUpTitleRes = R.string.onboarding_v4_wakeup_title_heavy_sleeper,
			defaultAlarmMessageRes = R.string.onboarding_v4_default_msg_heavy_sleeper,
			lockScreenTitleRes = R.string.onboarding_v4_preview_title_heavy_sleeper,
			lockScreenSubtitleRes = R.string.onboarding_v4_preview_subtitle_heavy_sleeper,
		)
		OnboardingStruggle.IgnoringAlarms -> OnboardingPersonalization(
			wakeUpTitleRes = R.string.onboarding_v4_wakeup_title_ignoring,
			defaultAlarmMessageRes = R.string.onboarding_v4_default_msg_ignoring,
			lockScreenTitleRes = R.string.onboarding_v4_preview_title_ignoring,
			lockScreenSubtitleRes = R.string.onboarding_v4_preview_subtitle_ignoring,
		)
		OnboardingStruggle.BeingOnTime -> OnboardingPersonalization(
			wakeUpTitleRes = R.string.onboarding_v4_wakeup_title_being_on_time,
			defaultAlarmMessageRes = R.string.onboarding_v4_default_msg_being_on_time,
			lockScreenTitleRes = R.string.onboarding_v4_preview_title_being_on_time,
			lockScreenSubtitleRes = R.string.onboarding_v4_preview_subtitle_being_on_time,
		)
		OnboardingStruggle.ForgettingThings -> OnboardingPersonalization(
			wakeUpTitleRes = R.string.onboarding_v4_wakeup_title_forgetting,
			defaultAlarmMessageRes = R.string.onboarding_v4_default_msg_forgetting,
			lockScreenTitleRes = R.string.onboarding_v4_preview_title_forgetting,
			lockScreenSubtitleRes = R.string.onboarding_v4_preview_subtitle_forgetting,
		)
	}
}

data class OnboardingUiState4(
	val missingSteps: List<PermissionStep> = emptyList(),
	val allCriticalGranted: Boolean = false,
	val selectedStruggles: Set<String> = emptySet(),
	val primaryStruggle: OnboardingStruggle? = null,
	val selectedProfile: String? = null,
	val wakeUpHour: Int = 7,
	val wakeUpMinute: Int = 0,
	val defaultFrequencyMin: Long = 2L,
	val defaultWindowMinutes: Int = 30,
	val savedAlarmId: Int? = null,
	val alarmData: AlarmData? = null,
	val onboardingFlow: OnboardingFlow = OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED,
) {
	override fun toString(): String {
		return "OnboardingUiState4: primaryStruggle=$primaryStruggle, wakeUp=${wakeUpHour}:${wakeUpMinute}, alarmData=$alarmData, missingSteps=$missingSteps, allCriticalGranted=$allCriticalGranted, onboardingFlow=$onboardingFlow"
	}
}
