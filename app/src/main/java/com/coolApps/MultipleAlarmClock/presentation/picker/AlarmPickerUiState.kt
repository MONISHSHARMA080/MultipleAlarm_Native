package com.coolApps.MultipleAlarmClock.presentation.picker

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep
import java.time.DayOfWeek
import java.util.Calendar

sealed interface AlarmEditorMode {
	// could be used while from the onboarding, e.g.  I have made an alarm in onboarding and I want to launch the alarmPickerScreen
	data class Create(val preset: AlarmData? = null) : AlarmEditorMode
	data class Edit(val alarm: AlarmData) : AlarmEditorMode
}

sealed interface AlarmCompletionAction {
	data class Saved(val alarmData: AlarmData) : AlarmCompletionAction
	data object Deleted : AlarmCompletionAction
}

data class AlarmPickerUiState(
		val alarmData: AlarmData = AlarmData(
		startTime = Calendar.getInstance().apply {
			add(Calendar.MINUTE, 1)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis,
		endTime = Calendar.getInstance().apply {
			add(Calendar.MINUTE, 45)
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis,
		message = "",
		frequencyInMin = 5,
		sound = null,
		isReadyToUse = true
	),
		val validationResult: AlarmDataValidationResult = AlarmDataValidationResult.Success,
		val isLoading: Boolean = false,
		val areAllPermissionsGranted: Boolean = false,
	// if this is null then we are creating a new alarm else if not null then we are editing an existing alarm
		val initialAlarm: AlarmData? = null,
		val showPermissionDialog: Boolean = false,
		val missingSteps: List<PermissionStep> = emptyList(),
		/** Set when the user hit a paywalled feature; consumed once the paywall is showing. */
	val paywallTriggerType: PaywallTriggerType? = null,
		val pendingRepeatDay: DayOfWeek? = null, // remember what the user was trying to do
		val pendingSound: AlarmSound? = null,
		val pendingRingDuration: Long? = null,
		val completedAction: AlarmCompletionAction? = null,
		val soundSelectionCompletedGoBack: Boolean = false,
)