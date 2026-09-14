package com.coolApps.MultipleAlarmClock.alarmFeature.ui.util

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

@Composable
fun PremiumPaywallDialog(
		isPlus: Boolean,
		onPurchaseCompletedEvent:(customerInfo:CustomerInfo,storeTransaction: StoreTransaction)->Unit,
		onRestoreCompletedEvent:(customerInfo:CustomerInfo)->Unit,
		onDismiss: () -> Unit
) {
	val paywallOptions = remember {
		PaywallOptions.Builder(dismissRequest = onDismiss)
			.setListener(object : PaywallListener {
				override fun onPurchaseCompleted(
						customerInfo: CustomerInfo,
						storeTransaction: StoreTransaction
				) {
					onPurchaseCompletedEvent(customerInfo, storeTransaction)
					onDismiss()
				}
				override fun onRestoreCompleted(customerInfo: CustomerInfo) {
					onRestoreCompletedEvent(customerInfo)
					onDismiss()
				}
			})
			.build()
	}


	Scaffold { innerPadding ->
		if (!isPlus) {
			Paywall(paywallOptions)

			FilledTonalIconButton(
				onClick = onDismiss,
				colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colorScheme.secondaryContainer.copy(alpha = 0.4f)),
				modifier = Modifier
					.padding(innerPadding)
					.padding(8.dp)
			) {
				Icon(
					painterResource(R.drawable.clear),
					null
				)
			}
		} else {
			CustomerCenter(onDismiss = onDismiss)
		}
	}
}