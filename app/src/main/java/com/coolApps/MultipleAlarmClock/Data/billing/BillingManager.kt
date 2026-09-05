package com.coolApps.MultipleAlarmClock.Data.billing

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BillingManager handles the integration with RevenueCat.
 * It tracks the user's entitlement status and provides methods to refresh customer info.
 */
@Singleton
class BillingManager @Inject constructor() {

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    /**
     * Flow containing the latest CustomerInfo from RevenueCat.
     */
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    /**
     * Flow tracking if the user has the 'com_coolapps_multiplealarmclock_pro' entitlement active.
     */
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val entitlementId = "com_coolapps_multiplealarmclock_pro"

    init {
        // Set up the listener for any changes to customer info (e.g., after a purchase)
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
            updateCustomerInfo(info)
        }

        // Fetch initial customer info using the cache if available to avoid unnecessary network calls
        fetchCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED)
    }

    private fun updateCustomerInfo(info: CustomerInfo) {
        _customerInfo.value = info
        _isPro.value = info.entitlements[entitlementId]?.isActive ?: false
    }

    /**
     * Manually refreshes the customer info from the RevenueCat servers.
     * Uses [CacheFetchPolicy.FETCH_CURRENT] to force a network request.
     */
    fun refresh() {
        fetchCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
    }

    private fun fetchCustomerInfo(policy: CacheFetchPolicy = CacheFetchPolicy.CACHED_OR_FETCHED) {
        Purchases.sharedInstance.getCustomerInfo(policy, object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateCustomerInfo(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                // SDK already logs detailed errors to Logcat if debugLogsEnabled is true
            }
        })
    }
}
