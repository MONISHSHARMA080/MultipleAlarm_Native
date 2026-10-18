package com.coolApps.MultipleAlarmClock.presentation.home

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.domain.usecase.InAppReviewEligibilityChecker


data class AlarmContainerUiState(
		val alarmList: List<AlarmData>? = null,
	val showReviewUi: Boolean = false,
	val feedbackPrompt: InAppFeedbackPromptUiState = InAppFeedbackPromptUiState(),
	val reviewInelligiblityReason: InAppReviewEligibilityChecker.Reason? = null
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
