package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun AlarmCard(
	alarmData: AlarmData,
	todayStartMs: Long,
	onEdit: (AlarmData) -> Unit,
	onToggle: (AlarmData, Boolean) -> Unit,
	onDelete: (AlarmData) -> Unit,
	modifier: Modifier = Modifier,
	onLongPress: (AlarmData) -> Unit = {}
) {
	val colorScheme = colorScheme
	val typography = typography

	val isActive = alarmData.isReadyToUse

	val containerColor by animateColorAsState(
		targetValue = if (isActive) colorScheme.primaryContainer else colorScheme.surfaceContainer,
		label = "containerColor"
	)

	val contentColor by animateColorAsState(
		targetValue = if (isActive) colorScheme.onPrimaryContainer else colorScheme.onSurface,
		label = "contentColor"
	)

	val animatedVerticalPadding by animateDpAsState(
		targetValue = if (isActive) 6.dp else 4.dp,
		label = "verticalPadding"
	)
	val view = LocalView.current
	val dismissState = rememberSwipeToDismissBoxState()
	val cardShape = RoundedCornerShape(45.dp)
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
			val chipWidth = with(density) { abs(rawOffset).toDp() }.coerceAtMost(600.dp)

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
						val iconVisible = chipWidth > 48.dp
						val iconAlpha by animateFloatAsState(if (iconVisible) 1f else 0f, label = "iconAlpha")
						Icon(
							Icons.Default.Delete,
							contentDescription = "Delete",
							tint = colorScheme.onErrorContainer,
							modifier = Modifier.alpha(iconAlpha)
						)
					}
				}
			}
		},
		modifier = modifier.fillMaxWidth()
	) {
		val secondaryContentColor = contentColor.copy(alpha = 0.7f)

		// 🔑 Lock font scale to ensure the card looks identical on all devices as per AGENTS.md
		CompositionLocalProvider(
			LocalDensity provides Density(
				density = LocalDensity.current.density,
				fontScale = 1f
			)
		) {
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = horizontalPadding, vertical = animatedVerticalPadding)
					.combinedClickable(
						onClick = { onEdit(alarmData) },
						onLongClick = { onLongPress(alarmData) }
					),
				colors = CardDefaults.cardColors(
					containerColor = containerColor,
					contentColor = contentColor
				),
				shape = cardShape,
				elevation = CardDefaults.cardElevation()
			) {
				Column(
					modifier = Modifier
						.padding(24.dp)
						.fillMaxWidth()
				) {
					Text(
						text = formatRelativeDate(
							alarmStartMs = alarmData.startTime,
							todayStartMs = todayStartMs,
						),
						style = typography.labelMedium,
						color = secondaryContentColor,
						fontWeight = FontWeight.Medium
					)

					Spacer(modifier = Modifier.height(10.dp))

					// Conditional styling for time display to satisfy emphasis requirements

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
								contentColor = contentColor, isActive = isActive
							)
							Icon(
								imageVector = Icons.AutoMirrored.Filled.ArrowForward,
								contentDescription = null,
								modifier = Modifier.padding(horizontal = 8.dp).size(20.dp),
								tint = secondaryContentColor
							)
							TimeDisplay(
								millis = alarmData.endTime,
								textStyle = typography.displaySmall,
								contentColor = contentColor, isActive = isActive
							)
						}

						Switch(
							checked = isActive,
							onCheckedChange = { value->
								onToggle(alarmData, value)
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
									if (value){
										view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
									}else{
										view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
									}
								}
							},
							colors = SwitchDefaults.colors(
								checkedThumbColor = colorScheme.onPrimary,
								checkedTrackColor = colorScheme.primary,
								uncheckedThumbColor = colorScheme.outline,
								uncheckedTrackColor = colorScheme.surfaceContainerHighest
							)
						)
					}
				}
			}
		}
	}
}

@Composable
private fun TimeDisplay(
	millis: Long,
	modifier: Modifier = Modifier,
	textStyle: TextStyle = typography.displaySmall ,
	contentColor: Color,
	isActive: Boolean
) {
	val timeAlpha by animateFloatAsState(
		targetValue = if (isActive) 1f else 0.54f,
		label = "timeAlpha"
	)
	val amPmAlpha by animateFloatAsState(
		targetValue = if (isActive) 0.7f else 0.50f,
		label = "amPmAlpha"
	)

	Row(
		modifier = modifier,
		verticalAlignment = Alignment.Bottom
	) {
		Text(
			text = formatTime12h(millis, "h:mm"),
			style = textStyle,
			fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
			color = contentColor.copy(alpha = timeAlpha),
			modifier = Modifier.alignByBaseline(),
			softWrap = false,
			maxLines = 1
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = formatTime12h(millis, "a"),
			style = typography.labelSmall,
			fontWeight = FontWeight.Bold,
			color = contentColor.copy(alpha = amPmAlpha),
			modifier = Modifier.alignByBaseline(),
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
