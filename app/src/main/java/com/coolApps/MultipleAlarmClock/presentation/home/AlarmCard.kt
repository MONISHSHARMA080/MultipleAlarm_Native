package com.coolApps.MultipleAlarmClock.presentation.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.AlarmContainerBoundsTransform
import com.coolApps.MultipleAlarmClock.presentation.AlarmTimeBoundsTransform
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable fun AlarmCard(
	alarmData: AlarmData,
	todayStartMs: Long,
	onEdit: (AlarmData) -> Unit,
	onToggle: (AlarmData, Boolean) -> Unit,
	onDelete: (AlarmData) -> Unit,
	modifier: Modifier = Modifier,
	onLongPress: (AlarmData) -> Unit = {},
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val colorScheme = colorScheme
	val isActive = alarmData.isReadyToUse
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val containerColor by animateColorAsState(
		targetValue = if (isActive) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "containerColor"
	)

	val contentColor by animateColorAsState(
		targetValue = if (isActive) colorScheme.onPrimaryContainer else colorScheme.onSurface,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "contentColor"
	)

	val animatedVerticalPadding by animateDpAsState(
		targetValue = if (isActive) 6.dp else 4.dp,
		animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
		label = "verticalPadding"
	)

	val targetCornerRadius = when {
		isPressed -> 16.dp
		isActive -> 28.dp
		else -> 20.dp
	}
	val cardCornerRadius by animateDpAsState(
		targetValue = targetCornerRadius,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "cardCornerRadius"
	)
	val cardScale by animateFloatAsState(
		targetValue = if (isPressed) 0.98f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "cardScale"
	)

	val view = LocalView.current
	val dismissState = rememberSwipeToDismissBoxState()
	val cardShape = RoundedCornerShape(cardCornerRadius)
	val horizontalPadding = 10.dp

	SwipeToDismissBox(
		state = dismissState,
		onDismiss = { _ ->
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
			}else{
				view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
			}
			onDelete(alarmData)
		},
		backgroundContent = {
			val direction = dismissState.dismissDirection
			val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
			val density = LocalDensity.current
			val rawOffset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
			val chipWidth = with(density) { abs(rawOffset).toDp() }

			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = horizontalPadding, vertical = animatedVerticalPadding),
				contentAlignment = alignment
			) {
				if (direction != SwipeToDismissBoxValue.Settled && chipWidth > 0.dp) {
					Box(
						modifier = Modifier
							.fillMaxHeight()
							.width(chipWidth)
							.padding(end = if (direction == SwipeToDismissBoxValue.EndToStart) 6.dp else 0.dp)
							.clip(cardShape)
							.background(colorScheme.errorContainer),
						contentAlignment = Alignment.Center
					) {
						val iconVisible = chipWidth > 52.dp
						val iconAlpha by animateFloatAsState(
							targetValue = if (iconVisible) 1f else 0f,
							animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
							label = "iconAlpha"
						)
						val iconScale by animateFloatAsState(
							targetValue = if (iconVisible) 1f else 0.7f,
							animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
							label = "iconScale"
						)
						Surface(
							shape = RoundedCornerShape(14.dp),
							color = colorScheme.error.copy(alpha = 0.14f),
							modifier = Modifier
								.size(40.dp)
								.scale(iconScale)
								.alpha(iconAlpha)
						) {
							Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
								Icon(
									Icons.Default.Delete,
									contentDescription = "Delete",
									tint = colorScheme.onErrorContainer,
									modifier = Modifier.size(22.dp)
								)
							}
						}
					}
				}
			}
		},
		modifier = modifier.fillMaxWidth()
	) {
		CompositionLocalProvider(
			LocalDensity provides Density(
				density = LocalDensity.current.density,
				fontScale = 1f
			)
		) {
			with(sharedTransitionScope) {
				Card(
					modifier = Modifier
						.fillMaxWidth()
						.scale(cardScale)
						.sharedBounds(
							sharedContentState = rememberSharedContentState(key = "alarm_card_${alarmData.id}"),
							animatedVisibilityScope = animatedVisibilityScope,
							clipInOverlayDuringTransition = OverlayClip(cardShape),
							boundsTransform = AlarmContainerBoundsTransform
						)
						.padding(horizontal = horizontalPadding, vertical = animatedVerticalPadding)
						.clip(cardShape)
						.combinedClickable(
							interactionSource = interactionSource,
							indication = null,
							onClick = { onEdit(alarmData) },
							onLongClick = { onLongPress(alarmData) }
						),
					colors = CardDefaults.cardColors(
						containerColor = containerColor,
						contentColor = contentColor
					),
					shape = cardShape,
					elevation = CardDefaults.cardElevation(
						defaultElevation = if (isActive) 2.dp else 0.dp
					)
				) {
					AlarmCardContent(
						alarmData = alarmData,
						todayStartMs = todayStartMs,
						isActive = isActive,
						contentColor = contentColor,
						onToggle = { value ->
							onToggle(alarmData, value)
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
								if (value) {
									view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
								} else {
									view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
								}
							}
						},
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmCardContent(
	alarmData: AlarmData,
	todayStartMs: Long,
	isActive: Boolean,
	contentColor: Color,
	onToggle: (Boolean) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val colorScheme = colorScheme
	val typography = typography
	val secondaryContentColor = contentColor.copy(alpha = 0.75f)

	// Optical roundness rule: outer card is 28dp (active) / 20dp (inactive) with 20dp padding -> inner badge ~12dp
	val badgeCornerRadius by animateDpAsState(
		targetValue = if (isActive) 12.dp else 10.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "badgeCornerRadius"
	)
	val badgeContainerColor by animateColorAsState(
		targetValue = if (isActive) {
			colorScheme.secondaryContainer
		} else {
			colorScheme.surfaceContainerHighest
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "badgeContainerColor"
	)
	val badgeContentColor by animateColorAsState(
		targetValue = if (isActive) {
			colorScheme.onSecondaryContainer
		} else {
			colorScheme.onSurfaceVariant
		},
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "badgeContentColor"
	)

	// Day/status indicator morphs between small circle (50% = 5.dp) when inactive and squircle (3.dp) when active
	val indicatorCorner by animateDpAsState(
		targetValue = if (isActive) 3.dp else 5.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "indicatorCorner"
	)
	val indicatorColor by animateColorAsState(
		targetValue = if (isActive) colorScheme.primary else colorScheme.outline,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "indicatorColor"
	)

	with(sharedTransitionScope) {
		Column(
			modifier = Modifier
				.padding(horizontal = 22.dp, vertical = 20.dp)
				.fillMaxWidth()
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Surface(
					shape = RoundedCornerShape(badgeCornerRadius),
					color = badgeContainerColor,
					contentColor = badgeContentColor
				) {
					Row(
						modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(6.dp)
					) {
						Box(
							modifier = Modifier
								.size(8.dp)
								.clip(RoundedCornerShape(indicatorCorner))
								.background(indicatorColor)
						)
						Text(
							text = formatRelativeDate(
								alarmStartMs = alarmData.startTime,
								todayStartMs = todayStartMs,
							),
							style = typography.labelMedium,
							color = badgeContentColor,
							fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(14.dp))

			// Time and Toggle Row
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.weight(1f)
				) {
					TimeDisplay(
						millis = alarmData.startTime,
						textStyle = typography.displaySmall,
						contentColor = contentColor,
						isActive = isActive,
						modifier = Modifier.sharedElement(
							sharedContentState = rememberSharedContentState(key = "start_time_${alarmData.id}"),
							animatedVisibilityScope = animatedVisibilityScope,
							boundsTransform = AlarmTimeBoundsTransform
						)
					)

					Surface(
						shape = RoundedCornerShape(10.dp),
						color = contentColor.copy(alpha = if (isActive) 0.12f else 0.06f),
						modifier = Modifier.padding(horizontal = 8.dp)
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowForward,
							contentDescription = null,
							modifier = Modifier
								.padding(6.dp)
								.size(18.dp)
								.sharedElement(
									sharedContentState = rememberSharedContentState(key = "arrow_${alarmData.id}"),
									animatedVisibilityScope = animatedVisibilityScope,
									boundsTransform = AlarmTimeBoundsTransform
								),
							tint = secondaryContentColor
						)
					}

					TimeDisplay(
						millis = alarmData.endTime,
						textStyle = typography.displaySmall,
						contentColor = contentColor,
						isActive = isActive,
						modifier = Modifier.sharedElement(
							sharedContentState = rememberSharedContentState(key = "end_time_${alarmData.id}"),
							animatedVisibilityScope = animatedVisibilityScope,
							boundsTransform = AlarmTimeBoundsTransform
						)
					)
				}

				Switch(
					checked = isActive,
					onCheckedChange = onToggle,
					thumbContent = if (isActive) {
						{
							Icon(
								imageVector = Icons.Default.Check,
								contentDescription = null,
								modifier = Modifier.size(SwitchDefaults.IconSize)
							)
						}
					} else null,
					colors = SwitchDefaults.colors(
						checkedThumbColor = colorScheme.onPrimary,
						checkedTrackColor = colorScheme.primary,
						checkedIconColor = colorScheme.primary,
						uncheckedThumbColor = colorScheme.outline,
						uncheckedTrackColor = colorScheme.surfaceContainerHighest
					)
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeDisplay(
	millis: Long,
	modifier: Modifier = Modifier,
	textStyle: TextStyle = typography.displaySmall,
	contentColor: Color,
	isActive: Boolean
) {
	val timeAlpha by animateFloatAsState(
		targetValue = if (isActive) 1f else 0.56f,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "timeAlpha"
	)
	val amPmAlpha by animateFloatAsState(
		targetValue = if (isActive) 0.78f else 0.50f,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "amPmAlpha"
	)

	val timeText by remember(millis) { mutableStateOf(formatTime12h(millis, "h:mm")) }
	val amPmText by remember(millis) { mutableStateOf(formatTime12h(millis, "a")) }

	Row(
		modifier = modifier,
		verticalAlignment = Alignment.Bottom
	) {
		Text(
			text = timeText,
			style = textStyle,
			fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
			color = contentColor,
			modifier = Modifier
				.alignByBaseline()
				.graphicsLayer { alpha = timeAlpha },
			softWrap = false,
			maxLines = 1
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = amPmText,
			style = typography.labelSmall,
			fontWeight = FontWeight.ExtraBold,
			color = contentColor,
			modifier = Modifier
				.alignByBaseline()
				.graphicsLayer { alpha = amPmAlpha },
			softWrap = false,
			maxLines = 1
		)
	}
}



fun formatTime12h(millis: Long, pattern: String ="h:mm" ): String {
	val formatter = SimpleDateFormat(pattern, Locale.getDefault())
	return formatter.format(Date(millis))
}

/**
 * Returns a human-friendly date label:
 *  - "Today" / "Yesterday" / "Tomorrow" when the alarm date matches those days
 *  - Full "EEEE, MMM d" string otherwise
 *
 * [todayStartMs] is the start-of-today in millis (computed once in AlarmContainer,
 * not per-card, to keep all cards in sync).
 */
@Composable
fun formatRelativeDate(
	alarmStartMs: Long,
	todayStartMs: Long,
): String {
	val zone = ZoneId.systemDefault()
	val alarmDate = Instant.ofEpochMilli(alarmStartMs).atZone(zone).toLocalDate()
	val todayDate = Instant.ofEpochMilli(todayStartMs).atZone(zone).toLocalDate()

	val labelToday = stringResource(R.string.date_label_today)
	val labelYesterday = stringResource(R.string.date_label_yesterday)
	val labelTomorrow = stringResource(R.string.date_label_tomorrow)

	return when (ChronoUnit.DAYS.between(todayDate, alarmDate)) {
		0L -> labelToday
		1L -> labelTomorrow
		-1L -> labelYesterday
		else -> alarmDate.format(
			DateTimeFormatter.ofPattern("EEEE, MMM d", LocalLocale.current.platformLocale)
		)
	}
}
