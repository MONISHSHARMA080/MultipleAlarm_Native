package com.coolApps.MultipleAlarmClock.data.paywall

import com.coolApps.MultipleAlarmClock.util.toAnalyticsString
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

sealed interface PaywallEvent {
    val eventName: String
    val eventParams: Map<String, String>

    data class PurchaseStarted(val rcPackage: Package) : PaywallEvent {
        override val eventName = "paywall_purchase_started"
        override val eventParams = mapOf(
            "package_identifier" to rcPackage.identifier,
            "product_id" to rcPackage.product.id,
            "rcPackage" to rcPackage.toString()
        )
    }

    data class PurchaseCompleted(
        val customerInfo: CustomerInfo, 
        val transaction: StoreTransaction
    ) : PaywallEvent {
        override val eventName = "paywall_purchase_completed"
        override val eventParams = mapOf(
            "order_id" to (transaction.orderId ?: "unknown"),
            "product_ids" to transaction.productIds.joinToString(","),
            "active_entitlements" to customerInfo.entitlements.active.keys.joinToString(","),
            "customerInfo" to customerInfo.toString(),
            "storeTransaction" to transaction.toAnalyticsString()
        )
    }

    data object PurchaseCancelled : PaywallEvent {
        override val eventName = "paywall_purchase_cancelled"
        override val eventParams = mapOf("status" to "cancelled")
    }

    data class PurchaseError(val error: PurchasesError) : PaywallEvent {
        override val eventName = "paywall_purchase_error"
        override val eventParams = mapOf(
            "error_code" to error.code.name,
            "error_message" to error.message,
            "purchase_error" to error.toString()
        )
    }

    data object RestoreStarted : PaywallEvent {
        override val eventName = "paywall_restore_started"
        override val eventParams = mapOf("status" to "started")
    }

    data class RestoreCompleted(val customerInfo: CustomerInfo) : PaywallEvent {
        override val eventName = "paywall_restore_completed"
        override val eventParams = mapOf(
            "active_entitlements" to customerInfo.entitlements.active.keys.joinToString(","),
            "customerInfo" to customerInfo.toString()
        )
    }

    data class RestoreError(val error: PurchasesError) : PaywallEvent {
        override val eventName = "paywall_restore_error"
        override val eventParams = mapOf(
            "error_code" to error.code.name,
            "error_message" to error.message,
            "restore_error" to error.toString()
        )
    }
}
