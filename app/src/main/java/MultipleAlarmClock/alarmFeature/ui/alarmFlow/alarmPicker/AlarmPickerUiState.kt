package com.example.MultipleAlarmClock.Ui.alarmPicker

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.model.AlarmObject
import MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import java.util.Calendar

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
		freqGottenAfterCallback = 1,
		alarmSoundUri = null
	),
	val validationResult: ValidationResult = ValidationResult.Success,
	val isLoading: Boolean = false,
	val areAllPermissionsGranted: Boolean = true,
	// if this is null then we are creating a new alarm else if not null then we are editing an existing alarm
	var initialAlarm: AlarmData? = null,
	val showPermissionDialog: Boolean = false,
	val missingSteps: List<PermissionStep> = emptyList(),
	val alarmOperationCompletedGoBack: Boolean = false
)

//sealed interface AlarmPickerEvent {
//	data object NavigateBack : AlarmPickerEvent
//	data class ShowPermissionDialog(val missingSteps: List<PermissionStep>) : AlarmPickerEvent
//	data object UpdateDataStoreGranted : AlarmPickerEvent
//}
