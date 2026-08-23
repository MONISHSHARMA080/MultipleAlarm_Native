package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker

import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.AlarmObject
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.Permissions.PermissionStep
import java.util.Calendar

enum class Progress{StartTime, EndTime, FullEditor}

/** [areAllPermissionsGranted] - make sure to set the right value when loading this, as the true is a false value */
data class AlarmPickerUiState(
	val alarmObject: AlarmObject = AlarmObject(
		startTime = Calendar.getInstance().apply {
			add(Calendar.MINUTE, 1)
			set(Calendar.SECOND, 0)
		},
		endTime = Calendar.getInstance().apply {
			add(Calendar.MINUTE, 45)
			set(Calendar.SECOND, 0)
		},
		date = Calendar.getInstance().timeInMillis,
		message = "",
		freqGottenAfterCallback = 5,
		alarmSoundUri = null
	),
	val validationResult: ValidationResult = ValidationResult.Success,
	val isLoading: Boolean = false,
	val areAllPermissionsGranted: Boolean = true,
	// if this is null then we are creating a new alarm else if not null then we are editing an existing alarm
	var initialAlarm: AlarmData? = null,
	val showPermissionDialog: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmOperationCompletedGoBack: Boolean = false,
	val progress: Progress = if(initialAlarm == null) Progress.StartTime else Progress.FullEditor

)

