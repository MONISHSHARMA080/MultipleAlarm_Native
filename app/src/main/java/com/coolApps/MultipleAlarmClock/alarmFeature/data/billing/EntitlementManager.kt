package com.coolApps.MultipleAlarmClock.alarmFeature.data.billing

import com.coolApps.MultipleAlarmClock.logD
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@Singleton
class EntitlementManager @Inject constructor() {
	private val _isPremium = MutableStateFlow(false)
	val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
	private val purchases by lazy { Purchases.sharedInstance }

	init {
		purchases.updatedCustomerInfoListener =
			UpdatedCustomerInfoListener { customerInfo ->
				updateFrom(customerInfo)
			}
		purchases.getCustomerInfoWith(
			onSuccess = { customerInfo ->
				_isPremium.value = customerInfo.entitlements[RevenueCatEntitlements.PREMIUM]?.isActive == true
			},
			onError = { error ->
				logD("GooglePlayPaywallManager Error fetching customer info: $error")
			}
		)

	}

	suspend fun refresh() {
		try {
			val customerInfo =
				Purchases.sharedInstance.awaitCustomerInfo()
			updateFrom(customerInfo)
		} catch (e: Exception) {
			// Keep the last known state.
			// Log/report if appropriate.
		}
	}

	private fun updateFrom(customerInfo: CustomerInfo) {
		_isPremium.value =
			customerInfo.entitlements
				.active
				.containsKey(RevenueCatEntitlements.PREMIUM)
	}
	object RevenueCatEntitlements {
		const val PREMIUM = "premium"
	}
}