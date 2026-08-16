package MultipleAlarmClock.alarmFeature.ui.onboarding.components

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_VISIBLE_TIMELINE_ROWS = 6
private val TIMELINE_ROW_STAGGER_MS = 90.milliseconds

// Note: Replace with your actual AlarmData class and logD implementation
// data class AlarmData(val startTime: Long, val endTime: Long, val frequencyInMin: Long)
// fun logD(msg: String) { println(msg) }

@Composable
fun AlarmResultClaude(
	alarmData: AlarmData?,
	onNextClick: () -> Unit
) {
	if (alarmData == null) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			CircularProgressIndicator()
		}
	} else {
		AlarmResultContent(
			alarmData = alarmData,
			onNextClick = onNextClick
		)
	}
}

@Composable
private fun AlarmResultContent(
	alarmData: AlarmData,
	onNextClick: () -> Unit
) {
	val zoneId = remember { ZoneId.systemDefault() }
	val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

	var notificationTimes by remember { mutableStateOf<List<Long>>(emptyList()) }
	var expanded by rememberSaveable { mutableStateOf(false) }

	// States for our 2-step animation
	var iconAnimationDone by remember { mutableStateOf(false) }
	var isSettled by remember { mutableStateOf(false) }
	var visibleRows by remember { mutableIntStateOf(0) }

	val haptic = LocalHapticFeedback.current
	val configuration = LocalConfiguration.current

	LaunchedEffect(alarmData) {
		notificationTimes = withContext(Dispatchers.Default) {
			computeNotificationTimes(alarmData)
		}
	}

	// Triggered after the custom tick icon finishes its drawing animation
	LaunchedEffect(iconAnimationDone) {
		if (iconAnimationDone) {
			haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			delay(400) // Brief pause before snapping to settled state
			isSettled = true

			// Wait for the layout to slide up before showing rows
			delay(350.milliseconds)

			val initialRowCount = minOf(notificationTimes.size, MAX_VISIBLE_TIMELINE_ROWS)
			repeat(initialRowCount) { index ->
				visibleRows = index + 1
				delay(TIMELINE_ROW_STAGGER_MS)
			}

			haptic.performHapticFeedback(HapticFeedbackType.LongPress)
		}
	}

	val visibleTimes = remember(expanded, notificationTimes) {
		if (expanded) notificationTimes else notificationTimes.take(MAX_VISIBLE_TIMELINE_ROWS)
	}

	Scaffold(
		bottomBar = {
			// Fade in bottom bar only when settled
			AnimatedVisibility(
				visible = isSettled,
				enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
			) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(colorScheme.background)
						.navigationBarsPadding()
						.padding(horizontal = 26.dp, vertical = 20.dp),
					contentAlignment = Alignment.Center
				) {
					Button(
						onClick = onNextClick,
						modifier = Modifier
							.fillMaxWidth()
							.height(56.dp),
						shape = shapes.extraLarge,
						colors = ButtonDefaults.buttonColors(
							containerColor = colorScheme.primaryContainer,
							contentColor = colorScheme.onPrimaryContainer
						)
					) {
						Text(
							text = stringResource(R.string.onboarding_result_finish),
							style = typography.titleMedium
						)
					}
				}
			}
		}
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(horizontal = 24.dp),
			horizontalAlignment = Alignment.Start
		) {
			item(key = "header_title") {
				// Dynamic spacer pushes content to the middle initially, and slides to top when settled
				val initialSpacerHeight =  (LocalWindowInfo.current.containerDpSize.height.value * 0.35f).dp
				val topSpacerHeight by animateDpAsState(
					targetValue = if (isSettled) 32.dp else initialSpacerHeight,
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioNoBouncy,
						stiffness = Spring.StiffnessLow
					),
					label = "spacer_anim"
				)

				Spacer(modifier = Modifier.height(topSpacerHeight))

				Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = Alignment.Center
				) {
					// Shrink and remove the icon when settling
					AnimatedVisibility(
						visible = !isSettled,
						exit = shrinkVertically(
							animationSpec = tween(900, easing = FastOutSlowInEasing)
						) + fadeOut() + scaleOut(targetScale = 0.5f)
					) {
						Column {
							AnimatedSuccessIcon(
								onAnimationComplete = { iconAnimationDone = true }
							)
							Spacer(modifier = Modifier.height(20.dp))
						}
					}
				}

				Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = Alignment.Center
				) {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = stringResource(R.string.onboarding_result_title),
							style = typography.headlineLarge,
							fontWeight = FontWeight.SemiBold,
							color = colorScheme.onBackground,
							textAlign = TextAlign.Center
						)
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = stringResource(R.string.onboarding_result_subtitle),
							style = typography.bodyMedium,
							color = colorScheme.onBackground.copy(alpha = 0.72f),
							textAlign = TextAlign.Center
						)
					}
				}

				Spacer(modifier = Modifier.height(28.dp))

				// Show Card when settled
				AnimatedVisibility(
					visible = isSettled,
					enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 40 })
				) {
					Column {
						AlarmCard(
							startTime = alarmData.startTime,
							endTime = alarmData.endTime,
							frequencyInMin = alarmData.frequencyInMin,
							formatter = timeFormatter,
							zoneId = zoneId
						)
						Spacer(modifier = Modifier.height(24.dp))
						Text(
							text = stringResource(R.string.onboarding_result_ring_on),
							style = typography.titleSmall,
							fontWeight = FontWeight.Normal,
							color = colorScheme.onSurface.copy(alpha = 0.67f)
						)
						Spacer(modifier = Modifier.height(12.dp))
					}
				}
			}

			/* TIMELINE ROWS */
			itemsIndexed(
				items = visibleTimes,
				key = { index, time -> "$time-$index" }
			) { index, time ->
				TimelineRow(
					time = time,
					formatter = timeFormatter,
					zoneId = zoneId,
					visible = expanded || (isSettled && index < visibleRows),
					isLast = index == visibleTimes.lastIndex
				)
			}

			if (notificationTimes.size > MAX_VISIBLE_TIMELINE_ROWS) {
				item(key = "expand_button") {
					AnimatedVisibility(visible = isSettled && visibleRows >= MAX_VISIBLE_TIMELINE_ROWS) {
						ExpandableMoreRow(
							hiddenCount = notificationTimes.size - MAX_VISIBLE_TIMELINE_ROWS,
							expanded = expanded,
							onToggle = { expanded = !expanded }
						)
					}
				}
			}

			item(key = "bottom_spacer") {
				Spacer(modifier = Modifier.height(24.dp))
			}
		}
	}
}

@Composable
private fun AnimatedSuccessIcon(
	onAnimationComplete: () -> Unit
) {
	val circleProgress = remember { Animatable(0f) }
	val checkProgress = remember { Animatable(0f) }
	val pathMeasure = remember { PathMeasure() }
	val primaryColor = colorScheme.primary
	val containerColor = colorScheme.primaryContainer

	LaunchedEffect(Unit) {
		// 1. Draw the circle
		circleProgress.animateTo(
			targetValue = 1f,
			animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
		)
		// 2. Draw the tick inside
		checkProgress.animateTo(
			targetValue = 1f,
			animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
		)
		onAnimationComplete()
	}

	Surface(
		modifier = Modifier.size(80.dp),
		shape = CircleShape,
		color = containerColor.copy(alpha = 0.5f)
	) {
		Canvas(modifier = Modifier
			.fillMaxSize()
			.padding(16.dp)) {

			val strokeWidth = 5.dp.toPx()

			// Draw animated Circle
			drawArc(
				color = primaryColor,
				startAngle = -90f,
				sweepAngle = 360f * circleProgress.value,
				useCenter = false,
				style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
			)

			// Draw animated Checkmark
			if (checkProgress.value > 0f) {
				val checkPath = Path().apply {
					val width = size.width
					val height = size.height
					moveTo(width * 0.25f, height * 0.52f)
					lineTo(width * 0.43f, height * 0.70f)
					lineTo(width * 0.75f, height * 0.35f)
				}

				pathMeasure.setPath(checkPath, false)
				val drawnPath = Path()
				pathMeasure.getSegment(
					startDistance = 0f,
					stopDistance = pathMeasure.length * checkProgress.value,
					destination = drawnPath,
					startWithMoveTo = true
				)

				drawPath(
					path = drawnPath,
					color = primaryColor,
					style = Stroke(
						width = strokeWidth,
						cap = StrokeCap.Round,
						join = StrokeJoin.Round
					)
				)
			}
		}
	}
}


@Composable
private fun AlarmCard(
	startTime: Long,
	endTime: Long,
	frequencyInMin: Long,
	formatter: DateTimeFormatter,
	zoneId: ZoneId
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = shapes.extraLarge,
		color = colorScheme.surfaceContainer
	) {
		Row(
			modifier = Modifier.padding(20.dp).fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				modifier = Modifier.size(48.dp),
				shape = shapes.large,
				color = colorScheme.primaryContainer
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = Icons.Outlined.Alarm,
						contentDescription = null,
						tint = colorScheme.onPrimaryContainer,
						modifier = Modifier.size(25.dp)
					)
				}
			}

			Spacer(modifier = Modifier.width(14.dp))

			Column {
				Text(
					text = stringResource(
						R.string.onboarding_result_time_range,
						formatEpochMillis(startTime, formatter, zoneId),
						formatEpochMillis(endTime, formatter, zoneId)
					),
					style = typography.titleMedium,
					fontWeight = FontWeight.Normal,
					color = colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(2.dp))
				Text(
					text = stringResource(R.string.onboarding_result_every_min, frequencyInMin),
					style = typography.bodySmall,
					color = colorScheme.onSurface.copy(alpha = 0.62f)
				)
			}
		}
	}
}

@Composable
private fun TimelineRow(
	time: Long,
	formatter: DateTimeFormatter,
	zoneId: ZoneId,
	visible: Boolean,
	isLast: Boolean
) {
	val formattedTime = remember(time, formatter, zoneId) {
		formatEpochMillis(time, formatter, zoneId)
	}

	val entrance by animateFloatAsState(
		targetValue = if (visible) 1f else 0f,
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessLow
		),
		label = "row_entrance"
	)

	val lineScale = remember { Animatable(0f) }
	LaunchedEffect(visible) {
		if (visible && !isLast) {
			delay(120)
			lineScale.animateTo(1f, animationSpec = tween(250, easing = FastOutSlowInEasing))
		}
	}

	Row(
		modifier = Modifier
			.height(IntrinsicSize.Min)
			.graphicsLayer {
				alpha = entrance
				translationX = (1f - entrance) * -24f
			}
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.width(20.dp).fillMaxHeight()
		) {
			Box(
				modifier = Modifier.padding(top = 6.dp).size(8.dp)
					.background(colorScheme.primary, CircleShape)
			)
			if (!isLast) {
				Box(
					modifier = Modifier.width(1.dp).weight(1f)
						.graphicsLayer {
							scaleY = lineScale.value
							transformOrigin = TransformOrigin(0.5f, 0f)
						}
						.background(colorScheme.outlineVariant)
				)
			}
		}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.padding(bottom = 16.dp)) {
			Text(
				text = formattedTime,
				style = typography.titleMedium,
				color = colorScheme.onBackground
			)
		}
	}
}

@Composable
private fun ExpandableMoreRow(
	hiddenCount: Int,
	expanded: Boolean,
	onToggle: () -> Unit
) {
	val chevronRotation by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(180),
		label = "chevron_rotation"
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shapes.large)
			.clickable(onClick = onToggle)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier.padding(start = 6.dp, end = 12.dp).size(8.dp)
				.border(1.dp, colorScheme.outline, CircleShape)
		)
		Text(
			text = if (expanded) {
				stringResource(R.string.onboarding_result_show_less)
			} else {
				stringResource(R.string.onboarding_result_show_more, hiddenCount)
			},
			style = typography.bodySmall,
			color = colorScheme.onBackground.copy(alpha = 0.7f)
		)
		Spacer(Modifier.size(8.dp))
		Icon(
			imageVector = Icons.Default.KeyboardArrowDown,
			contentDescription = null,
			tint = colorScheme.onBackground.copy(alpha = 0.6f),
			modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = chevronRotation }
		)
	}
}

private fun computeNotificationTimes(alarmData: AlarmData): List<Long> {
	val intervalMillis = alarmData.frequencyInMin.coerceAtLeast(1) * 60_000L
	if (alarmData.endTime <= alarmData.startTime) return listOf(alarmData.startTime)

	val times = mutableListOf<Long>()
	var current = alarmData.startTime
	while (current <= alarmData.endTime) {
		times.add(current)
		current += intervalMillis
	}
	return times
}

private fun formatEpochMillis(millis: Long, formatter: DateTimeFormatter, zoneId: ZoneId): String {
	return Instant.ofEpochMilli(millis).atZone(zoneId).format(formatter).lowercase()
}
