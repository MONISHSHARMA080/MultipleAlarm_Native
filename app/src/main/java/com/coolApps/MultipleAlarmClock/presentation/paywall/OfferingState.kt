package com.coolApps.MultipleAlarmClock.presentation.paywall

import com.revenuecat.purchases.Offering

sealed interface OfferingState {
	data object Loading : OfferingState
	data class Loaded(val offering: Offering?) : OfferingState
	data object Failed : OfferingState
}