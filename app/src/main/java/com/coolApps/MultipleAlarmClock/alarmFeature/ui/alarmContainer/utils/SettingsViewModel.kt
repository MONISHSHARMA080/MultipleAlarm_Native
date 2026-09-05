package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.Data.billing.BillingManager
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val analytics: Analytics,
	private val billingManager: BillingManager
)  : ViewModel() {

	val isPro: StateFlow<Boolean> = billingManager.isPro

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