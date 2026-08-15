package MultipleAlarmClock.alarmFeature.ui.onboarding.components

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


private const val MAX_VISIBLE_TIMELINE_ROWS = 6
private const val TIMELINE_ROW_STAGGER_MS = 90L

@Composable
fun AlarmResultClaude(
	alarmData: AlarmData?,
	onNextClick: () -> Unit
) {
	if (alarmData == null ) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			CircularProgressIndicator()
		}
	}else{
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

	val notificationTimes = remember(alarmData) { computeNotificationTimes(alarmData) }
	val startLabel = remember(alarmData) { formatEpochMillis(alarmData.startTime, timeFormatter, zoneId) }
	val endLabel = remember(alarmData) { formatEpochMillis(alarmData.endTime, timeFormatter, zoneId) }

	val collapsedRowCount = minOf(notificationTimes.size, MAX_VISIBLE_TIMELINE_ROWS)
	val hiddenCount = notificationTimes.size - collapsedRowCount

	var expanded by rememberSaveable { mutableStateOf(false) }
	var checkVisible by remember { mutableStateOf(false) }
	var headerVisible by remember { mutableStateOf(false) }
	var cardVisible by remember { mutableStateOf(false) }
	var visibleRows by remember { mutableIntStateOf(0) }

	val haptic = LocalHapticFeedback.current

	LaunchedEffect(alarmData) {
		checkVisible = true
		haptic.performHapticFeedback(HapticFeedbackType.LongPress)
		delay(250)
		headerVisible = true
		delay(150)
		cardVisible = true
		delay(200)
		repeat(collapsedRowCount) { index ->
			visibleRows = index + 1
			delay(TIMELINE_ROW_STAGGER_MS)
		}
		haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
						text = "Done",
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
				.padding(horizontal = 24.dp)
				.verticalScroll(rememberScrollState())
		) {
			Spacer(modifier = Modifier.height(24.dp))

			AlarmResultHeader(
				checkVisible = checkVisible,
				headerVisible = headerVisible,
			)

			Spacer(modifier = Modifier.height(20.dp))

			AlarmSummaryCard(
				visible = cardVisible,
				startLabel = startLabel,
				endLabel = endLabel,
				frequencyInMin = alarmData.frequencyInMin,
				notificationCount = notificationTimes.size,
				message = alarmData.message
			)

			Spacer(modifier = Modifier.height(24.dp))

			Column {
				notificationTimes.take(collapsedRowCount).forEachIndexed { index, time ->
					TimelineRow(
						time = time,
						formatter = timeFormatter,
						zoneId = zoneId,
						visible = index < visibleRows,
						isLast = index == collapsedRowCount - 1 && hiddenCount == 0
					)
				}

				if (hiddenCount > 0) {
					ExpandableMoreRow(
						hiddenCount = hiddenCount,
						expanded = expanded,
						visible = visibleRows >= collapsedRowCount,
						onToggle = { expanded = !expanded }
					)

					AnimatedVisibility(
						visible = expanded,
						enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(),
						exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut()
					) {
						Column {
							val expandedTimes = notificationTimes.drop(collapsedRowCount)
							expandedTimes.forEachIndexed { index, time ->
								TimelineRow(
									time = time,
									formatter = timeFormatter,
									zoneId = zoneId,
									visible = true,
									isLast = index == expandedTimes.size - 1
								)
							}
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}

@Composable
private fun AlarmResultHeader(
	checkVisible: Boolean,
	headerVisible: Boolean,
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Box(
			modifier = Modifier.size(36.dp),
			contentAlignment = Alignment.Center
		) {
			val ringScale by animateFloatAsState(
				targetValue = if (checkVisible) 2.4f else 0.8f,
				animationSpec = tween(700, easing = FastOutSlowInEasing),
				label = "ring_scale"
			)
			val ringAlpha by animateFloatAsState(
				targetValue = if (checkVisible) 0f else 0.5f,
				animationSpec = tween(700, easing = FastOutSlowInEasing),
				label = "ring_alpha"
			)

			Box(
				modifier = Modifier
					.matchParentSize()
					.graphicsLayer {
						scaleX = ringScale
						scaleY = ringScale
						alpha = ringAlpha
					}
					.background(colorScheme.primary.copy(alpha = 0.16f), CircleShape)
			)

			val checkScale by animateFloatAsState(
				targetValue = if (checkVisible) 1f else 0.4f,
				animationSpec = spring(
					dampingRatio = Spring.DampingRatioMediumBouncy,
					stiffness = Spring.StiffnessMedium
				),
				label = "check_scale"
			)
			val checkAlpha by animateFloatAsState(
				targetValue = if (checkVisible) 1f else 0f,
				animationSpec = tween(250),
			)

			Surface(
				modifier = Modifier
					.matchParentSize()
					.graphicsLayer {
						scaleX = checkScale
						scaleY = checkScale
						alpha = checkAlpha
					},
				shape = CircleShape,
				color = colorScheme.primaryContainer
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = Icons.Default.Check,
						contentDescription = null,
						tint = colorScheme.onPrimaryContainer,
						modifier = Modifier.size(18.dp)
					)
				}
			}
		}

		AnimatedVisibility(
			visible = headerVisible,
			enter = fadeIn(tween(400)) + slideInVertically(
				animationSpec = tween(400, easing = FastOutSlowInEasing),
				initialOffsetY = { it / 3 }
			)
		) {
			Column {
				Text(
					text = "You're all set",
					style = typography.titleMedium,
					color = colorScheme.onBackground
				)
			}
		}
	}
}

@Composable
private fun AlarmSummaryCard(
	visible: Boolean,
	startLabel: String,
	endLabel: String,
	frequencyInMin: Long,
	notificationCount: Int,
	message: String
) {
	AnimatedVisibility(
		visible = visible,
		enter = fadeIn(tween(400)) + slideInVertically(
			animationSpec = tween(400, easing = FastOutSlowInEasing),
			initialOffsetY = { it / 4 }
		)
	) {
		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = shapes.extraLarge,
			color = colorScheme.primaryContainer
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 20.dp, vertical = 16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Surface(
					modifier = Modifier.size(44.dp),
					shape = shapes.large,
					color = colorScheme.primary.copy(alpha = 0.12f)
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = Icons.Outlined.Alarm,
							contentDescription = null,
							tint = colorScheme.onPrimaryContainer,
							modifier = Modifier.size(22.dp)
						)
					}
				}

				Spacer(modifier = Modifier.width(14.dp))

				Column(modifier = Modifier.weight(1f)) {
					if (message.isNotBlank()) {
						Text(
							text = message,
							style = typography.labelLarge,
							color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
						)
						Spacer(modifier = Modifier.height(2.dp))
					}
					Text(
						text = "$startLabel - $endLabel",
						style = typography.titleMedium,
						color = colorScheme.onPrimaryContainer
					)
					Spacer(modifier = Modifier.height(2.dp))
					Text(
						text = "Every $frequencyInMin min - $notificationCount notifications",
						style = typography.bodySmall,
						color = colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
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
			Text(
				text = "Notification",
				style = typography.bodySmall,
				color = colorScheme.onBackground.copy(alpha = 0.6f)
			)
		}
	}
}

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
		animationSpec = tween(200),
		label = "chevron_rotation"
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.graphicsLayer { alpha = entrance }
			.clip(shapes.large)
			.clickable(enabled = visible, onClick = onToggle)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.padding(start = 6.dp, end = 12.dp)
				.size(8.dp)
				.border(1.dp, colorScheme.outline, CircleShape)
		)

		Text(
			text = if (expanded) "Show less" else "and $hiddenCount more",
			style = typography.bodySmall,
			color = colorScheme.onBackground.copy(alpha = 0.7f),
			modifier = Modifier.weight(1f)
		)

		Icon(
			imageVector = Icons.Default.KeyboardArrowDown,
			contentDescription = null,
			tint = colorScheme.onBackground.copy(alpha = 0.6f),
			modifier = Modifier
				.size(18.dp)
				.graphicsLayer { rotationZ = chevronRotation }
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
