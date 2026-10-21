package com.coolApps.MultipleAlarmClock.util

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
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

fun Offering.trialPeriodFor(productIds: List<String>): Period? = availablePackages.firstOrNull { it.product.id in productIds }?.product?.freeTrialPhase()?.billingPeriod

data class TrialOfferDetails(
	val trialPeriod: Period?,
	val fullPrice: String?,
	val fullPriceUnit: Period?,
)

fun extractTrialOfferDetails(offering: Offering): TrialOfferDetails {
	val trialPhaseProduct = offering.availablePackages.firstOrNull { pkg ->
		pkg.product.freeTrialPhase() != null
	}

	val product: StoreProduct? = trialPhaseProduct?.product ?: offering.availablePackages.firstOrNull()?.product

	val freePhase = product?.freeTrialPhase()
	val pricePhase = product?.defaultOption?.fullPricePhase

	return TrialOfferDetails(
		trialPeriod = freePhase?.billingPeriod,
		fullPrice = pricePhase?.price?.formatted,
		fullPriceUnit = pricePhase?.billingPeriod,
	)
}

private fun StoreProduct.freeTrialPhase(): PricingPhase? {
	val default = defaultOption ?: return null
	return default.freePhase?.takeIf { it.offerPaymentMode == OfferPaymentMode.FREE_TRIAL }
}