package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager.RevenueCatEntitlements
import com.coolApps.MultipleAlarmClock.presentation.paywall.PaywallEvent
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)

@Composable
 fun OnboardingPaywallScreen(
		isHardPaywall: Boolean,
		onBackPress: () -> Unit,
		onFinished: () -> Unit, offering: Offering?,
		onButtonStateChange: (ButtonState) -> Unit,
		onEvent: (PaywallEvent) -> Unit
) {
	onButtonStateChange(ButtonState.Hidden)
	BackHandler {
		if (isHardPaywall) {
			onBackPress()
		} else {
			onFinished()
		}
	}

	 // buttonState is now pre-set by the parent before this screen composes
	when {
		offering != null -> {
			Box(modifier = Modifier.fillMaxSize()) {
				Paywall(
					options = PaywallOptions.Builder(
						dismissRequest = { if (!isHardPaywall) onFinished() } // hard paywall: not dismissible
					)
						.setOffering(offering)
						.setListener(object : PaywallListener {
							override fun onPurchaseStarted(rcPackage: Package) = onEvent(PaywallEvent.PurchaseStarted(rcPackage))
							override fun onPurchaseCancelled() = onEvent(PaywallEvent.PurchaseCancelled)
							override fun onPurchaseError(error: PurchasesError) = onEvent(PaywallEvent.PurchaseError(error))
							override fun onRestoreStarted() = onEvent(PaywallEvent.RestoreStarted)
							override fun onRestoreError(error: PurchasesError) = onEvent(PaywallEvent.RestoreError(error))
							override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
								onEvent(PaywallEvent.PurchaseCompleted(customerInfo, storeTransaction))
								onFinished()
							}
							
							override fun onRestoreCompleted(customerInfo: CustomerInfo) {
								onEvent(PaywallEvent.RestoreCompleted(customerInfo))
								val hasPremium = customerInfo.entitlements[RevenueCatEntitlements.PREMIUM]?.isActive == true
								if (hasPremium) {
									onFinished()
								}
							}
						})
						.build()
				)

				if (!isHardPaywall) {
					FilledTonalIconButton(
						onClick = onFinished,
						colors = IconButtonDefaults.filledTonalIconButtonColors(),
						modifier = Modifier
							.statusBarsPadding()
							.padding(16.dp)
					) {
						Icon(
							painterResource(R.drawable.clear),
							null
						)
					}
				}
			}

		}
		else-> {
			// offerings failed to load (e.g. offline) — don't block onboarding
			LaunchedEffect(Unit) { onFinished() }
		}
//		else -> {
//			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//				CircularProgressIndicator()
//			}
//		}
	}
}