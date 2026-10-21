package com.coolApps.MultipleAlarmClock.presentation.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Alarm
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DEMO_TOTAL_ALARMS = 61 // 5:00 .. 6:00 inclusive
private val NOTIFICATION_HEIGHT = 88.dp
private val STACK_GAP = 16.dp
private val STACK_HEIGHT = 124.dp // NOTIFICATION_HEIGHT + 2 * STACK_GAP + breathing room

private fun demoTime(index: Int, endTime: String): String {
	val m = index % DEMO_TOTAL_ALARMS
	return if (m >= 60) endTime else "5:%02d".format(m)
}

/* The alarm as it appears in the app: ONE alarm, 5:00 -> 6:00, every 1 min */
@Composable
internal fun RepeatAlarmCard() {
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
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = stringResource(R.string.paywall_demo_start_time) + " ",
					style = typography.displaySmall.copy(
						fontWeight = FontWeight.ExtraBold,
					),
					color = colorScheme.onSurface,
				)
				Icon(
					imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
					contentDescription = null,
					tint = colorScheme.onSecondaryContainer,
				)

				Text(
					text = " " + stringResource(R.string.paywall_demo_end_time) + " ",
					style = typography.displaySmall.copy(
						fontWeight = FontWeight.ExtraBold,
						fontFeatureSettings = "tnum",
					),
					color = colorScheme.onSurface,
				)
			}

			Text(
				text = stringResource(R.string.paywall_demo_every_min, DEMO_TOTAL_ALARMS),
				style = typography.bodyMedium,
				color = colorScheme.onSurface.copy(alpha = 0.65f),
			)
		}

		Switch(checked = true, onCheckedChange = null)
	}
}

@Composable
internal fun RepeatAlarmDemoCard(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(14.dp),
	) {
		RepeatAlarmCard()
		RingingAlarmStack()
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RingingAlarmStack(
	onAlarmDismissed: (Int) -> Unit = {},
	modifier: Modifier = Modifier,
) {
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
			onAlarmDismissed(front - 1)
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

	NotificationStack(
		front = front,
		stackPos = stackPos.asState(),
		swipe = swipe.asState(),
		press = press.asState(),
		spin = spin,
		modifier = modifier,
	)
}

@Composable
private fun NotificationStack(
		front: Int,
		stackPos: State<Float>,
		swipe: State<Float>,
		press: State<Float>,
		spin: State<Float>,
		modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
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
	val endTime = stringResource(R.string.paywall_demo_end_time)

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
				text = stringResource(R.string.paywall_demo_notification_app),
				style = typography.labelMedium,
				color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
			)
			Text(
				text = demoTime(index, endTime),
				style = typography.headlineSmall.copy(
					fontWeight = FontWeight.ExtraBold,
					fontFeatureSettings = "tnum",
				),
				color = colorScheme.onPrimaryContainer,
			)
			Text(
				text = stringResource(
					R.string.paywall_demo_repeat_count,
					index % DEMO_TOTAL_ALARMS + 1,
					DEMO_TOTAL_ALARMS,
				),
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
		text = stringResource(R.string.paywall_demo_stop),
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
