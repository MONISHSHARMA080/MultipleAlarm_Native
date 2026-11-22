package com.coolApps.MultipleAlarmClock.presentation.home

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.domain.usecase.InAppReviewEligibilityChecker
import com.google.android.play.core.appupdate.AppUpdateInfo

sealed interface AlarmListItem {
	val key: String

	data class Alarm(val alarmData: AlarmData) : AlarmListItem {
		override val key: String get() = "alarm_${alarmData.id}"
	}

	data class Ad(val slotIndex: Int) : AlarmListItem {
		override val key: String get() = "ad_$slotIndex"
	}
}

data class AlarmContainerUiState(
	val items: List<AlarmListItem>? = null,
	val showReviewUi: Boolean = false,
	val feedbackPrompt: InAppFeedbackPromptUiState = InAppFeedbackPromptUiState(),
	val reviewInelligiblityReason: InAppReviewEligibilityChecker.Reason? = null,
)

data class InAppFeedbackPromptUiState(
	val isVisible: Boolean = false,
	val rating: Int? = null,
	val step: InAppFeedbackPromptStep = InAppFeedbackPromptStep.Rating,
)

enum class InAppFeedbackPromptStep {
	Rating,
	Review,
}

sealed interface AppUpdateState {
	data object Idle : AppUpdateState
	data class Available(
		val info: AppUpdateInfo,
		val stalenessDays: Int?,
		val updatePriority: Int,
		val shouldPromptFlexible: Boolean,
		val shouldPromptImmediate: Boolean,
	) : AppUpdateState
	data class Downloading(val progress: Float = 0f) : AppUpdateState
	data object ReadyToInstall : AppUpdateState
}
