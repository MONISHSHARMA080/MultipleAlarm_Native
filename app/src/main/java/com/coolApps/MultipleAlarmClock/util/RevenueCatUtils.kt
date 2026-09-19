package com.coolApps.MultipleAlarmClock.util

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.models.StoreTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension function to provide a detailed string representation of a StoreTransaction,
 * mimicking the style of RevenueCat's CustomerInfo.toString().
 * This is useful for analytics and logging where the default toString() is insufficient.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
fun StoreTransaction.toAnalyticsString(): String {
    val date = Date(this.purchaseTime)
    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)

    return "<StoreTransaction\n" +
            " orderId: $orderId\n" +
            " productIds: $productIds\n" +
            " type: $type\n" +
            " purchaseTime: $formattedDate ($purchaseTime)\n" +
            " purchaseState: $purchaseState\n" +
            " purchaseType: $purchaseType\n" +
            " presentedOfferingContext: $presentedOfferingContext\n" +
            " subscriptionOptionId: $subscriptionOptionId\n" +
            " marketplace: $marketplace\n" +
            " isAutoRenewing: $isAutoRenewing\n" +
            " storeUserID: $storeUserID\n" +
            " replacementMode: $replacementMode\n" +
            ">"
}
