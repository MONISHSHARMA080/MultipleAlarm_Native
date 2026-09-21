package com.coolApps.MultipleAlarmClock.presentation.picker

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import android.net.Uri

data class AlarmSound(
	val title: String,
	val soundUri: Uri
)