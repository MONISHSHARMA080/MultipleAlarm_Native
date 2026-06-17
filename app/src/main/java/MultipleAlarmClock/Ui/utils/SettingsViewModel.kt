package com.example.MultipleAlarmClock.Ui.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val analytics: Analytics,
)  : ViewModel() {
	fun submitFeedback(feedback: String): Unit {
		viewModelScope.launch {
			analytics.captureEvent("feedback given", mapOf(
				"feedback" to feedback
			))
		}

	}

}