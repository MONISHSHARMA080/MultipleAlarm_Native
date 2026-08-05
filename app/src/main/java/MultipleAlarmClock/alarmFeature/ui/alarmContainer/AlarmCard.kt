package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmCard(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit,
	onToggle: (AlarmData, Boolean) -> Unit,
	onDelete: (AlarmData) -> Unit,
	modifier: Modifier = Modifier,
	onLongPress: (AlarmData) -> Unit = {}
) {
	val colorScheme = MaterialTheme.colorScheme
	val typography = MaterialTheme.typography

	val isActive = alarmData.isReadyToUse

	val dismissState = rememberSwipeToDismissBoxState()

	val cardShape = RoundedCornerShape(45.dp)

	// Determine padding based on active state to satisfy user requirement for different spacing
	val horizontalPadding = 10.dp
	val verticalPadding = if (isActive) 12.dp else 5.dp

	SwipeToDismissBox(
		state = dismissState,
		onDismiss = { _ ->
			onDelete(alarmData)
		},
		backgroundContent = {
			val color = when (dismissState.dismissDirection) {
				SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> colorScheme.errorContainer
				else -> Color.Transparent
			}
			val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

			Box(
				Modifier
					.fillMaxSize()
					.padding(horizontal = horizontalPadding, vertical = verticalPadding)
					.background(color, cardShape)
					.padding(horizontal = 24.dp)
				,
				contentAlignment = alignment
			) {
				if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
					Icon(
						Icons.Default.Delete,
						contentDescription = "Delete",
						tint = colorScheme.onErrorContainer
					)
				}
			}
		},
		modifier = modifier
	) {
		val containerColor = when {
			isActive -> colorScheme.primaryContainer
			else -> colorScheme.surfaceContainer
		}

		val contentColor = when {
			isActive -> colorScheme.onPrimaryContainer
			else -> colorScheme.onSurface
		}

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
					.padding(horizontal = horizontalPadding, vertical = verticalPadding)
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
					// Date
					Text(
						text = formatDate(alarmData.startTime),
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
								modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
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
							onCheckedChange = { onToggle(alarmData, it) },
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
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.Bottom
	) {
		Text(
			text = formatTime12h(millis, "h:mm"),
			style = textStyle,
			fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
			color = if (isActive)contentColor else contentColor.copy(alpha = 0.8f),
			modifier = Modifier.alignByBaseline()
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = formatTime12h(millis, "a"),
			style = typography.labelSmall,
			fontWeight = FontWeight.Bold,
			color = contentColor.copy(alpha = 0.8f),
			modifier = Modifier.alignByBaseline()
		)
	}
}


fun formatTime12h(millis: Long, pattern: String ="h:mm" ): String {
	val formatter = SimpleDateFormat(pattern, Locale.getDefault())
	return formatter.format(Date(millis))
}

fun formatDate(millis: Long): String =
	SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(millis))
