package MultipleAlarmClock.alarmFeature.ui.onboarding.components

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AlarmResultOpenAi(
	alarmData: AlarmData?,
	onNextClick: () -> Unit
) {
	if (alarmData == null) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = "Something went wrong",
				style = MaterialTheme.typography.bodyLarge
			)
		}
		return
	}

	val colorScheme = MaterialTheme.colorScheme
	val typography = MaterialTheme.typography
	val shapes = MaterialTheme.shapes

	val startTime = alarmData.startTime.toLocalTime()
	val endTime = alarmData.endTime.toLocalTime()

	val alarmTimes = remember(
		alarmData.startTime,
		alarmData.endTime,
		alarmData.frequencyInMin
	) {
		generateAlarmTimes(
			startTimeMillis = alarmData.startTime,
			endTimeMillis = alarmData.endTime,
			frequencyInMin = alarmData.frequencyInMin
		)
	}

	/*
	 * We don't want to render hundreds of times.
	 *
	 * For example:
	 *
	 * 12:05
	 * 12:10
	 * 12:15
	 * 12:20
	 * ...
	 * 1:00
	 */
	val displayTimes = remember(alarmTimes) {
		when {
			alarmTimes.size <= 6 -> alarmTimes

			else -> buildList {
				addAll(alarmTimes.take(4))
				add(alarmTimes.last())
			}
		}
	}

	val hiddenCount = remember(alarmTimes) {
		if (alarmTimes.size <= 6) {
			0
		} else {
			alarmTimes.size - 5
		}
	}

	var showSuccess by remember { mutableStateOf(false) }
	var showSchedule by remember { mutableStateOf(false) }
	var visibleTimes by remember { mutableIntStateOf(0) }
	var showNotification by remember { mutableStateOf(false) }
	var showBottomContent by remember { mutableStateOf(false) }

	LaunchedEffect(alarmData) {
		// Success icon
		showSuccess = true

		delay(180)

		// Schedule card
		showSchedule = true

		delay(350)

		// Reveal alarm times one by one
		displayTimes.indices.forEach { index ->
			visibleTimes = index + 1
			delay(90)
		}

		delay(250)

		// Notification arrives
		showNotification = true

		delay(550)

		// Bottom CTA
		showBottomContent = true
	}

	Scaffold(
		bottomBar = {
			AnimatedVisibility(
				visible = showBottomContent,
				enter = fadeIn(
					animationSpec = tween(300)
				) + slideInVertically(
					initialOffsetY = { it / 2 },
					animationSpec = tween(
						durationMillis = 450,
						easing = FastOutSlowInEasing
					)
				)
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.background(colorScheme.background)
						.navigationBarsPadding()
						.padding(horizontal = 26.dp)
						.padding(bottom = 20.dp),
					horizontalAlignment = Alignment.CenterHorizontally
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
		}
	) { paddingValues ->

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {

			Spacer(modifier = Modifier.height(32.dp))

			/*
			 * SUCCESS ICON
			 */
			AnimatedVisibility(
				visible = showSuccess,
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

			Spacer(modifier = Modifier.height(20.dp))

			/*
			 * TITLE
			 */
			AnimatedVisibility(
				visible = showSuccess,
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

					Spacer(modifier = Modifier.height(2.dp))

					Text(
						text = "Your alarm is scheduled",
						style = typography.bodyMedium,
						color = colorScheme.onBackground.copy(alpha = 0.72f),
						textAlign = TextAlign.Center
					)
				}
			}

			Spacer(modifier = Modifier.height(28.dp))

			/*
			 * SCHEDULE CARD
			 */
			AnimatedVisibility(
				visible = showSchedule,
				enter = fadeIn(
					animationSpec = tween(350)
				) + slideInVertically(
					initialOffsetY = { 30 },
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioNoBouncy,
						stiffness = Spring.StiffnessMediumLow
					)
				)
			) {
				ScheduleCard(
					startTime = alarmData.startTime,
					endTime = alarmData.endTime,
					frequencyInMin = alarmData.frequencyInMin
				)
			}

			Spacer(modifier = Modifier.height(28.dp))

			/*
			 * "YOU'LL RECEIVE" SECTION
			 */
			AnimatedVisibility(
				visible = visibleTimes > 0,
				enter = fadeIn(tween(300))
			) {
				Column(
					modifier = Modifier.fillMaxWidth()
				) {

					Text(
						text = "You'll receive notification on",
						style = typography.titleSmall,
						fontWeight = FontWeight.Medium,
						color = colorScheme.onBackground
					)

					Spacer(modifier = Modifier.height(12.dp))

					Surface(
						modifier = Modifier.fillMaxWidth(),
						shape = shapes.extraLarge,
						color = colorScheme.surfaceContainer
					) {
						Column(
							modifier = Modifier.padding(18.dp)
						) {

							displayTimes.forEachIndexed { index, time ->

								val isLast = index == displayTimes.lastIndex

								AnimatedVisibility(
									visible = index < visibleTimes,
									enter = fadeIn(
										animationSpec = tween(220)
									) + slideInVertically(
										initialOffsetY = { 10 },
										animationSpec = tween(220)
									)
								) {
									AlarmTimeRow(
										time = time,
										isLast = isLast,
										colorScheme = colorScheme,
										typography = typography
									)
								}
							}

							if (hiddenCount > 0) {
								AnimatedVisibility(
									visible = visibleTimes >= displayTimes.size,
									enter = fadeIn(tween(250))
								) {
									Text(
										text = "$hiddenCount more notifications",
										style = typography.bodyMedium,
										color = colorScheme.onSurfaceVariant,
										modifier = Modifier
											.padding(
												start = 38.dp,
												top = 4.dp
											)
									)
								}
							}
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(28.dp))

			/*
			 * NOTIFICATION PREVIEW
			 */
//			AnimatedVisibility(
//				visible = showNotification,
//				enter = fadeIn(
//					animationSpec = tween(300)
//				) + slideInVertically(
//					initialOffsetY = { -20 },
//					animationSpec = spring(
//						dampingRatio = Spring.DampingRatioNoBouncy,
//						stiffness = Spring.StiffnessMedium
//					)
//				) + scaleIn(
//					initialScale = 0.97f,
//					animationSpec = tween(300)
//				)
//			) {
//				NotificationPreview(
//					message = alarmData.message,
//					time = alarmData.startTime,
//					colorScheme = colorScheme,
//					typography = typography
//				)
//			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
@Composable
private fun SuccessIcon() {
	val colorScheme = MaterialTheme.colorScheme

	Surface(
		modifier = Modifier.size(72.dp),
		shape = MaterialTheme.shapes.extraLarge,
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
@Composable
private fun ScheduleCard(
	startTime: Long,
	endTime: Long,
	frequencyInMin: Long
) {
	val colorScheme = MaterialTheme.colorScheme
	val typography = MaterialTheme.typography

	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.extraLarge,
		color = colorScheme.primaryContainer
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
					shape = MaterialTheme.shapes.large,
					color = colorScheme.primary.copy(alpha = 0.12f)
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

				Spacer(modifier = Modifier.width(14.dp))

				Column {
					Text(
						text = "${startTime.formatAlarmTime()} → ${endTime.formatAlarmTime()}",
						style = typography.titleLarge,
						fontWeight = FontWeight.SemiBold,
						color = colorScheme.onPrimaryContainer
					)

					Spacer(modifier = Modifier.height(3.dp))

					Text(
						text = "Every $frequencyInMin minutes",
						style = typography.bodyMedium,
						color = colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
					)
				}
			}
		}
	}
}
@Composable
private fun AlarmTimeRow(
	time: Long,
	isLast: Boolean,
	colorScheme: ColorScheme,
	typography: Typography
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(42.dp),
		verticalAlignment = Alignment.CenterVertically
	) {

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.width(20.dp)
		) {
			Box(
				modifier = Modifier
					.size(9.dp)
					.background(
						color = colorScheme.primary,
						shape = CircleShape
					)
			)

			if (!isLast) {
				Box(
					modifier = Modifier
						.width(1.dp)
						.height(25.dp)
						.background(
							color = colorScheme.outlineVariant
						)
				)
			}
		}

		Spacer(modifier = Modifier.width(18.dp))

		Text(
			text = time.formatAlarmTime(),
			style = typography.bodyLarge,
			fontWeight = FontWeight.Medium,
			color = colorScheme.onSurface
		)
	}
}
@Composable
private fun NotificationPreview(
	message: String,
	time: Long,
	colorScheme: ColorScheme,
	typography: Typography
) {
	Column(
		modifier = Modifier.fillMaxWidth()
	) {

		Text(
			text = "Your notification",
			style = typography.titleMedium,
			fontWeight = FontWeight.SemiBold,
			color = colorScheme.onBackground
		)

		Spacer(modifier = Modifier.height(12.dp))

		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = MaterialTheme.shapes.large,
			color = colorScheme.surfaceContainerHigh,
			tonalElevation = 2.dp
		) {
			Row(
				modifier = Modifier.padding(16.dp),
				verticalAlignment = Alignment.Top
			) {

				Surface(
					modifier = Modifier.size(42.dp),
					shape = MaterialTheme.shapes.medium,
					color = colorScheme.primaryContainer
				) {
					Box(
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.Outlined.Notifications,
							contentDescription = null,
							tint = colorScheme.onPrimaryContainer,
							modifier = Modifier.size(22.dp)
						)
					}
				}

				Spacer(modifier = Modifier.width(12.dp))

				Column(
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = "Multiple Alarm",
						style = typography.labelLarge,
						color = colorScheme.onSurfaceVariant
					)

					Spacer(modifier = Modifier.height(3.dp))

					Text(
						text = message.ifBlank { "Your alarm" },
						style = typography.titleMedium,
						color = colorScheme.onSurface
					)

					Spacer(modifier = Modifier.height(3.dp))

					Text(
						text = time.formatAlarmTime(),
						style = typography.bodySmall,
						color = colorScheme.onSurfaceVariant
					)
				}
			}
		}
	}
}
private fun Long.toLocalTime(): LocalTime {
	return Instant
		.ofEpochMilli(this)
		.atZone(ZoneId.systemDefault())
		.toLocalTime()
}

private fun generateAlarmTimes(
	startTimeMillis: Long,
	endTimeMillis: Long,
	frequencyInMin: Long
): List<Long> {
	if (frequencyInMin <= 0) {
		return listOf(startTimeMillis)
	}

	val result = mutableListOf<Long>()

	var current = startTimeMillis
	val intervalMillis = frequencyInMin * 60_000L

	while (current <= endTimeMillis) {
		result += current
		current += intervalMillis
	}

	return result
}
private fun Long.formatAlarmTime(): String {
	return Instant
		.ofEpochMilli(this)
		.atZone(ZoneId.systemDefault())
		.format(
			DateTimeFormatter.ofPattern("h:mm a")
		)
}