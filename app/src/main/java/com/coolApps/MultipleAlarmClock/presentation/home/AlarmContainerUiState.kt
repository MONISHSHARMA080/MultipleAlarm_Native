@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation.home

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.domain.usecase.InAppReviewEligibilityChecker


data class AlarmContainerUiState(
	val alarmList: List<AlarmData>? = null,
	val showFeedbackUI: Boolean = false,
	val showReviewUi: Boolean = false,
	val reviewInelligiblityReason: InAppReviewEligibilityChecker.Reason? = null
)