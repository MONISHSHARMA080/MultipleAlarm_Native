package com.example.MultipleAlarmClock.Ui.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.launch

class SettingsViewModel @Inject constructor(
	private val analytics: Analytics,
	@ApplicationContext  val context: Context
)  : ViewModel() {
	fun submitFeedback(feedback: String): Unit {
		viewModelScope.launch {
			analytics.captureEvent("feedback given", mapOf(
				"feedback" to feedback
			))
		}

	}

}