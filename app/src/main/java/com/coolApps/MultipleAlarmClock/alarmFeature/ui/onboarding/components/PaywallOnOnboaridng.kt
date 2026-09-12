package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
 fun OnboardingPaywallScreen(onFinished: () -> Unit, offering: Offering?, loadFailed:Boolean) {
	when {
		offering != null -> {
			Box(modifier = Modifier.fillMaxSize()) {
				Paywall(
					options = PaywallOptions.Builder(
						dismissRequest = { onFinished() } // user closes it -> continue onboarding
					)
						.setOffering(offering)
						.setListener(object : PaywallListener {
							override fun onPurchaseCompleted(
									customerInfo: CustomerInfo,
									storeTransaction: StoreTransaction
							) {
								onFinished()
							}
							override fun onRestoreCompleted(customerInfo: CustomerInfo) {
								onFinished()
							}
						})
						.build()
				)

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
		loadFailed -> {
			// offerings failed to load (e.g. offline) — don't block onboarding
			LaunchedEffect(Unit) { onFinished() }
		}
		else -> {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		}
	}
}