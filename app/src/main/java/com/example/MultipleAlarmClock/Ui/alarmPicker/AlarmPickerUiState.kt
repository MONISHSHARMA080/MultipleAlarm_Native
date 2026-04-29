package com.example.MultipleAlarmClock.Ui.alarmPicker

import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
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
		freqGottenAfterCallback = 1
	),
	val validationResult: ValidationResult = ValidationResult.Success,
	val isLoading: Boolean = false,
	val areAllPermissionsGranted: Boolean = true
){
	override fun toString(): String {
		return "AlarmPickerUiState: alarmObject:$alarmObject , \n  validationResult:$validationResult , \n isLoading:$isLoading , \n  areAllPermissionsGranted:$areAllPermissionsGranted \n   "
	}
}

sealed interface AlarmPickerEvent {
	data object NavigateBack : AlarmPickerEvent
	data class ShowPermissionDialog(val missingSteps: List<PermissionStep>) : AlarmPickerEvent
	data object UpdateDataStoreGranted : AlarmPickerEvent
}
