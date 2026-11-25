package com.coolApps.MultipleAlarmClock.presentation.onboarding.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState
import com.coolApps.MultipleAlarmClock.presentation.onboarding.OnboardingPersonalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

private const val PIXEL_10_PRO_ASPECT_RATIO = 9f / 19.6f
private const val PIXEL_BASE_WIDTH_DP = 260f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelLockScreenPreviewScreen(
	alarmData: AlarmData?,
	personalization: OnboardingPersonalization,
	onButtonStateChange: (ButtonState) -> Unit,
	modifier: Modifier = Modifier,
) {
	val view = LocalView.current

	LaunchedEffect(alarmData != null) {
		if (alarmData != null) {
			onButtonStateChange(ButtonState.Enabled)
			view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
		} else {
			onButtonStateChange(ButtonState.Hidden)
		}
	}

	if (alarmData == null) {
		Box(
			modifier = modifier.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			CircularProgressIndicator()
		}
		return
	}

	var alarmTimes by remember(alarmData.startTime, alarmData.endTime, alarmData.frequencyInMin) {
		mutableStateOf(listOf(alarmData.startTime))
	}
	LaunchedEffect(alarmData.startTime, alarmData.endTime, alarmData.frequencyInMin) {
		alarmTimes = withContext(Dispatchers.Default) {
			alarmData.alarmTimeSequence().toList().ifEmpty { listOf(alarmData.startTime) }
		}
	}

	var activeIndex by remember(alarmTimes) { mutableIntStateOf(0) }
	var notificationVisible by remember(alarmTimes) { mutableStateOf(true) }
	var notificationTriggerId by remember(alarmTimes) { mutableIntStateOf(0) }
	val enterProgress = remember { Animatable(1f) }
	val stopPress = remember { Animatable(0f) }
	val swipeX = remember { Animatable(0f) }
	val enterSpatialSpec = motionScheme.defaultSpatialSpec<Float>()

	LaunchedEffect(alarmTimes) {
		activeIndex = 0
		stopPress.snapTo(0f)
		swipeX.snapTo(0f)
		enterProgress.snapTo(1f)
		notificationVisible = true
		notificationTriggerId++
		if (alarmTimes.size <= 1) return@LaunchedEffect
		while (true) {
			val isLast = activeIndex == alarmTimes.lastIndex
			delay(if (isLast) 1700.milliseconds else 1150.milliseconds)

			stopPress.animateTo(1f, tween(110))
			delay(60.milliseconds)

			swipeX.animateTo(-0.035f, tween(80, easing = FastOutSlowInEasing))
			swipeX.animateTo(1.25f, tween(340, easing = CubicBezierEasing(0.45f, 0f, 1f, 0.6f)))

			// Hide while swipeX is still off-screen (alpha 0) so resetting swipeX cannot flash the old card back.
			notificationVisible = false
			delay(140.milliseconds)

			activeIndex = (activeIndex + 1) % alarmTimes.size
			delay(260.milliseconds)

			stopPress.snapTo(0f)
			swipeX.snapTo(0f)
			enterProgress.snapTo(0f)
			notificationVisible = true
			notificationTriggerId++
			enterProgress.animateTo(1f, enterSpatialSpec)
		}
	}

	val safeIndex = activeIndex.coerceIn(0, alarmTimes.lastIndex.coerceAtLeast(0))
	val locale = LocalLocale.current.platformLocale
	val zoneId = remember { ZoneId.systemDefault() }
	val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm", locale) }
	val amPmFormatter = remember(locale) { DateTimeFormatter.ofPattern("a", locale) }
	val fullTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }
	val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }

	val startFormatted = remember(alarmData.startTime, fullTimeFormatter, zoneId) {
		Instant.ofEpochMilli(alarmData.startTime).atZone(zoneId).format(fullTimeFormatter)
	}
	val endFormatted = remember(alarmData.endTime, fullTimeFormatter, zoneId) {
		Instant.ofEpochMilli(alarmData.endTime).atZone(zoneId).format(fullTimeFormatter)
	}
	val lockScreenDate = remember(alarmData.startTime, dateFormatter, zoneId) {
		Instant.ofEpochMilli(alarmData.startTime).atZone(zoneId).format(dateFormatter)
	}
	val defaultMessage = stringResource(personalization.defaultAlarmMessageRes)
	val alarmTitle = alarmData.message.ifBlank { defaultMessage }

	BoxWithConstraints(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		val isWideLayout = maxWidth >= 600.dp || maxWidth > maxHeight

		if (isWideLayout) {
			Row(
				modifier = Modifier
					.widthIn(max = 920.dp)
					.fillMaxSize()
					.padding(horizontal = 24.dp)
					.padding(top = 12.dp, bottom = 128.dp),
				horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Column(
					modifier = Modifier.weight(1f),
					horizontalAlignment = Alignment.Start,
				) {
					Text(
						text = stringResource(personalization.lockScreenTitleRes),
						style = typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
						color = colorScheme.onBackground,
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = stringResource(
							personalization.lockScreenSubtitleRes,
							alarmData.frequencyInMin,
						),
						style = typography.bodyLarge,
						color = colorScheme.onSurfaceVariant,
					)
					Spacer(modifier = Modifier.height(20.dp))
					WideScheduleSummaryCard(
						startFormatted = startFormatted,
						endFormatted = endFormatted,
						frequencyInMin = alarmData.frequencyInMin,
						activeNumber = safeIndex + 1,
						totalCount = alarmTimes.size,
					)
				}

				BoxWithConstraints(
					modifier = Modifier
						.weight(1f)
						.fillMaxHeight(),
					contentAlignment = Alignment.Center,
				) {
					AdaptivePixel10ProMockup(
						maxAvailableWidth = maxWidth,
						maxAvailableHeight = maxHeight,
						alarmTimes = alarmTimes,
						activeIndex = safeIndex,
						notificationVisible = notificationVisible,
						notificationTriggerId = notificationTriggerId,
						enterProgress = enterProgress.asState(),
						stopPress = stopPress.asState(),
						swipeX = swipeX.asState(),
						lockScreenDate = lockScreenDate,
						alarmMessage = alarmTitle,
						zoneId = zoneId,
						timeFormatter = timeFormatter,
						amPmFormatter = amPmFormatter,
						fullTimeFormatter = fullTimeFormatter,
					)
				}
			}
		} else {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = 24.dp)
					.padding(top = 8.dp, bottom = 128.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Text(
					text = stringResource(personalization.lockScreenTitleRes),
					style = typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onBackground,
					textAlign = TextAlign.Center,
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(
						personalization.lockScreenSubtitleRes,
						alarmData.frequencyInMin,
					),
					style = typography.bodyMedium,
					color = colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center,
					modifier = Modifier.widthIn(max = 360.dp),
				)
				Spacer(modifier = Modifier.height(10.dp))
				Surface(
					shape = shapes.large,
					color = colorScheme.secondaryContainer,
				) {
					Text(
						text = stringResource(
							R.string.onboarding_v4_wakeup_summary,
							startFormatted,
							endFormatted,
							alarmData.frequencyInMin,
						),
						style = typography.labelMedium.copy(
							fontWeight = FontWeight.Bold,
							fontFeatureSettings = "tnum",
						),
						color = colorScheme.onSecondaryContainer,
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
					)
				}
				Spacer(modifier = Modifier.height(14.dp))
				BoxWithConstraints(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
					contentAlignment = Alignment.Center,
				) {
					AdaptivePixel10ProMockup(
						maxAvailableWidth = maxWidth * 0.86f,
						maxAvailableHeight = maxHeight,
						alarmTimes = alarmTimes,
						activeIndex = safeIndex,
						notificationVisible = notificationVisible,
						notificationTriggerId = notificationTriggerId,
						enterProgress = enterProgress.asState(),
						stopPress = stopPress.asState(),
						swipeX = swipeX.asState(),
						lockScreenDate = lockScreenDate,
						alarmMessage = alarmTitle,
						zoneId = zoneId,
						timeFormatter = timeFormatter,
						amPmFormatter = amPmFormatter,
						fullTimeFormatter = fullTimeFormatter,
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WideScheduleSummaryCard(
	startFormatted: String,
	endFormatted: String,
	frequencyInMin: Long,
	activeNumber: Int,
	totalCount: Int,
) {
	val progress by animateFloatAsState(
		targetValue = activeNumber.toFloat() / totalCount.coerceAtLeast(1).toFloat(),
		animationSpec = motionScheme.defaultSpatialSpec(),
		label = "previewAlarmProgress",
	)

	Surface(
		shape = shapes.extraLarge,
		color = colorScheme.surfaceContainerHigh,
		modifier = Modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Surface(
					shape = shapes.large,
					color = colorScheme.primaryContainer,
					modifier = Modifier.size(44.dp),
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = Icons.Outlined.Alarm,
							contentDescription = null,
							tint = colorScheme.onPrimaryContainer,
						)
					}
				}
				Column {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = startFormatted,
							style = typography.titleMedium.copy(
								fontWeight = FontWeight.Bold,
								fontFeatureSettings = "tnum",
							),
							color = colorScheme.onSurface,
						)
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
							contentDescription = null,
							tint = colorScheme.onSurfaceVariant,
							modifier = Modifier
								.padding(horizontal = 8.dp)
								.size(16.dp),
						)
						Text(
							text = endFormatted,
							style = typography.titleMedium.copy(
								fontWeight = FontWeight.Bold,
								fontFeatureSettings = "tnum",
							),
							color = colorScheme.onSurface,
						)
					}
					Text(
						text = stringResource(
							R.string.onboarding_v4_preview_every_min,
							frequencyInMin,
							totalCount,
						),
						style = typography.bodySmall,
						color = colorScheme.onSurfaceVariant,
					)
				}
			}

			LinearProgressIndicator(
				progress = { progress },
				modifier = Modifier.fillMaxWidth(),
				strokeCap = StrokeCap.Round,
			)

			Text(
				text = stringResource(R.string.onboarding_v4_preview_alarm_count, activeNumber, totalCount),
				style = typography.labelMedium.copy(fontFeatureSettings = "tnum"),
				color = colorScheme.primary,
			)
		}
	}
}

@Composable
private fun AdaptivePixel10ProMockup(
	maxAvailableWidth: Dp,
	maxAvailableHeight: Dp,
	alarmTimes: List<Long>,
	activeIndex: Int,
	notificationVisible: Boolean,
	notificationTriggerId: Int,
	enterProgress: State<Float>,
	stopPress: State<Float>,
	swipeX: State<Float>,
	lockScreenDate: String,
	alarmMessage: String,
	zoneId: ZoneId,
	timeFormatter: DateTimeFormatter,
	amPmFormatter: DateTimeFormatter,
	fullTimeFormatter: DateTimeFormatter,
) {
	val targetHeight = minOf(maxAvailableHeight, maxAvailableWidth / PIXEL_10_PRO_ASPECT_RATIO)
		.coerceAtLeast(1.dp)
	val targetWidth = targetHeight * PIXEL_10_PRO_ASPECT_RATIO
	val scale = targetWidth.value / PIXEL_BASE_WIDTH_DP
	val baseDensity = LocalDensity.current

	// Scale canonical 260dp Pixel 10 Pro geometry proportionally to any viewport.
	Box(
		modifier = Modifier.size(width = targetWidth, height = targetHeight),
		contentAlignment = Alignment.Center,
	) {
		CompositionLocalProvider(
			LocalDensity provides Density(
				density = baseDensity.density * scale,
				fontScale = 1f,
			),
		) {
			Pixel10ProFrame(
				alarmTimes = alarmTimes,
				activeIndex = activeIndex,
				notificationVisible = notificationVisible,
				notificationTriggerId = notificationTriggerId,
				enterProgress = enterProgress,
				stopPress = stopPress,
				swipeX = swipeX,
				lockScreenDate = lockScreenDate,
				alarmMessage = alarmMessage,
				zoneId = zoneId,
				timeFormatter = timeFormatter,
				amPmFormatter = amPmFormatter,
				fullTimeFormatter = fullTimeFormatter,
			)
		}
	}
}

@Composable
private fun Pixel10ProFrame(
	alarmTimes: List<Long>,
	activeIndex: Int,
	notificationVisible: Boolean,
	notificationTriggerId: Int,
	enterProgress: State<Float>,
	stopPress: State<Float>,
	swipeX: State<Float>,
	lockScreenDate: String,
	alarmMessage: String,
	zoneId: ZoneId,
	timeFormatter: DateTimeFormatter,
	amPmFormatter: DateTimeFormatter,
	fullTimeFormatter: DateTimeFormatter,
) {
	val baseHeight = (PIXEL_BASE_WIDTH_DP / PIXEL_10_PRO_ASPECT_RATIO).dp
	val outerRadius = 38.dp
	val bezelPadding = 8.dp
	val screenRadius = outerRadius - bezelPadding

	Box(
		modifier = Modifier
			.size(width = PIXEL_BASE_WIDTH_DP.dp, height = baseHeight),
		contentAlignment = Alignment.Center,
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val buttonWidth = 3.dp.toPx()
			val x = size.width - 0.5.dp.toPx()
			drawRoundRect(
				color = Color(0xFF565A64),
				topLeft = Offset(x, size.height * 0.23f),
				size = Size(buttonWidth, size.height * 0.065f),
				cornerRadius = CornerRadius(2.dp.toPx()),
			)
			drawRoundRect(
				color = Color(0xFF4B4F58),
				topLeft = Offset(x, size.height * 0.33f),
				size = Size(buttonWidth, size.height * 0.125f),
				cornerRadius = CornerRadius(2.dp.toPx()),
			)
		}

		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(end = 2.dp)
				.clip(RoundedCornerShape(outerRadius))
				.background(
					brush = Brush.linearGradient(
						colors = listOf(
							Color(0xFF4C505A),
							Color(0xFF23262D),
							Color(0xFF3E424B),
						),
					),
				)
				.padding(2.dp)
				.clip(RoundedCornerShape(outerRadius - 2.dp))
				.background(Color(0xFF0A0B0E))
				.padding(bezelPadding - 2.dp),
		) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clip(RoundedCornerShape(screenRadius)),
			) {
				PixelLockScreenContent(
					alarmTimes = alarmTimes,
					activeIndex = activeIndex,
					notificationVisible = notificationVisible,
					notificationTriggerId = notificationTriggerId,
					enterProgress = enterProgress,
					stopPress = stopPress,
					swipeX = swipeX,
					lockScreenDate = lockScreenDate,
					alarmMessage = alarmMessage,
					zoneId = zoneId,
					timeFormatter = timeFormatter,
					amPmFormatter = amPmFormatter,
					fullTimeFormatter = fullTimeFormatter,
				)

				Box(
					modifier = Modifier
						.align(Alignment.TopCenter)
						.padding(top = 2.dp)
						.size(width = 36.dp, height = 2.dp)
						.clip(CircleShape)
						.background(Color(0xFF1E2128)),
				)
				Box(
					modifier = Modifier
						.align(Alignment.TopCenter)
						.padding(top = 10.dp)
						.size(10.dp)
						.clip(CircleShape)
						.background(Color(0xFF050608))
						.border(0.8.dp, Color(0xFF232834), CircleShape),
					contentAlignment = Alignment.Center,
				) {
					Box(
						modifier = Modifier
							.size(3.dp)
							.clip(CircleShape)
							.background(Color(0xFF1F2B3E)),
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PixelLockScreenContent(
	alarmTimes: List<Long>,
	activeIndex: Int,
	notificationVisible: Boolean,
	notificationTriggerId: Int,
	enterProgress: State<Float>,
	stopPress: State<Float>,
	swipeX: State<Float>,
	lockScreenDate: String,
	alarmMessage: String,
	zoneId: ZoneId,
	timeFormatter: DateTimeFormatter,
	amPmFormatter: DateTimeFormatter,
	fullTimeFormatter: DateTimeFormatter,
) {
	val primaryTint = lerp(Color(0xFF0F172A), colorScheme.primary, 0.28f)
	val tertiaryTint = lerp(Color(0xFF0D111A), colorScheme.tertiary, 0.22f)
	val activeTimeMillis = alarmTimes[activeIndex]
	val formattedClock = remember(activeTimeMillis, timeFormatter, zoneId) {
		Instant.ofEpochMilli(activeTimeMillis).atZone(zoneId).format(timeFormatter)
	}
	val formattedAmPm = remember(activeTimeMillis, amPmFormatter, zoneId) {
		Instant.ofEpochMilli(activeTimeMillis).atZone(zoneId).format(amPmFormatter)
	}

	val clockStyle = typography.displayLarge.copy(
		fontSize = 54.sp,
		lineHeight = 58.sp,
		fontWeight = FontWeight.Bold,
		letterSpacing = 0.sp,
		fontFeatureSettings = "tnum",
		color = Color.White,
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(
				brush = Brush.verticalGradient(
					colors = listOf(
						primaryTint,
						Color(0xFF0D1017),
						tertiaryTint,
					),
				),
			),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 14.dp, vertical = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 1.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					imageVector = Icons.Rounded.Alarm,
					contentDescription = null,
					tint = Color.White.copy(alpha = 0.85f),
					modifier = Modifier.size(11.dp),
				)
				Row(
					horizontalArrangement = Arrangement.spacedBy(3.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.Rounded.SignalCellularAlt,
						contentDescription = null,
						tint = Color.White.copy(alpha = 0.85f),
						modifier = Modifier.size(11.dp),
					)
					Icon(
						imageVector = Icons.Rounded.Wifi,
						contentDescription = null,
						tint = Color.White.copy(alpha = 0.85f),
						modifier = Modifier.size(11.dp),
					)
					Icon(
						imageVector = Icons.Rounded.BatteryFull,
						contentDescription = null,
						tint = Color.White.copy(alpha = 0.85f),
						modifier = Modifier.size(11.dp),
					)
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			Text(
				text = lockScreenDate,
				style = typography.labelMedium.copy(fontWeight = FontWeight.Medium),
				color = Color.White.copy(alpha = 0.86f),
			)

			Spacer(modifier = Modifier.height(4.dp))

			Row(
				verticalAlignment = Alignment.Bottom,
				horizontalArrangement = Arrangement.Center,
			) {
				RollingClockText(
					timeText = formattedClock,
					style = clockStyle,
				)
				Spacer(modifier = Modifier.width(4.dp))
				RollingAmPmText(
					amPm = formattedAmPm,
					modifier = Modifier.padding(bottom = 10.dp),
				)
			}

			Spacer(modifier = Modifier.height(20.dp))

			PixelSingleNotificationSlot(
				alarmTimes = alarmTimes,
				activeIndex = activeIndex,
				visible = notificationVisible,
				triggerId = notificationTriggerId,
				enterProgress = enterProgress,
				stopPress = stopPress,
				swipeX = swipeX,
				alarmMessage = alarmMessage,
				zoneId = zoneId,
				fullTimeFormatter = fullTimeFormatter,
			)

			Spacer(modifier = Modifier.weight(1f))

			Box(
				modifier = Modifier
					.size(38.dp)
					.clip(CircleShape)
					.background(Color.White.copy(alpha = 0.09f))
					.border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = Icons.Rounded.Fingerprint,
					contentDescription = null,
					tint = Color.White.copy(alpha = 0.85f),
					modifier = Modifier.size(22.dp),
				)
			}

			Spacer(modifier = Modifier.height(14.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				LockShortcutIcon(Icons.Rounded.FlashlightOn)
				Box(
					modifier = Modifier
						.size(width = 78.dp, height = 3.5.dp)
						.clip(CircleShape)
						.background(Color.White.copy(alpha = 0.55f)),
				)
				LockShortcutIcon(Icons.Rounded.CameraAlt)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RollingClockText(
	timeText: String,
	style: TextStyle,
) {
	val parts = timeText.split(':')
	val hourPart = parts.getOrElse(0) { timeText }
	val minutePart = parts.getOrNull(1)

	Row(
		modifier = Modifier.animateContentSize(animationSpec = motionScheme.defaultSpatialSpec()),
		verticalAlignment = Alignment.CenterVertically,
	) {
		for (i in hourPart.indices) {
			val slotFromColon = hourPart.length - 1 - i
			key(slotFromColon) {
				RollingDigitSlot(
					char = hourPart[i],
					style = style,
				)
			}
		}
		if (minutePart != null) {
			Text(
				text = ":",
				style = style,
				maxLines = 1,
				softWrap = false,
			)
			for (i in minutePart.indices) {
				key("min_$i") {
					RollingDigitSlot(
						char = minutePart[i],
						style = style,
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RollingDigitSlot(
	char: Char,
	style: TextStyle,
) {
	val spatialSpec = motionScheme.defaultSpatialSpec<IntOffset>()
	val effectsSpec = motionScheme.defaultEffectsSpec<Float>()

	AnimatedContent(
		targetState = char,
		modifier = Modifier.clipToBounds(),
		transitionSpec = {
			(slideInVertically(spatialSpec) { it } + fadeIn(effectsSpec)) togetherWith
				(slideOutVertically(spatialSpec) { -it } + fadeOut(effectsSpec)) using
				SizeTransform(clip = true)
		},
		label = "rollingClockDigit",
	) { digit ->
		Text(
			text = digit.toString(),
			style = style,
			maxLines = 1,
			softWrap = false,
		)
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RollingAmPmText(
	amPm: String,
	modifier: Modifier = Modifier,
) {
	val spatialSpec = motionScheme.defaultSpatialSpec<IntOffset>()
	val effectsSpec = motionScheme.defaultEffectsSpec<Float>()

	AnimatedContent(
		targetState = amPm,
		modifier = modifier.clipToBounds(),
		transitionSpec = {
			(slideInVertically(spatialSpec) { it } + fadeIn(effectsSpec)) togetherWith
				(slideOutVertically(spatialSpec) { -it } + fadeOut(effectsSpec)) using
				SizeTransform(clip = true)
		},
		label = "rollingAmPm",
	) { period ->
		Text(
			text = period,
			style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
			color = Color.White.copy(alpha = 0.72f),
			maxLines = 1,
			softWrap = false,
		)
	}
}

@Composable
private fun PixelSingleNotificationSlot(
	alarmTimes: List<Long>,
	activeIndex: Int,
	visible: Boolean,
	triggerId: Int,
	enterProgress: State<Float>,
	stopPress: State<Float>,
	swipeX: State<Float>,
	alarmMessage: String,
	zoneId: ZoneId,
	fullTimeFormatter: DateTimeFormatter,
) {
	val triggerTime = alarmTimes.getOrNull(activeIndex)

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(74.dp),
		contentAlignment = Alignment.TopCenter,
	) {
		if (visible && triggerTime != null) {
			val timeText = remember(triggerTime, fullTimeFormatter, zoneId) {
				Instant.ofEpochMilli(triggerTime).atZone(zoneId).format(fullTimeFormatter)
			}
			ActiveAlarmNotificationCard(
				triggerId = triggerId,
				alarmNumber = activeIndex + 1,
				totalAlarms = alarmTimes.size,
				formattedTime = timeText,
				alarmMessage = alarmMessage,
				enterProgress = enterProgress,
				stopPress = stopPress,
				swipeX = swipeX,
			)
		}
	}
}

@Composable
private fun ActiveAlarmNotificationCard(
	triggerId: Int,
	alarmNumber: Int,
	totalAlarms: Int,
	formattedTime: String,
	alarmMessage: String,
	enterProgress: State<Float>,
	stopPress: State<Float>,
	swipeX: State<Float>,
) {
	val bellRotation = remember { Animatable(0f) }
	val pulse = remember { Animatable(0f) }

	LaunchedEffect(triggerId) {
		delay(120.milliseconds)
		pulse.snapTo(0.01f)
		pulse.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
		pulse.snapTo(0f)
	}

	LaunchedEffect(triggerId) {
		delay(120.milliseconds)
		repeat(4) {
			bellRotation.animateTo(14f, tween(55))
			bellRotation.animateTo(-14f, tween(55))
		}
		bellRotation.animateTo(0f, tween(70))
	}

	val ringColor = colorScheme.onPrimaryContainer

	Surface(
		shape = RoundedCornerShape(18.dp),
		color = colorScheme.primaryContainer,
		modifier = Modifier
			.fillMaxWidth()
			.graphicsLayer {
				val enter = enterProgress.value
				val sx = swipeX.value
				val enterScale = 0.92f + 0.08f * enter
				scaleX = enterScale
				scaleY = enterScale
				translationY = (1f - enter) * (-16.dp.toPx())
				translationX = sx * size.width
				rotationZ = sx * 5.5f
				val enterAlpha = enter.coerceIn(0f, 1f)
				val exitAlpha = 1f - ((sx - 0.25f) / 0.7f).coerceIn(0f, 1f)
				alpha = enterAlpha * exitAlpha
			},
	) {
		Row(
			modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Box(
				modifier = Modifier
					.size(28.dp)
					.drawBehind {
						val p = pulse.value
						if (p > 0f) {
							drawCircle(
								color = ringColor.copy(alpha = 0.35f * (1f - p)),
								radius = size.minDimension / 2f * (1f + 0.55f * p),
								style = Stroke(width = 1.5.dp.toPx()),
							)
						}
					}
					.clip(CircleShape)
					.background(colorScheme.primary.copy(alpha = 0.16f)),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = Icons.Rounded.Alarm,
					contentDescription = null,
					tint = colorScheme.onPrimaryContainer,
					modifier = Modifier
						.size(16.dp)
						.graphicsLayer { rotationZ = bellRotation.value },
				)
			}

			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(1.dp),
			) {
				Text(
					text = stringResource(
						R.string.onboarding_v4_preview_app_time,
						stringResource(R.string.app_name),
						formattedTime,
					),
					style = typography.labelSmall.copy(
						fontSize = 9.5.sp,
						fontFeatureSettings = "tnum",
					),
					color = colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				Text(
					text = alarmMessage,
					style = typography.labelMedium.copy(
						fontSize = 11.5.sp,
						fontWeight = FontWeight.Bold,
					),
					color = colorScheme.onPrimaryContainer,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				Text(
					text = stringResource(
						R.string.onboarding_v4_preview_alarm_count,
						alarmNumber,
						totalAlarms,
					),
					style = typography.labelSmall.copy(
						fontSize = 9.sp,
						fontFeatureSettings = "tnum",
					),
					color = colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
					maxLines = 1,
				)
			}

			val press = stopPress.value
			Surface(
				shape = CircleShape,
				color = lerp(colorScheme.secondaryContainer, colorScheme.primary, press),
				modifier = Modifier.graphicsLayer {
					val s = 1f - 0.12f * press
					scaleX = s
					scaleY = s
				},
			) {
				Text(
					text = stringResource(R.string.paywall_demo_stop),
					style = typography.labelSmall.copy(
						fontSize = 9.5.sp,
						fontWeight = FontWeight.Bold,
					),
					color = lerp(colorScheme.onSecondaryContainer, colorScheme.onPrimary, press),
					modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
				)
			}
		}
	}
}

@Composable
private fun LockShortcutIcon(icon: ImageVector) {
	Box(
		modifier = Modifier
			.size(28.dp)
			.clip(CircleShape)
			.background(Color.White.copy(alpha = 0.14f)),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = Color.White.copy(alpha = 0.88f),
			modifier = Modifier.size(14.dp),
		)
	}
}
