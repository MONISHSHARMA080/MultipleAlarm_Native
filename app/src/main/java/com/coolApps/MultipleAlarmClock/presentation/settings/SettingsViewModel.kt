package com.coolApps.MultipleAlarmClock.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager
import com.coolApps.MultipleAlarmClock.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val analytics: Analytics,
	private val entitlementManager: EntitlementManager, // <-- add

)  : ViewModel() {

	val isPremium: StateFlow<Boolean> = entitlementManager.isPremium

	fun submitFeedback(feedback: String): Unit {
		viewModelScope.launch {
			analytics.captureEvent("feedback given", mapOf(
				"feedback" to feedback
			))
		}

	}

	fun captureEvent(title: String, properties: Map<String, Any> ): Unit {
		viewModelScope.launch {
			analytics.captureEvent(title, properties)
		}
	}

}