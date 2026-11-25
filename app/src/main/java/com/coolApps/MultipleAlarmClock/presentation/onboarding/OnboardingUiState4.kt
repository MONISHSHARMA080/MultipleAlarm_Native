package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.annotation.StringRes
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep

enum class DisplayState4 {
	UserStruggles,
	UserProfile,
	WakeUpTime,
	Permission,
	CreateFirstAlarm,
	AlarmPreview,
	PrePaywall,
	OnboardingPaywall,
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
	val displayState: DisplayState4 = DisplayState4.UserStruggles,
	val missingSteps: List<PermissionStep> = emptyList(),
	val allCriticalGranted: Boolean = false,
	val selectedStruggles: Set<String> = emptySet(),
	val primaryStruggle: OnboardingStruggle? = null,
	val selectedProfile: String? = null,
	val wakeUpHour: Int = 7,
	val wakeUpMinute: Int = 0,
	val defaultFrequencyMin: Long = 2L,
	val defaultWindowMinutes: Int = 30,
	val alarmData: AlarmData? = null,
	val onboardingFlow: OnboardingFlow = OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED,
	val canNavigateBack: Boolean = false,
) {
	override fun toString(): String {
		return "OnboardingUiState4: displayState=$displayState, primaryStruggle=$primaryStruggle, wakeUp=${wakeUpHour}:${wakeUpMinute}, alarmData=$alarmData, missingSteps=$missingSteps, allCriticalGranted=$allCriticalGranted, onboardingFlow=$onboardingFlow"
	}
}
