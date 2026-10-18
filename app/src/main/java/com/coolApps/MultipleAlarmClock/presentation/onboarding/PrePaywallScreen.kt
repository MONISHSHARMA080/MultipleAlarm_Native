package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct

/**
 * Gate screen shown only when the hard paywall feature flag is enabled,
 * before the actual [DisplaySate.OnboardingPaywall] step.
 *
 * Its job is to communicate (Cal AI style) that this is a Premium app,
 * while making the free trial the hero: "your trial is ready – start it now".
 * The only path forward is the trial CTA.
 *
 * [offering] is non-null on purpose: the caller only composes this screen once
 * a real `[Offering]` exists, so trial/price details are always derivable from it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HardPaywallEnabledScreen(
	offering: Offering,
	modifier: Modifier = Modifier,
	onContinue: () -> Unit,
	onButtonStateChange: (ButtonState) -> Unit,
) {
	logD("in the hard paywall screen")
	// Screen carries its own CTA, so hide the shared onboarding button.
	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Hidden)
	}

	// The offer copy straight from RevenueCat configured packages.
	val offerDetails = remember(offering) { extractTrialOfferDetails(offering) }

	// Entrance: hero scales in with a bounce, content fades with a slow effects spring.
	var appeared by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) { appeared = true }
	val heroScale by animateFloatAsState(
		targetValue = if (appeared) 1f else 0.6f,
		animationSpec = motionScheme.fastSpatialSpec(),
		label = "heroScale",
	)
	val contentAlpha by animateFloatAsState(
		targetValue = if (appeared) 1f else 0f,
		animationSpec = motionScheme.slowEffectsSpec(),
		label = "contentAlpha",
	)

	// CTA press: squash the corner radius (ExtraLarge -> Large) with a spatial spring,
	// expressive shape-morph on pressed state.
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val ctaCornerRadius by animateDpAsState(
		targetValue = if (isPressed) 16.dp else 28.dp,
		animationSpec = motionScheme.fastSpatialSpec(),
		label = "ctaCorner",
	)

	Box(
		modifier = modifier
			.fillMaxSize()
			.statusBarsPadding()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 24.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer { alpha = contentAlpha },
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Column(
				modifier = Modifier
					.widthIn(max = 560.dp)
					.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				// Hero icon in a vibrant circular container – premium/Bolt moment
				Box(
					modifier = Modifier
						.size(72.dp)
						.graphicsLayer {
							scaleX = heroScale
							scaleY = heroScale
						}
						.background(color = colorScheme.tertiaryContainer, shape = CircleShape),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						imageVector = Icons.Rounded.Bolt,
						contentDescription = null,
						tint = colorScheme.onTertiaryContainer,
					)
				}

				// Hero headline – heaviest weight + largest scale on screen
				Text(
					text = "Your free trial is ready", // TODO: extract to string resource
					textAlign = TextAlign.Center,
					style = typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onBackground,
				)

				Text(
					text = "This app is premium – and your access starts free. " +
						"Full features, zero cost while you decide.", // TODO: extract to string resource
					textAlign = TextAlign.Center,
					style = typography.bodyLarge,
					color = colorScheme.onBackground.copy(alpha = 0.7f),
				)

				// Hero moment #1: the trial offer card
				TrialOfferCard(
					trialHeadlineText = offerDetails.trialHeadline,
					priceNoteText = offerDetails.priceNote,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp)
				)

				// Hero moment #2: what the trial unlocks
//				TrialBenefitCard(
//					modifier = Modifier.fillMaxWidth()
//				)
			}
		}

		// CTA pinned bottom, mirrors the paywall layout for a continuous handoff
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.widthIn(max = 560.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(10.dp),
		) {
			Button(
				onClick = onContinue,
				interactionSource = interactionSource,
				shape = RoundedCornerShape(ctaCornerRadius),
				colors = ButtonDefaults.buttonColors(
					containerColor = colorScheme.primaryContainer,
					contentColor = colorScheme.onPrimaryContainer,
				),
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
			) {
				Text(
					text = "Start my free trial", // TODO: extract to string resource
					style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
				)
			}

			// Price footnote from RevenueCat: no commitment + real recurring terms
			Text(
				text = "No commitment. Cancel anytime. ${offerDetails.priceNote}", // TODO: extract to string resource
				textAlign = TextAlign.Center,
				style = typography.bodySmall,
				color = colorScheme.onBackground.copy(alpha = 0.5f),
			)
		}
	}
}

/* ------------------------------------------------------------------ */
/* Offering -> offer details                                           */
/* ------------------------------------------------------------------ */

/**
 * The pieces of RevenueCat config this screen renders.
 *
 * @param trialHeadline e.g. "3 days free"
 * @param priceNote e.g. "Then $6.99/week – billed only if you keep Premium."
 */
private data class TrialOfferDetails(
	val trialHeadline: String,
	val priceNote: String,
)

/**
 * Derives the trial headline + recurring price note from the offering's packages.
 *
 * Preference order: a package whose default subscription option includes a free
 * trial phase (FREE_TRIAL payment mode), then any package with a "free" phase,
 * then the first package as a graceful fallback (trial-less hard paywall).
 *
 * TODO: localize unit strings ("day(s)/week(s)…") once copy goes to string resources.
 */
private fun extractTrialOfferDetails(offering: Offering): TrialOfferDetails {
	val trialPhaseProduct = offering.availablePackages.firstOrNull { pkg ->
		pkg.product.freeTrialPhase() != null
	}

	val product: StoreProduct? =
		trialPhaseProduct?.product ?: offering.availablePackages.firstOrNull()?.product

	val freePhase = product?.freeTrialPhase()
	val pricePhase = product?.defaultOption?.fullPricePhase

	return TrialOfferDetails(
		trialHeadline = freePhase?.let { formatPeriod(it.billingPeriod) + " free" }
			?: "Free trial",
		priceNote = pricePhase?.let { phase ->
			"Then ${phase.price.formatted}/${periodShortUnit(phase.billingPeriod)} – " +
				"billed only if you keep Premium."
		}
			?: "Billed only if you keep Premium.",
	)
}

/**
 * The free-trial phase of the default subscription option, when the store
 * product actually ships a free trial.
 */
private fun StoreProduct.freeTrialPhase(): PricingPhase? {
	val default = defaultOption ?: return null
	return default.freePhase?.takeIf { it.offerPaymentMode == OfferPaymentMode.FREE_TRIAL }
}

/**
 * "3 days" / "1 week" / "1 month" — one-entry plural handling.
 */
private fun formatPeriod(period: Period): String {
	val unitName = when (period.unit) {
		Period.Unit.DAY -> if (period.value == 1) "day" else "days"
		Period.Unit.WEEK -> if (period.value == 1) "week" else "weeks"
		Period.Unit.MONTH -> if (period.value == 1) "month" else "months"
		Period.Unit.YEAR -> if (period.value == 1) "year" else "years"
		Period.Unit.UNKNOWN -> "period"
	}
	return "${period.value} $unitName"
}

/**
 * The unit portion of a recurring price label: "/ wk", "/ mo", "/ yr".
 */
private fun periodShortUnit(period: Period): String = when (period.unit) {
	Period.Unit.DAY -> "day"
	Period.Unit.WEEK -> "week"
	Period.Unit.MONTH -> "month"
	Period.Unit.YEAR -> "year"
	Period.Unit.UNKNOWN -> "period"
}

/* ------------------------------------------------------------------ */
/* UI blocks                                                           */
/* ------------------------------------------------------------------ */

/**
 * The offer card: states plainly that the app is premium while the trial makes
 * it free right now. Cal AI style: bold plan title, big trial line, quiet price note.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrialOfferCard(
	trialHeadlineText: String,
	priceNoteText: String,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(color = colorScheme.secondaryContainer, shape = shapes.extraExtraLarge)
			.padding(24.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(2.dp),
			) {
				Text(
					text = "PREMIUM", // TODO: extract to string resource
					style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
					color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
				)
				Text(
					text = trialHeadlineText, // e.g. "3 days free", from RevenueCat
					style = typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onSecondaryContainer,
				)
			}

			// Squarish badge next to the extra-round card for expressive shape contrast
			Box(
				modifier = Modifier
					.background(
						color = colorScheme.tertiaryContainer,
						shape = RoundedCornerShape(16.dp),
					)
					.padding(horizontal = 14.dp, vertical = 8.dp),
			) {
				Text(
					text = "Full access", // TODO: extract to string resource
					style = typography.labelLarge,
					color = colorScheme.onTertiaryContainer,
				)
			}
		}

		// Nested inner text: outer card 48dp -> inner corner 16dp keeps optical
		// roundness (outer - padding, never equal to the outer radius).
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(
					color = colorScheme.surface.copy(alpha = 0.4f),
					shape = RoundedCornerShape(16.dp),
				)
				.padding(horizontal = 16.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp),
		) {
			Icon(
				imageVector = Icons.Rounded.NotificationsActive,
				contentDescription = null,
				tint = colorScheme.onSecondaryContainer,
			)
			Text(
				text = priceNoteText, // e.g. "Then $6.99/week – billed only if you keep Premium."
				style = typography.bodyMedium,
				color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
			)
		}
	}
}

/**
 * What the trial unlocks, contained in a single tonal surface so the benefit
 * reads as one cohesive group, with a check-circle for instant scannability.
 */
//@Composable
//private fun TrialBenefitCard(modifier: Modifier = Modifier) {
//	Row(
//		modifier = modifier
//			.background(color = colorScheme.surfaceContainerLow, shape = shapes.extraLarge)
//			.padding(20.dp),
//		verticalAlignment = Alignment.CenterVertically,
//		horizontalArrangement = Arrangement.spacedBy(16.dp),
//	) {
//		Icon(
//			imageVector = Icons.Rounded.CheckCircle,
//			contentDescription = null,
//			tint = colorScheme.primary,
//		)
//		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//			Text(
//				text = "Unlimited alarms & wake-up ramps", // TODO: extract to string resource
//				style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//				color = colorScheme.onSurface,
//			)
//			Text(
//				text = "Gently pulled out of sleep with rising volume, every alarm protected.", // TODO: extract to string resource
//				style = typography.bodyMedium,
//				color = colorScheme.onSurface.copy(alpha = 0.6f),
//			)
//		}
//	}
//}
