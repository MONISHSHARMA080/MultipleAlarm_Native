package com.example.MultipleAlarmClock.Ui.alarmPicker.data

sealed interface AlarmSoundSelection {
	data object Random : AlarmSoundSelection
	data class Custom( val alarmSound: AlarmSound ) : AlarmSoundSelection
}