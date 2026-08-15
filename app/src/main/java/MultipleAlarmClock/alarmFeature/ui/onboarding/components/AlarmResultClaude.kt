package MultipleAlarmClock.alarmFeature.ui.onboarding.components

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds


private const val MAX_VISIBLE_TIMELINE_ROWS = 6
private val TIMELINE_ROW_STAGGER_MS = 90.milliseconds

@Composable
fun AlarmResultClaude(
	alarmData: AlarmData?,
	onNextClick: () -> Unit
) {
	logD("alarmData is $alarmData")

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
	val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

	var notificationTimes by remember {
		mutableStateOf<List<Long>>(emptyList())
	}

	var expanded by rememberSaveable {
		mutableStateOf(false)
	}

	var checkVisible by remember {
		mutableStateOf(false)
	}

	var headerVisible by remember {
		mutableStateOf(false)
	}

	var cardVisible by remember {
		mutableStateOf(false)
	}

	val haptic = LocalHapticFeedback.current

	LaunchedEffect(alarmData) {
		// 1. Move computation to background thread
		notificationTimes = withContext(Dispatchers.Default) {
			computeNotificationTimes(alarmData)
		}

		// 2. Run sequential entrance animations after state updates
		checkVisible = true

		haptic.performHapticFeedback(
			HapticFeedbackType.LongPress
		)

		delay(250.milliseconds)

		headerVisible = true

		delay(150.milliseconds)

		cardVisible = true

		delay(200.milliseconds)

		// 3. Stagger initial timeline rows
		repeat(
			minOf(
				notificationTimes.size,
				MAX_VISIBLE_TIMELINE_ROWS
			)
		) {
			delay(TIMELINE_ROW_STAGGER_MS)
		}

		haptic.performHapticFeedback(
			HapticFeedbackType.LongPress
		)
	}

	Scaffold(
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(colorScheme.background)
					.navigationBarsPadding()
					.padding(26.dp)
					.padding(bottom = 20.dp),
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
						text = "Finish",
						style = typography.titleMedium
					)
				}
			}
		}
	) { padding ->

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {

			Spacer(
				modifier = Modifier.height(32.dp)
			)

			/*
			 * SUCCESS ICON
			 */
			AnimatedVisibility(
				visible = headerVisible,
				enter = scaleIn(
					initialScale = 0.75f,
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioMediumBouncy,
						stiffness = Spring.StiffnessMedium
					)
				) + fadeIn(
					animationSpec = tween(250)
				)
			) {
				SuccessIcon()
			}

			Spacer(
				modifier = Modifier.height(20.dp)
			)

			/*
			 * TITLE
			 */
			AnimatedVisibility(
				visible = headerVisible,
				enter = fadeIn(
					animationSpec = tween(
						durationMillis = 350,
						delayMillis = 100
					)
				) + slideInVertically(
					initialOffsetY = { 12 },
					animationSpec = tween(350)
				)
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text(
						text = "You're all set!",
						style = typography.headlineLarge,
						fontWeight = FontWeight.SemiBold,
						color = colorScheme.onBackground,
						textAlign = TextAlign.Center
					)

					Spacer(
						modifier = Modifier.height(4.dp)
					)

					Text(
						text = "Your alarm was scheduled successfully",
						style = typography.bodyMedium,
						color = colorScheme.onBackground.copy(alpha = 0.72f),
						textAlign = TextAlign.Center
					)
				}
			}

			Spacer(
				modifier = Modifier.height(28.dp)
			)

			AlarmCard(
				alarmData.startTime,
				alarmData.endTime,
				alarmData.frequencyInMin
			)

			Spacer(
				modifier = Modifier.height(24.dp)
			)

			Column(
				horizontalAlignment = Alignment.Start
			) {
				Text(
					text = "Alarms would be sent on",
					style = typography.titleSmall,
					fontWeight = FontWeight.Normal,
					color = colorScheme.onSurface.copy(alpha = 0.72f)
				)

				Spacer(modifier = Modifier.height(8.dp))

				val visibleTimes = if (expanded) {
					notificationTimes
				} else {
					notificationTimes.take(MAX_VISIBLE_TIMELINE_ROWS)
				}

				visibleTimes.forEachIndexed { index, time ->
					key(time) {
						TimelineRow(
							time = time,
							formatter = timeFormatter,
							zoneId = zoneId,
							visible = true,
							isLast = index == visibleTimes.lastIndex
						)
					}
				}

				if (notificationTimes.size > MAX_VISIBLE_TIMELINE_ROWS) {
					ExpandableMoreRow(
						hiddenCount = notificationTimes.size - MAX_VISIBLE_TIMELINE_ROWS,
						expanded = expanded,
						visible = true,
						onToggle = {
							expanded = !expanded
						}
					)
				}
			}

			Spacer(
				modifier = Modifier.height(24.dp)
			)
		}
	}
}

@Composable
private fun AlarmCard(
	startTime: Long,
	endTime: Long,
	frequencyInMin: Long
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = shapes.extraLarge,
		color = colorScheme.surfaceContainer
	) {
		Column(
			modifier = Modifier.padding(20.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Surface(
					modifier = Modifier.size(48.dp),
					shape = shapes.large,
					color = colorScheme.primaryContainer
				) {
					Box(
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.Outlined.Alarm,
							contentDescription = null,
							tint = colorScheme.onPrimaryContainer,
							modifier = Modifier.size(25.dp)
						)
					}
				}

				Spacer(
					modifier = Modifier.width(14.dp)
				)

				Column {
					Text(
						text = "${startTime.formatAlarmTime()} ? ${endTime.formatAlarmTime()}",
						style = typography.titleMedium,
						fontWeight = FontWeight.Normal,
						color = colorScheme.onSurface
					)

					Spacer(
						modifier = Modifier.height(2.dp)
					)

					Text(
						text = "Every $frequencyInMin minutes",
						style = typography.bodySmall,
						color = colorScheme.onSurface.copy(alpha = 0.62f)
					)
				}
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
			modifier = Modifier
				.width(20.dp)
				.fillMaxHeight()
		) {
			Box(
				modifier = Modifier
					.padding(top = 6.dp)
					.size(8.dp)
					.background(colorScheme.primary, CircleShape)
			)
			if (!isLast) {
				Box(
					modifier = Modifier
						.width(1.dp)
						.weight(1f)
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
				text = formatEpochMillis(time, formatter, zoneId),
				style = typography.titleMedium,
				color = colorScheme.onBackground
			)
		}
	}
}


//
//@Composable
//private fun TimelineRow(
//	time: Long,
//	formatter: DateTimeFormatter,
//	zoneId: ZoneId,
//	visible: Boolean,
//	isLast: Boolean
//) {
//	val entrance by animateFloatAsState(
//		targetValue = if (visible) 1f else 0f,
//		animationSpec = spring(
//			dampingRatio = Spring.DampingRatioMediumBouncy,
//			stiffness = Spring.StiffnessLow
//		),
//		label = "timeline_row_entrance"
//	)
//
//	val lineScale by animateFloatAsState(
//		targetValue = if (visible && !isLast) 1f else 0f,
//		animationSpec = tween(
//			durationMillis = 250,
//			easing = FastOutSlowInEasing
//		),
//		label = "timeline_line"
//	)
//
//	Row(
//		modifier = Modifier
//			.height(IntrinsicSize.Min)
//			.graphicsLayer {
//				alpha = entrance
//				translationX = (1f - entrance) * -24f
//			}
//	) {
//		Column(
//			horizontalAlignment = Alignment.CenterHorizontally,
//			modifier = Modifier
//				.width(20.dp)
//				.fillMaxHeight()
//		) {
//			Box(
//				modifier = Modifier
//					.padding(top = 6.dp)
//					.size(8.dp)
//					.background(
//						color = colorScheme.primary,
//						shape = CircleShape
//					)
//			)
//
//			if (!isLast) {
//				Box(
//					modifier = Modifier
//						.width(1.dp)
//						.weight(1f)
//						.graphicsLayer {
//							scaleY = lineScale
//							transformOrigin = TransformOrigin(
//								pivotFractionX = 0.5f,
//								pivotFractionY = 0f
//							)
//						}
//						.background(colorScheme.outlineVariant)
//				)
//			}
//		}
//
//		Spacer(modifier = Modifier.width(12.dp))
//
//		Text(
//			text = formatEpochMillis(
//				time,
//				formatter,
//				zoneId
//			),
//			style = typography.titleMedium,
//			color = colorScheme.onBackground.copy(alpha = 0.82f),
//			modifier = Modifier.padding(bottom = 16.dp)
//		)
//	}
//}

@Composable
private fun ExpandableMoreRow(
	hiddenCount: Int,
	expanded: Boolean,
	visible: Boolean,
	onToggle: () -> Unit
) {
	val entrance by animateFloatAsState(
		targetValue = if (visible) 1f else 0f,
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessLow
		),
		label = "more_row_entrance"
	)

	val chevronRotation by animateFloatAsState(
		targetValue = if (expanded) 180f else 0f,
		animationSpec = tween(180),
		label = "chevron_rotation"
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.graphicsLayer {
				alpha = entrance
			}
			.clip(shapes.large)
			.clickable(
				enabled = visible,
				onClick = onToggle
			)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {

		Box(
			modifier = Modifier
				.padding(
					start = 6.dp,
					end = 12.dp
				)
				.size(8.dp)
				.border(
					1.dp,
					colorScheme.outline,
					CircleShape
				)
		)

		Text(
			text = if (expanded) {
				"Show less"
			} else {
				"Show $hiddenCount more"
			},
			style = typography.bodySmall,
			color = colorScheme.onBackground.copy(
				alpha = 0.7f
			)
		)

		Spacer(
			Modifier.size(8.dp)
		)

		Icon(
			imageVector = Icons.Default.KeyboardArrowDown,
			contentDescription = null,
			tint = colorScheme.onBackground.copy(
				alpha = 0.6f
			),
			modifier = Modifier
				.size(18.dp)
				.graphicsLayer {
					rotationZ = chevronRotation
				}
		)
	}
}

private fun computeNotificationTimes(
	alarmData: AlarmData
): List<Long> {
	val intervalMillis =
		alarmData.frequencyInMin.coerceAtLeast(1) * 60_000L

	if (alarmData.endTime <= alarmData.startTime) {
		return listOf(alarmData.startTime)
	}

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

@Composable
private fun SuccessIcon() {
	Surface(
		modifier = Modifier.size(72.dp),
		shape = shapes.extraLarge,
		color = colorScheme.primaryContainer
	) {
		Box(
			contentAlignment = Alignment.Center
		) {
			Icon(
				imageVector = Icons.Default.Check,
				contentDescription = null,
				tint = colorScheme.onPrimaryContainer,
				modifier = Modifier.size(38.dp)
			)
		}
	}
}

private fun Long.formatAlarmTime(): String {
	return Instant
		.ofEpochMilli(this)
		.atZone(ZoneId.systemDefault())
		.format(
			DateTimeFormatter.ofPattern("h:mm a")
		)
}
