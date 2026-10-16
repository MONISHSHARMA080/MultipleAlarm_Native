package com.coolApps.MultipleAlarmClock.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.paywall.PaywallEvent
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PremiumPaywallDialog(
		isPlus: Boolean,
		onEvent: (PaywallEvent) -> Unit,
		onDismiss: () -> Unit
) {
	val paywallOptions = remember {
		PaywallOptions.Builder(dismissRequest = onDismiss)
			.setListener(object : PaywallListener {
				override fun onPurchaseStarted(rcPackage: Package) = onEvent(PaywallEvent.PurchaseStarted(rcPackage))
				override fun onPurchaseCancelled() = onEvent(PaywallEvent.PurchaseCancelled)
				override fun onPurchaseError(error: PurchasesError) = onEvent(PaywallEvent.PurchaseError(error))
				override fun onRestoreStarted() = onEvent(PaywallEvent.RestoreStarted)
				override fun onRestoreError(error: PurchasesError) = onEvent(PaywallEvent.RestoreError(error))
				
				override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
					onEvent(PaywallEvent.PurchaseCompleted(customerInfo, storeTransaction))
					onDismiss()
				}
				override fun onRestoreCompleted(customerInfo: CustomerInfo) {
					onEvent(PaywallEvent.RestoreCompleted(customerInfo))
					onDismiss()
				}
			})
			.build()
	}

	val closeInteractionSource = remember { MutableInteractionSource() }
	val isClosePressed by closeInteractionSource.collectIsPressedAsState()
	val closeCorner by animateDpAsState(
		targetValue = if (isClosePressed) 8.dp else 14.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "closeCorner"
	)
	val closeScale by animateFloatAsState(
		targetValue = if (isClosePressed) 0.92f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "closeScale"
	)

	Scaffold(
		containerColor = colorScheme.surface
	) { innerPadding ->
		if (!isPlus) {
			Box(modifier = Modifier.fillMaxSize()) {
				Paywall(paywallOptions)

				FilledTonalIconButton(
					onClick = onDismiss,
					interactionSource = closeInteractionSource,
					shape = RoundedCornerShape(closeCorner),
					colors = IconButtonDefaults.filledTonalIconButtonColors(
						containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
						contentColor = colorScheme.onSurface
					),
					modifier = Modifier
						.align(Alignment.TopStart)
						.padding(innerPadding)
						.padding(12.dp)
						.size(44.dp)
						.graphicsLayer {
							scaleX = closeScale
							scaleY = closeScale
						}
				) {
					Icon(
						painter = painterResource(R.drawable.clear),
						contentDescription = null
					)
				}
			}
		} else {
			CustomerCenter(onDismiss = onDismiss)
		}
	}
}