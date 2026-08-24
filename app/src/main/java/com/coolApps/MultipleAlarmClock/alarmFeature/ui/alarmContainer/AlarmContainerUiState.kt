package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer

import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.InAppReviewEligibilityChecker


data class AlarmContainerUiState(
	val alarmList: List<AlarmData>? = null,
	val showFeedbackUI: Boolean = false,
	val showReviewUi: Boolean = false,
	val reviewInelligiblityReason: InAppReviewEligibilityChecker.Reason? = null
)