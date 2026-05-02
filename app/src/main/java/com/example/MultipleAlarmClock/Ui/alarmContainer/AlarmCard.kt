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

@Composable fun AlarmCard(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit ,
	onStop: (AlarmData) -> Unit ,
	onReset: (AlarmData) -> Unit ,
	onDelete: (AlarmData) -> Unit,
	modifier: Modifier,
	onLongPress: (AlarmData) -> Unit
) {
	val colorScheme = MaterialTheme.colorScheme
	var isExpanded by remember { mutableStateOf(false) }
	val buttonColor by animateColorAsState(
		targetValue = if (alarmData.isReadyToUse) colorScheme.primary else colorScheme.secondary,
		animationSpec = tween(durationMillis = 155),
	)
	val buttonContentColor by animateColorAsState(
		targetValue = if (alarmData.isReadyToUse) colorScheme.onPrimary else colorScheme.onSecondary,
		animationSpec = tween(durationMillis = 155),
	)

	Card(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp)
			.clip(RoundedCornerShape(32.dp))
			.combinedClickable(
				onClick = {
					if (alarmData.message.isNotEmpty())isExpanded = !isExpanded
				},
				onLongClick = { onLongPress(alarmData) }
			),
		colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh)
	) {
		Column(modifier = Modifier.padding(24.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column {
					Text(
						text =formatDate(alarmData.startTime) ,
						style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
						fontSize = 18.sp,
						color = colorScheme.primary
					)
					Text(
						text = "Every ${alarmData.frequencyInMin} mins",
						style = MaterialTheme.typography.bodySmall,
						fontSize = 11.sp,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				IconButton(
					onClick = { onEdit(alarmData) },
					modifier = Modifier
						.background(colorScheme.secondaryContainer, CircleShape)
						.size(43.dp)
				) {
					Icon(
						imageVector = Icons.Default.Edit,
						contentDescription = "Edit Alarm",
						modifier = Modifier.size(20.dp),
						tint = colorScheme.onSecondaryContainer
					)
				}
			}
			Column(modifier = Modifier.padding(vertical = 16.dp)) {
				Row(
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(
						text = formatTime12h(alarmData.startTime),
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						letterSpacing = (-2).sp,
						style = MaterialTheme.typography.displayMedium,
						modifier = Modifier.alignByBaseline() // Anchors the "floor"
					)
					Text(
						text = formatTime12h(alarmData.startTime, "a"),
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(start = 1.dp).alignByBaseline()
					)

					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowForward,
						contentDescription = null,
						modifier = Modifier.padding(horizontal = 10.dp).size(34.dp).align(Alignment.CenterVertically),
						tint = colorScheme.outlineVariant
					)
					// END TIME (Repeat the logic)
					Text(
						text = formatTime12h(alarmData.startTime), // Assuming this is your end time logic
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						letterSpacing = (-2).sp,
						style = MaterialTheme.typography.displayMedium,
						modifier = Modifier.alignByBaseline()
					)
					Text(
						text = formatTime12h(alarmData.startTime, "a"),
						color = colorScheme.onSurface,
						fontWeight = FontWeight.Medium,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(start = 1.dp).alignByBaseline()
					)
				}
			}
			AnimatedVisibility(visible = isExpanded) {
				Text(
					text = alarmData.message,
					modifier = Modifier.padding(bottom = 16.dp),
					color = colorScheme.onSurface,
					style = MaterialTheme.typography.bodyLarge,
					fontStyle = if(alarmData.message.isEmpty()) FontStyle.Italic else FontStyle.Normal
				)
			}
			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Button(
					onClick = { if (alarmData.isReadyToUse) onStop(alarmData) else onReset(alarmData) },
					modifier = Modifier.weight(1f).height(64.dp),
					shape = RoundedCornerShape(24.dp),
					colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = buttonContentColor)
				) {
					AnimatedContent(
						targetState = if (alarmData.isReadyToUse) "Stop" else "Reset",
						transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
					) { text ->
						Text(
							text = text,
							style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
						)
					}
				}
				FilledIconButton(
					onClick = { onDelete(alarmData) },
					modifier = Modifier.size(64.dp),
					shape = RoundedCornerShape(24.dp),
					colors = IconButtonDefaults.filledIconButtonColors(
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentColor = MaterialTheme.colorScheme.onErrorContainer
					)
				) {
					Icon(Icons.Default.Delete, contentDescription = "Delete")
				}
			}
		}
	}
}

fun formatTime12h(millis: Long, pattern: String ="h:mm " ): String {
	val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
	return formatter.format(java.util.Date(millis))
}

fun formatDate(millis: Long): String =
	java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))