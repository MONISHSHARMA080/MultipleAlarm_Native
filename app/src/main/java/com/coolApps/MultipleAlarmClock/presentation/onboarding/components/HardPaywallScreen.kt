package com.coolApps.MultipleAlarmClock.presentation.onboarding.components

import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.util.extractTrialOfferDetails
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.Period
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HardPaywallScreen(
		offering: Offering,
		modifier: Modifier = Modifier,
		onContinue: () -> Unit,
		onButtonStateChange: (ButtonState) -> Unit,
) {
	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Hidden)
	}

	val offerDetails = extractTrialOfferDetails(offering)

	val trialHeadline = offerDetails.trialPeriod
		?.let { stringResource(R.string.paywall_trial_headline, formatPeriod(it)) }
		?: stringResource(R.string.paywall_free_trial_default)

	val priceNote = if (offerDetails.fullPrice != null && offerDetails.fullPriceUnit != null) {
		stringResource(
			R.string.paywall_price_note,
			offerDetails.fullPrice,
			periodShortUnit(offerDetails.fullPriceUnit),
		)
	} else {
		""
	}

	var showHeadline by remember { mutableStateOf(false) }
	var showSubtitle by remember { mutableStateOf(false) }
	var showHero by remember { mutableStateOf(false) }
	var showCta by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		showHeadline = true
		delay(350.milliseconds)
		showSubtitle = true
		delay(300.milliseconds)
		showHero = true
		delay(350.milliseconds)
		showCta = true
	}

	val enterReveal = fadeIn(
		animationSpec = motionScheme.slowEffectsSpec()
	) + slideInVertically(
		animationSpec = motionScheme.slowSpatialSpec(),
		initialOffsetY = { 40 }
	) + scaleIn(
		initialScale = 0.95f,
		animationSpec = motionScheme.slowSpatialSpec()
	)

	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val ctaCornerRadius by animateDpAsState(
		targetValue = if (isPressed) 18.dp else 36.dp,
		animationSpec = motionScheme.fastSpatialSpec(),
		label = "ctaCorner",
	)

	val headline = buildAnnotatedString {
		append(stringResource(R.string.paywall_headline_plain))
		withStyle(SpanStyle(color = colorScheme.primary)) {
			append(stringResource(R.string.paywall_headline_highlight))
		}
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.statusBarsPadding()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Spacer(Modifier.height(16.dp))

		AnimatedVisibility(visible = showHeadline, enter = enterReveal) {
			Text(
				text = headline,
				textAlign = TextAlign.Center,
				style = typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
				color = colorScheme.onBackground,
			)
		}

		Spacer(Modifier.height(10.dp))

		AnimatedVisibility(visible = showSubtitle, enter = enterReveal) {
			Text(
				text = stringResource(R.string.paywall_subtitle, trialHeadline),
				textAlign = TextAlign.Center,
				style = typography.titleMedium,
				color = colorScheme.onBackground.copy(alpha = 0.6f),
			)
		}

		Spacer(Modifier.weight(1f))

		AnimatedVisibility(
			visible = showHero,
			enter = enterReveal,
			modifier = Modifier.widthIn(max = 480.dp),
		) {
			RepeatAlarmDemoCard(modifier = Modifier.fillMaxWidth())
		}

		Spacer(Modifier.height(14.dp))

		AnimatedVisibility(visible = showHero, enter = enterReveal) {
			Text(
				text = stringResource(R.string.paywall_demo_caption),
				textAlign = TextAlign.Center,
				style = typography.bodyMedium,
				color = colorScheme.onBackground.copy(alpha = 0.65f),
				modifier = Modifier.widthIn(max = 340.dp),
			)
		}

		Spacer(Modifier.weight(1f))

		AnimatedVisibility(visible = showCta, enter = enterReveal) {
			Column(
				modifier = Modifier
					.widthIn(max = 560.dp)
					.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp),
				) {
					Icon(
						imageVector = Icons.Rounded.Check,
						contentDescription = null,
						tint = colorScheme.onBackground,
						modifier = Modifier.size(20.dp),
					)
					Text(
						text = stringResource(R.string.paywall_no_payment_due),
						style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
						color = colorScheme.onBackground,
					)
				}

				Button(
					onClick = onContinue,
					interactionSource = interactionSource,
					shape = RoundedCornerShape(ctaCornerRadius),
					colors = ButtonDefaults.buttonColors(
						containerColor = colorScheme.primary,
						contentColor = colorScheme.onPrimary,
					),
					modifier = Modifier
						.fillMaxWidth()
						.height(72.dp),
				) {
					Text(
						text = stringResource(R.string.paywall_cta_try_free),
						style = typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
					)
				}

				Text(
					text = if (priceNote.isNotBlank()) {
						stringResource(R.string.paywall_fine_print_with_price, trialHeadline, priceNote)
					} else {
						stringResource(R.string.paywall_fine_print_no_price, trialHeadline)
					},
					textAlign = TextAlign.Center,
					style = typography.bodySmall,
					color = colorScheme.onBackground.copy(alpha = 0.5f),
				)
			}
		}
	}
}

@Composable
private fun formatPeriod(period: Period): String {
	val unitName = when (period.unit) {
		Period.Unit.DAY -> if (period.value == 1) stringResource(R.string.paywall_period_day) else stringResource(R.string.paywall_period_days)
		Period.Unit.WEEK -> if (period.value == 1) stringResource(R.string.paywall_period_week) else stringResource(R.string.paywall_period_weeks)
		Period.Unit.MONTH -> if (period.value == 1) stringResource(R.string.paywall_period_month) else stringResource(R.string.paywall_period_months)
		Period.Unit.YEAR -> if (period.value == 1) stringResource(R.string.paywall_period_year) else stringResource(R.string.paywall_period_years)
		Period.Unit.UNKNOWN -> stringResource(R.string.paywall_period_unknown)
	}
	return stringResource(R.string.paywall_period_value_format, period.value, unitName)
}

@Composable
private fun periodShortUnit(period: Period): String = when (period.unit) {
	Period.Unit.DAY -> stringResource(R.string.paywall_period_day)
	Period.Unit.WEEK -> stringResource(R.string.paywall_period_week)
	Period.Unit.MONTH -> stringResource(R.string.paywall_period_month)
	Period.Unit.YEAR -> stringResource(R.string.paywall_period_year)
	Period.Unit.UNKNOWN -> stringResource(R.string.paywall_period_unknown)
}
