package com.coolApps.MultipleAlarmClock.presentation.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import com.coolApps.MultipleAlarmClock.util.toAnalyticsString
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
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
	
	fun onPurchaseCompletedEvent(customerInfo: CustomerInfo, storeTransaction: StoreTransaction){
		viewModelScope.launch {
			analytics.captureEvent("purchase_completed",mapOf(
				"customerInfo" to customerInfo.toString(),
				"storeTransaction" to storeTransaction.toAnalyticsString(),
			))
			TrialReminderScheduler.scheduleIfOnTrial(context, customerInfo)
		}
	}
	fun onRestoreCompletedEvent(customerInfo:CustomerInfo){
		viewModelScope.launch {
			analytics.captureEvent("restore_completed",mapOf(
				"customerInfo" to customerInfo.toString()
			))
		}
	}

	fun screen(screenName:String, properties: Map<String, Any>? = null): Unit {
		viewModelScope.launch {
			analytics.screen(screenName, properties)
		}
	}

}
