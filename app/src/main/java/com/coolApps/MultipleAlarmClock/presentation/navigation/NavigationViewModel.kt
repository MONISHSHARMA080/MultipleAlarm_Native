package com.coolApps.MultipleAlarmClock.presentation.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.presentation.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel()
class NavigationViewModel @Inject constructor(
	private val dataStore: DataStore<Settings>,
	val analytics: Analytics,
	@ApplicationContext private val context: Context,
) : ViewModel() {

	val isFirstLaunch: StateFlow<Boolean?> = dataStore.data
		.map { it.isFirstLaunch }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = null
		)


	fun captureEvent(eventName:String, properties: Map<String, Any>): Unit {
		viewModelScope.launch {
			analytics.captureEvent(eventName, properties)
		}
	}
	
	fun onPaywallEvent(event: PaywallEvent) {
		viewModelScope.launch {
			analytics.captureEvent(event.eventName, event.eventParams)
			when (event) {
				is PaywallEvent.PurchaseCompleted -> {
					TrialReminderScheduler.scheduleIfOnTrial(context, event.customerInfo)
				}
				else -> Unit
			}
		}
	}

	fun screen(screenName:String, properties: Map<String, Any>? = null): Unit {
		viewModelScope.launch {
			analytics.screen(screenName, properties)
		}
	}

}
