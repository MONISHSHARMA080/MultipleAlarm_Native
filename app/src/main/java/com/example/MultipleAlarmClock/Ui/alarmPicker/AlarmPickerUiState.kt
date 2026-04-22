package com.example.MultipleAlarmClock.Ui.alarmPicker

import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import java.util.Calendar


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
	val isLoading: Boolean = false
)
