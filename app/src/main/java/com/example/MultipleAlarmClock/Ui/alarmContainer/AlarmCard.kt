package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.posthog.android.replay.PostHogMaskModifier.postHogMask

@Composable fun AlarmCard(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit,
	onStop: (AlarmData) -> Unit,
	onReset: (AlarmData) -> Unit,
	onDelete: (AlarmData) -> Unit,
	modifier: Modifier,
	onLongPress: (AlarmData) -> Unit
) {
	val colorScheme = MaterialTheme.colorScheme

	var isExpanded by remember { mutableStateOf(false) }
	val buttonColor by animateColorAsState(
		targetValue = if (alarmData.isReadyToUse) colorScheme.primaryContainer else colorScheme.surfaceVariant,
		animationSpec = tween(durationMillis = 155),
	)
	val buttonContentColor by animateColorAsState(
		targetValue = if (alarmData.isReadyToUse) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
		animationSpec = tween(durationMillis = 155),
	)

	Card(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp)
			// M3 Expressive: cards use ExtraLarge shape (28.dp) at the container level;
			// the clip is needed to honour the shape on combinedClickable ripple
			.clip(RoundedCornerShape(28.dp))
			.combinedClickable(
				onClick = { if (alarmData.message.isNotEmpty()) isExpanded = !isExpanded },
				onLongClick = { onLongPress(alarmData) }
			),
		shape = RoundedCornerShape(28.dp),
		// M3: surfaceContainerHigh is correct for elevated cards — keep as-is
		colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
		// M3 Expressive: cards carry a subtle tonal elevation to lift them from the background
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(24.dp)) {

			// ── Header row ────────────────────────────────────────────────
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
					Text(
						text = formatDate(alarmData.startTime),
						// M3: date label → titleSmall. Avoid hardcoded sp overrides;
						// titleSmall is already ~14 sp and semibold in the M3 type scale.
						style = MaterialTheme.typography.titleSmall,
						color = colorScheme.primary
					)
					Text(
						text = "Every ${alarmData.frequencyInMin} mins",
						// bodySmall is correct per M3 for supporting/secondary labels
						style = MaterialTheme.typography.bodySmall,
						color = colorScheme.onSurfaceVariant
					)
				}

				// M3: icon-only action on a card → use surfaceContainerHighest tonal fill,
				// NOT surfaceVariant (deprecated in M3 colour system)
				IconButton(
					onClick = { onEdit(alarmData) },
					modifier = Modifier
						.postHogMask()
						.background(colorScheme.surfaceContainerHighest, CircleShape)
						.size(40.dp)  // M3 standard touch-target for icon buttons
				) {
					Icon(
						imageVector = Icons.Default.Edit,
						contentDescription = "Edit Alarm",
						// Keep icon intrinsic size — avoid hardcoding dp here
						tint = colorScheme.onSurfaceVariant
					)
				}
			}

			// ── Time range display ─────────────────────────────────────────
			// M3 / Google Clock: large time display sits in its own visual block
			// with generous breathing room above the action row
			Column(modifier = Modifier.padding(vertical = 20.dp)) {
				Row(modifier = Modifier.fillMaxWidth()) {

					// START TIME — hour:minute
					Text(
						text = formatTime12h(alarmData.startTime),
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						letterSpacing = (-2).sp,
						style = MaterialTheme.typography.displayMedium,
						modifier = Modifier.alignByBaseline()
					)
					// AM/PM — M3 Clock pairs this with titleMedium, not bodySmall,
					// so the suffix feels intentional alongside displayMedium numerals
					Text(
						text = formatTime12h(alarmData.startTime, "a"),
						color = colorScheme.onSurfaceVariant,   // dimmed per M3 Clock pattern
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier
							.padding(start = 4.dp, end = 4.dp)
							.alignByBaseline()
					)

					// M3: use outline (not outlineVariant) for directional icons between
					// two content blocks — outlineVariant is for decorative dividers only
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowForward,
						contentDescription = null,
						modifier = Modifier
							.padding(horizontal = 8.dp)
							.align(Alignment.CenterVertically),
						tint = colorScheme.outline
					)

					// END TIME — hour:minute
					Text(
						text = formatTime12h(alarmData.endTime),
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						letterSpacing = (-2).sp,
						style = MaterialTheme.typography.displayMedium,
						modifier = Modifier.alignByBaseline()
					)
					Text(
						text = formatTime12h(alarmData.endTime, "a"),
						color = colorScheme.onSurfaceVariant,   // consistent with start AM/PM
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier
							.padding(start = 4.dp)
							.alignByBaseline()
					)
				}
			}

			// ── Expandable message ─────────────────────────────────────────
			AnimatedVisibility(visible = isExpanded) {
				Text(
					text = alarmData.message,
					modifier = Modifier.padding(bottom = 16.dp),
					color = colorScheme.onSurfaceVariant, // M3: secondary content → onSurfaceVariant
					style = MaterialTheme.typography.bodyLarge,
					fontStyle = if (alarmData.message.isEmpty()) FontStyle.Italic else FontStyle.Normal
				)
			}

			// ── Action row ────────────────────────────────────────────────
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Button(
					onClick = { if (alarmData.isReadyToUse) onStop(alarmData) else onReset(alarmData) },
					modifier = Modifier
						.weight(1f)
						.height(56.dp),   // M3 standard button height (48–56 dp range)
					shape = RoundedCornerShape(16.dp), // M3 Expressive: buttons inside cards use Medium shape
					colors = ButtonDefaults.buttonColors(
						containerColor = buttonColor,
						contentColor = buttonContentColor
					)
				) {
					AnimatedContent(
						targetState = if (alarmData.isReadyToUse) "Stop" else "Reset",
						transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
					) { text ->
						Text(
							text = text,
							// M3: primary button label → labelLarge (not titleMedium + manual weight)
							style = MaterialTheme.typography.labelLarge
						)
					}
				}

				// Delete — errorContainer fill is correct per M3 colour roles
				FilledIconButton(
					onClick = { onDelete(alarmData) },
					modifier = Modifier.size(56.dp),  // matched to button height above
					shape = RoundedCornerShape(16.dp),
					colors = IconButtonDefaults.filledIconButtonColors(
						containerColor = colorScheme.errorContainer,
						contentColor = colorScheme.onErrorContainer
					)
				) {
					Icon(Icons.Default.Delete, contentDescription = "Delete")
				}
			}
		}
	}
}

@Composable
fun TimeRow(label: String, time: String, isDimmed: Boolean = false) {
	val colorScheme = MaterialTheme.colorScheme
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			modifier = Modifier.width(45.dp),
			color = colorScheme.onSurfaceVariant
		)
		Text(
			text = time,
			style = MaterialTheme.typography.displaySmall.copy(
				fontWeight = FontWeight.Black,
				fontSize = 32.sp
			),
			color = if (isDimmed) colorScheme.onSurfaceVariant else colorScheme.onSurface
		)
	}
}

fun formatTime12h(millis: Long, pattern: String ="h:mm " ): String {
	val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
	return formatter.format(java.util.Date(millis))
}

fun formatDate(millis: Long): String =
	java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))
