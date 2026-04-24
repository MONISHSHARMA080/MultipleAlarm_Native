package com.example.MultipleAlarmClock.Data.dataStore

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
	val isFirstLaunch: Boolean,
	var allPermissionsGranted: Boolean
)