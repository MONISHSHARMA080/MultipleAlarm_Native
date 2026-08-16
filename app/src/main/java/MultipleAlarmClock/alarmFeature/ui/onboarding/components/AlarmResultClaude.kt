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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
	val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

	var notificationTimes by remember { mutableStateOf<List<Long>>(emptyList()) }
	var expanded by rememberSaveable { mutableStateOf(false) }
	var headerVisible by remember { mutableStateOf(false) }
	var visibleRows by remember { mutableIntStateOf(0) }

	val haptic = LocalHapticFeedback.current

	LaunchedEffect(alarmData) {
		notificationTimes = withContext(Dispatchers.Default) {
			computeNotificationTimes(alarmData)
		}

		haptic.performHapticFeedback(HapticFeedbackType.LongPress)
		delay(250.milliseconds)

		headerVisible = true
		delay(350.milliseconds)

		val initialRowCount = minOf(notificationTimes.size, MAX_VISIBLE_TIMELINE_ROWS)
		repeat(initialRowCount) { index ->
			visibleRows = index + 1
			delay(TIMELINE_ROW_STAGGER_MS)
		}

		haptic.performHapticFeedback(HapticFeedbackType.LongPress)
	}

	// Determine displayed subset efficiently
	val visibleTimes = remember(expanded, notificationTimes) {
		if (expanded) notificationTimes else notificationTimes.take(MAX_VISIBLE_TIMELINE_ROWS)
	}

	Scaffold(
		bottomBar = {
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
					Text(text = "Finish", style = typography.titleMedium)
				}
			}
		}
	) { padding ->
		// Use LazyColumn instead of Column + verticalScroll
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(horizontal = 24.dp),
			// Align column children horizontally to the START (left)
			horizontalAlignment = Alignment.Start
		) {
			/* TITLE - Kept centered via Box */
			item(key = "header_title") {
				Spacer(modifier = Modifier.height(32.dp))
				Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = Alignment.Center
				) {
					AnimatedVisibility(
						visible = headerVisible,
						enter = scaleIn(
							initialScale = 0.75f,
							animationSpec = spring(
								dampingRatio = Spring.DampingRatioMediumBouncy,
								stiffness = Spring.StiffnessMedium
							)
						) + fadeIn(animationSpec = tween(250))
					) {
						SuccessIcon()
					}
				}
				Spacer(modifier = Modifier.height(20.dp))
				Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = Alignment.Center
				) {
					AnimatedVisibility(
						visible = headerVisible,
						enter = fadeIn(animationSpec = tween(350, delayMillis = 100)) +
								slideInVertically(initialOffsetY = { 12 }, animationSpec = tween(350))
					) {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(
								text = "You're all set!",
								style = typography.headlineLarge,
								fontWeight = FontWeight.SemiBold,
								color = colorScheme.onBackground,
								textAlign = TextAlign.Center
							)
							Spacer(modifier = Modifier.height(4.dp))
							Text(
								text = "Your alarm was scheduled successfully",
								style = typography.bodyMedium,
								color = colorScheme.onBackground.copy(alpha = 0.72f),
								textAlign = TextAlign.Center
							)
						}
					}
				}
				Spacer(modifier = Modifier.height(28.dp))
				AlarmCard(
					startTime = alarmData.startTime,
					endTime = alarmData.endTime,
					frequencyInMin = alarmData.frequencyInMin,
					formatter = timeFormatter,
					zoneId = zoneId
				)
				Spacer(modifier = Modifier.height(24.dp))
				Text(
					text = "Alarms would ring on",
					style = typography.titleSmall,
					fontWeight = FontWeight.Normal,
					color = colorScheme.onSurface.copy(alpha = 0.67f)
				)
				Spacer(modifier = Modifier.height(12.dp))
			}


			/* TIMELINE ROWS - Automatically align Start */
			itemsIndexed(
				items = visibleTimes,
				key = { index, time -> "$time-$index" }
			) { index, time ->
				TimelineRow(
					time = time,
					formatter = timeFormatter,
					zoneId = zoneId,
					visible = expanded || index < visibleRows,
					isLast = index == visibleTimes.lastIndex
				)
			}

			/* SHOW MORE / SHOW LESS BUTTON */
			if (notificationTimes.size > MAX_VISIBLE_TIMELINE_ROWS) {
				item(key = "expand_button") {
					ExpandableMoreRow(
						hiddenCount = notificationTimes.size - MAX_VISIBLE_TIMELINE_ROWS,
						expanded = expanded,
						onToggle = { expanded = !expanded }
					)
				}
			}

			item(key = "bottom_spacer") {
				Spacer(modifier = Modifier.height(24.dp))
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
		Column(modifier = Modifier.padding(20.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
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
						text = "${formatEpochMillis(startTime, formatter, zoneId)} - ${formatEpochMillis(endTime, formatter, zoneId)}",
						style = typography.titleMedium,
						fontWeight = FontWeight.Normal,
						color = colorScheme.onSurface
					)

					Spacer(modifier = Modifier.height(2.dp))

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
	// Format once per timestamp change
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
		// Timeline Dot & Line
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
			modifier = Modifier
				.padding(start = 6.dp, end = 12.dp)
				.size(8.dp)
				.border(1.dp, colorScheme.outline, CircleShape)
		)

		Text(
			text = if (expanded) "Show less" else "Show $hiddenCount more",
			style = typography.bodySmall,
			color = colorScheme.onBackground.copy(alpha = 0.7f)
		)

		Spacer(Modifier.size(8.dp))

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
		Box(contentAlignment = Alignment.Center) {
			Icon(
				imageVector = Icons.Default.Check,
				contentDescription = null,
				tint = colorScheme.onPrimaryContainer,
				modifier = Modifier.size(38.dp)
			)
		}
	}
}
