package com.coolApps.MultipleAlarmClock.presentation.picker

import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionStep
import java.time.DayOfWeek
import java.util.Calendar

enum class Progress{StartTime, EndTime, FullEditor}

/** [areAllPermissionsGranted] - make sure to set the right value when loading this, as the true is a false value */
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
	val showPaywall: Boolean = false,
	val pendingRepeatDay: DayOfWeek? = null, // remember what the user was trying to do
	val pendingSound: AlarmSound? = null,
	val alarmOperationCompletedGoBack: Boolean = false,
	val soundSelectionCompletedGoBack: Boolean = false,
	val progress: Progress = if (initialAlarm == null) Progress.StartTime else Progress.FullEditor
)