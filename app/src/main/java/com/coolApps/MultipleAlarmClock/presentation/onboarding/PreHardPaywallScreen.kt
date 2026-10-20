package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

	val offerDetails = remember(offering) { extractTrialOfferDetails(offering) }

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

	val primary = colorScheme.primary
	val headline = remember(primary) {
		buildAnnotatedString {
			append("We want you to\ntry the app ")
			withStyle(SpanStyle(color = primary)) { append("for free") }
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
				text = "${offerDetails.trialHeadline} .  everything unlocked",
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
				text = "One setup. It keeps ringing every minute until you're actually up.",
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
						text = "No Payment Due Now",
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
						text = "Try the app for free",
						style = typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
					)
				}

				Text(
					text = buildString {
						append(offerDetails.trialHeadline)
						if (offerDetails.priceNote.isNotBlank()) {
							append(", then ")
							append(offerDetails.priceNote)
						}
						append(" Cancel anytime.")
					},
					textAlign = TextAlign.Center,
					style = typography.bodySmall,
					color = colorScheme.onBackground.copy(alpha = 0.5f),
				)
			}
		}
	}
}

private const val DEMO_TOTAL_ALARMS = 61 // 5:00 .. 6:00 inclusive
private val NOTIFICATION_HEIGHT = 88.dp
private val STACK_GAP = 16.dp
private val STACK_HEIGHT = 124.dp // NOTIFICATION_HEIGHT + 2 * STACK_GAP + breathing room

private fun demoTime(index: Int): String {
	val m = index % DEMO_TOTAL_ALARMS
	return if (m >= 60) "6:00" else "5:%02d".format(m)
}
/* The alarm as it appears in the app: ONE alarm, 5:00 -> 6:00, every 1 min */
@Composable
private fun RepeatAlarmCard() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(32.dp))
			.background(colorScheme.surfaceContainerHigh)
			.padding(horizontal = 20.dp, vertical = 18.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(6.dp),
		) {
			Row(
				modifier = Modifier
					.clip(CircleShape)
					.background(colorScheme.secondaryContainer)
					.padding(horizontal = 10.dp, vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(4.dp),
			) {
				Icon(
					imageVector = Icons.Rounded.Repeat,
					contentDescription = null,
					tint = colorScheme.onSecondaryContainer,
					modifier = Modifier.size(14.dp),
				)
				Text(
					text = "Repeat alarm",
					style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
					color = colorScheme.onSecondaryContainer,
				)
			}

			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = "5:00 ",
					style = typography.displaySmall.copy(
						fontWeight = FontWeight.ExtraBold,
//					fontFeatureSettings = "tnum",
					),
					color = colorScheme.onSurface,
				)
				Icon(
					imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
					contentDescription = null,
					tint = colorScheme.onSecondaryContainer,
				)

				Text(
					text = " 6:00 ",
					style = typography.displaySmall.copy(
						fontWeight = FontWeight.ExtraBold,
						fontFeatureSettings = "tnum",
					),
					color = colorScheme.onSurface,
				)
			}

			Text(
				text = "Every 1 min \u00B7 $DEMO_TOTAL_ALARMS alarms",
				style = typography.bodyMedium,
				color = colorScheme.onSurface.copy(alpha = 0.65f),
			)
		}

		Switch(checked = true, onCheckedChange = null)
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepeatAlarmDemoCard(modifier: Modifier = Modifier) {
	val stackPos = remember { Animatable(0f) } // continuous position in the stack
	val swipe = remember { Animatable(0f) }    // front card's swipe, in card widths
	val press = remember { Animatable(0f) }    // Stop pill press
	var front by remember { mutableIntStateOf(0) }

	val stackSpec = motionScheme.defaultSpatialSpec<Float>()

	LaunchedEffect(Unit) {
		delay(1800.milliseconds) // let the first alarm ring
		while (true) {
			// 1. tap Stop
			press.animateTo(1f, tween(110))
			delay(60.milliseconds)
			// 2. wind-up
			swipe.animateTo(-0.04f, tween(90, easing = FastOutSlowInEasing))
			// 3. flick away while the stack shifts forward
			coroutineScope {
				launch {
					swipe.animateTo(
						1.2f,
						tween(340, easing = CubicBezierEasing(0.5f, 0f, 1f, 0.6f)),
					)
				}
				launch {
					delay(120.milliseconds)
					stackPos.animateTo((front + 1).toFloat(), stackSpec)
				}
			}
			// 4. hand over to the next card
			front++
			swipe.snapTo(0f)
			press.snapTo(0f)
			delay(1800.milliseconds)
		}
	}

	val spin = rememberInfiniteTransition(label = "demoSpin").animateFloat(
		initialValue = 0f,
		targetValue = 360f,
		animationSpec = infiniteRepeatable(
			animation = tween(14000, easing = LinearEasing),
		),
		label = "spin",
	)

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		RepeatAlarmCard()
		NotificationStack(
			front = front,
			stackPos = stackPos.asState(),
			swipe = swipe.asState(),
			press = press.asState(),
			spin = spin,
		)
	}
}

@Composable
private fun NotificationStack(
		front: Int,
		stackPos: State<Float>,
		swipe: State<Float>,
		press: State<Float>,
		spin: State<Float>,
) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(STACK_HEIGHT),
	) {
		// Back to front. front+3 is the card that is about to arrive.
		for (index in (front + 3) downTo front) {
			key(index) {
				StackedNotification(
					index = index,
					front = front,
					stackPos = stackPos,
					swipe = swipe,
					press = press,
					spin = spin,
				)
			}
		}
	}
}

@Composable
private fun StackedNotification(
		index: Int,
		front: Int,
		stackPos: State<Float>,
		swipe: State<Float>,
		press: State<Float>,
		spin: State<Float>,
) {
	val shape = RoundedCornerShape(28.dp)
	val frontColor = colorScheme.primaryContainer
	val backColor = colorScheme.surfaceContainerHighest
	val isFront = index == front

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(NOTIFICATION_HEIGHT)
			.graphicsLayer {
				// Everything derives from ONE number: the card's slot in the stack.
				val d = index - stackPos.value
				val behind = d.coerceIn(0f, 2f)
				val arriving = (d - 2f).coerceIn(0f, 1f) // 1 = just spawned, 0 = in place

				val sx = when {
					index < front -> 1.2f
					index == front -> swipe.value
					else -> 0f
				}

				val scale = (1f - 0.05f * behind) * (1f - 0.08f * arriving)
				scaleX = scale
				scaleY = scale
				transformOrigin = TransformOrigin(0.5f, 0f)
				translationY = behind * STACK_GAP.toPx() + arriving * 12.dp.toPx()

				translationX = sx * size.width
				rotationZ = sx * 7f

				val exitAlpha = 1f - ((sx - 0.3f) / 0.6f).coerceIn(0f, 1f)
				alpha = (1f - arriving) * exitAlpha

				this.shape = shape
				clip = true
				shadowElevation = (1f - behind / 2f).coerceIn(0f, 1f) * 6.dp.toPx()
			}
			.drawBehind {
				val d = index - stackPos.value
				val t = (d.coerceIn(0f, 2f) / 2f) * 0.7f
				drawRect(lerp(frontColor, backColor, t))
			},
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer {
					alpha = (1f - (index - stackPos.value)).coerceIn(0f, 1f)
				},
		) {
			RingingContent(
				index = index,
				isFront = isFront,
				stopPress = if (isFront) press.value else 0f,
				spin = spin,
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingingContent(
		index: Int,
		isFront: Boolean,
		stopPress: Float,
		spin: State<Float>,
) {
	val rotation = remember { Animatable(0f) }
	val pulse = remember { Animatable(0f) }

	// Bell: a short burst once the card has settled at the front, then rest.
	LaunchedEffect(isFront) {
		if (isFront) {
			delay(150.milliseconds)
			repeat(5) {
				rotation.animateTo(14f, tween(60))
				rotation.animateTo(-14f, tween(60))
			}
			rotation.animateTo(0f, tween(80))
		} else {
			rotation.snapTo(0f)
		}
	}

	// Ripple rings: two expanding pulses in sync with the bell.
	LaunchedEffect(isFront) {
		if (isFront) {
			delay(150.milliseconds)
			repeat(2) {
				pulse.snapTo(0.001f)
				pulse.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
			}
			pulse.snapTo(0f)
		} else {
			pulse.snapTo(0f)
		}
	}

	val cookie = MaterialShapes.Cookie9Sided.toShape()
	val ringColor = colorScheme.onPrimaryContainer

	Row(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 14.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Box(
			modifier = Modifier
				.size(48.dp)
				.drawBehind {
					val p = pulse.value
					if (p > 0f) {
						drawCircle(
							color = ringColor.copy(alpha = 0.35f * (1f - p)),
							radius = size.minDimension / 2f * (1f + 0.7f * p),
							style = Stroke(width = 2.dp.toPx()),
						)
					}
				},
			contentAlignment = Alignment.Center,
		) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.graphicsLayer { rotationZ = spin.value }
					.background(colorScheme.onPrimaryContainer.copy(alpha = 0.2f), cookie),
			)
			Icon(
				imageVector = Icons.Rounded.Alarm,
				contentDescription = null,
				tint = colorScheme.onPrimaryContainer,
				modifier = Modifier
					.size(24.dp)
					.graphicsLayer { rotationZ = rotation.value },
			)
		}

		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(1.dp),
		) {
			Text(
				text = "Alarm \u00B7 wake up",
				style = typography.labelMedium,
				color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
			)
			Text(
				text = demoTime(index),
				style = typography.headlineSmall.copy(
					fontWeight = FontWeight.ExtraBold,
					fontFeatureSettings = "tnum",
				),
				color = colorScheme.onPrimaryContainer,
			)
			Text(
				text = "Repeat ${index % DEMO_TOTAL_ALARMS + 1} of $DEMO_TOTAL_ALARMS",
				style = typography.bodySmall,
				color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
			)
		}

		StopPill(press = stopPress)
	}
}

@Composable
private fun StopPill(press: Float) {
	Text(
		text = "Stop",
		style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
		color = lerp(colorScheme.onSecondaryContainer, colorScheme.onPrimary, press),
		modifier = Modifier
			.graphicsLayer {
				val s = 1f - 0.12f * press
				scaleX = s
				scaleY = s
			}
			.clip(CircleShape)
			.background(lerp(colorScheme.secondaryContainer, colorScheme.primary, press))
			.padding(horizontal = 18.dp, vertical = 10.dp),
	)
}

/* ------------------------------------------------------------------ */
/* Offering -> offer details                                           */
/* ------------------------------------------------------------------ */

private data class TrialOfferDetails(
		val trialHeadline: String,
		val priceNote: String,
)

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
			"${phase.price.formatted}/${periodShortUnit(phase.billingPeriod)}."
		} ?: "",
	)
}

private fun StoreProduct.freeTrialPhase(): PricingPhase? {
	val default = defaultOption ?: return null
	return default.freePhase?.takeIf { it.offerPaymentMode == OfferPaymentMode.FREE_TRIAL }
}

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

private fun periodShortUnit(period: Period): String = when (period.unit) {
	Period.Unit.DAY -> "day"
	Period.Unit.WEEK -> "week"
	Period.Unit.MONTH -> "month"
	Period.Unit.YEAR -> "year"
	Period.Unit.UNKNOWN -> "period"
}
