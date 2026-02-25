package com.example.trying_native.Components_for_ui_compose.alarmListScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.dataBase.AlarmData


// good pixel like ui for the alarm, the different reset, edit, delete button is good, but the color is bad
@Composable fun AlarmCard1(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit = {},
	onStop: (AlarmData) -> Unit = {},
	onReset: (AlarmData) -> Unit = {},
	onDelete: (AlarmData) -> Unit = {}
) {
	// State to handle the "Incremental" reveal of message and frequency
	var isExpanded by remember { mutableStateOf(false) }

	// M3 Expressive uses very large corner radii (28dp - 32dp)
	val cardPadding by animateDpAsState(if (isExpanded) 24.dp else 20.dp)

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 6.dp, horizontal = 12.dp)
			.clip(RoundedCornerShape(32.dp)) // Android 16 "Expressive" squarcle feel
			.animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy))
			.clickable { isExpanded = !isExpanded },
		colors = CardDefaults.cardColors(
			containerColor = if (alarmData.isReadyToUse)
				MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
			else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
		)
	) {
		Column(modifier = Modifier.padding(cardPadding)) {

			// --- TOP ROW: TIME & TOGGLE ---
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = formatTime(alarmData.startTime),
							style = MaterialTheme.typography.displayMedium.copy(
								fontWeight = FontWeight.SemiBold,
								letterSpacing = (-1).sp
							)
						)
						Text(
							text = " → ",
							style = MaterialTheme.typography.titleLarge,
							color = MaterialTheme.colorScheme.outline,
							modifier = Modifier.padding(horizontal = 4.dp)
						)
						Text(
							text = formatTime(alarmData.endTime),
							style = MaterialTheme.typography.headlineMedium.copy(
								color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
							)
						)
					}
				}

				// M3 Expressive Switch or Status Icon
				IconButton(
					onClick = { onDelete(alarmData) },
					modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
				) {
					Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error)
				}
			}

			// --- CHIPS ROW (Frequency) ---
			Row(
				modifier = Modifier.padding(top = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				// Frequency Chip
				AssistChip(
					onClick = { /* Detail */ },
					label = { Text("Every ${alarmData.frequencyInMin / 60000} mins") },
					shape = CircleShape,
					colors = AssistChipDefaults.assistChipColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
					),
					border = null
				)

				if (alarmData.isReadyToUse) {
					Badge(containerColor = Color(0xFF00E5FF)) {
						Text("ACTIVE", Modifier.padding(4.dp), fontWeight = FontWeight.Bold, color = Color.Black)
					}
				}
			}

			// --- EXPANDED SECTION: MESSAGE & ACTIONS ---
			if (isExpanded) {
				Spacer(modifier = Modifier.height(16.dp))
				HorizontalDivider(modifier = Modifier.alpha(0.2f))
				Spacer(modifier = Modifier.height(16.dp))

				// The Hidden Message (Revealed on tap)
				Text(
					text = alarmData.message.ifBlank { "No motivation set." },
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.fillMaxWidth()
				)

				Spacer(modifier = Modifier.height(20.dp))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(12.dp)
				) {
					// STOP / RESET Button (The Big Action)
					Button(
						onClick = { if (alarmData.isReadyToUse) onStop(alarmData) else onReset(alarmData) },
						modifier = Modifier.weight(1f).height(56.dp),
						shape = RoundedCornerShape(20.dp),
						colors = ButtonDefaults.buttonColors(
							containerColor = if (alarmData.isReadyToUse) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
						)
					) {
						Text(
							text = if (alarmData.isReadyToUse) "STOP" else "RESET",
							style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
						)
					}

					// EDIT Button (Secondary)
					FilledTonalButton(
						onClick = { onEdit(alarmData) },
						modifier = Modifier.height(56.dp),
						shape = RoundedCornerShape(20.dp)
					) {
						Icon(Icons.Default.Edit, contentDescription = null)
					}
				}
			}
		}
	}
}


fun formatTime(millis: Long): String {
	val date = java.util.Date(millis)
	val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
	return formatter.format(date)
}



// good pixel like ui for the alarm, but the no delete and edit button and only one button at the bottom
@Composable
fun AlarmCard2(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit = {},
	onStop: (AlarmData) -> Unit = {},
	onReset: (AlarmData) -> Unit = {},
	onDelete: (AlarmData) -> Unit = {}
) {
	// State to toggle the "Message" view on tap
	var isExpanded by remember { mutableStateOf(false) }

	// Animate the corner radius and height for that "Expressive" feel
	val animatedCornerRadius by animateDpAsState(targetValue = if (isExpanded) 16.dp else 32.dp)
	val cyanAccent = Color(0xFF00E5FF)

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp)
			.clip(RoundedCornerShape(animatedCornerRadius))
			.clickable { isExpanded = !isExpanded }, // Tap to reveal message
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
		)
	) {
		Column(modifier = Modifier.padding(24.dp)) {

			// TOP ROW: Frequency & Delete
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				// Frequency Chip (Pixel 16 style: pill-shaped, tonal)
				SuggestionChip(
					onClick = { /* No-op */ },
					label = {
						Text(
							"Every ${alarmData.frequencyInMin / 60000} mins",
							style = MaterialTheme.typography.labelSmall
						)
					},
					shape = CircleShape,
					border = null,
					colors = SuggestionChipDefaults.suggestionChipColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
					),
					icon = { Icon(Icons.Default.Repeat, "Freq", modifier = Modifier.size(14.dp)) }
				)

				IconButton(onClick = { onDelete(alarmData) }) {
					Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.outline)
				}
			}

			// MAIN TIME: Big, Bold, Expressive
			Row(
				modifier = Modifier.padding(vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = formatTime(alarmData.startTime),
					style = MaterialTheme.typography.displayLarge.copy(
						fontWeight = FontWeight.Black,
						letterSpacing = (-2).sp // Tight kerning for that Pixel look
					)
				)
				Icon(
					imageVector = Icons.AutoMirrored.Filled.ArrowForward,
					contentDescription = null,
					modifier = Modifier.padding(horizontal = 12.dp).size(28.dp),
					tint = cyanAccent
				)
				Text(
					text = formatTime(alarmData.endTime),
					style = MaterialTheme.typography.displayMedium.copy(
						fontWeight = FontWeight.Normal,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				)
			}

			// REVEALED MESSAGE (Only shows on tap)
			AnimatedVisibility(
				visible = isExpanded,
				enter = expandVertically() + fadeIn(),
				exit = shrinkVertically() + fadeOut()
			) {
				Column {
					HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
					Text(
						text = alarmData.message.ifEmpty { "No motivation set." },
						style = MaterialTheme.typography.bodyLarge,
						fontStyle = FontStyle.Italic,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					TextButton(
						onClick = { onEdit(alarmData) },
						modifier = Modifier.align(Alignment.End)
					) {
						Text("Edit Alarm", color = cyanAccent)
					}
				}
			}

			// BOTTOM ACTIONS: stop/reset
			Row(
				modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				val actionModifier = Modifier.weight(1f).height(56.dp)

				if (alarmData.isReadyToUse) {
					// STOP: High contrast, rounded square (Expressive style)
					Button(
						onClick = { onStop(alarmData) },
						modifier = actionModifier,
						colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black),
						shape = RoundedCornerShape(18.dp)
					) {
						Text("STOP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
					}
				} else {
					// RESET: Tonal, subtle
					FilledTonalButton(
						onClick = { onReset(alarmData) },
						modifier = actionModifier,
						shape = RoundedCornerShape(18.dp)
					) {
						Text("RESET CYCLE", fontWeight = FontWeight.Bold)
					}
				}
			}
		}
	}

}



// Reset Cycle one(changed it to reset and made both start and end time same)
@Composable
fun AlarmCard3(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit = {},
	onStop: (AlarmData) -> Unit = {},
	onReset: (AlarmData) -> Unit = {},
	onDelete: (AlarmData) -> Unit = {}
) {
	// State to toggle the "Message" view on tap
	var isExpanded by remember { mutableStateOf(false) }

	// Animate the corner radius and height for that "Expressive" feel
	val animatedCornerRadius by animateDpAsState(targetValue = if (isExpanded) 16.dp else 32.dp)
	val cyanAccent = Color(0xFF00E5FF)

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp)
			.clip(RoundedCornerShape(animatedCornerRadius))
			.clickable { isExpanded = !isExpanded }, // Tap to reveal message
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
		)
	) {
		Column(modifier = Modifier.padding(24.dp)) {
			// TOP ROW: Frequency & Delete
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				// Frequency Chip (Pixel 16 style: pill-shaped, tonal)
				SuggestionChip(
					onClick = { /* No-op */ },
					label = {
						Text(
							"Every ${alarmData.frequencyInMin / 60000} mins",
							style = MaterialTheme.typography.labelSmall
						)
					},
					shape = CircleShape,
					border = null,
					colors = SuggestionChipDefaults.suggestionChipColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
					),
					icon = { Icon(Icons.Default.Repeat, "Freq", modifier = Modifier.size(14.dp)) }
				)

				IconButton(onClick = { onDelete(alarmData) }) {
					Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.outline)
				}
			}
			// MAIN TIME: Big, Bold, Expressive
			Row(
				modifier = Modifier.padding(vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = formatTime(alarmData.startTime),
					style = MaterialTheme.typography.displayMedium.copy(
						fontWeight = FontWeight.Black,
						letterSpacing = (-2).sp // Tight kerning for that Pixel look
					)
				)
				Icon(
					imageVector = Icons.AutoMirrored.Filled.ArrowForward,
					contentDescription = null,
					modifier = Modifier.padding(horizontal = 12.dp).size(28.dp),
					tint = cyanAccent
				)
				Text(
					text = formatTime(alarmData.endTime),
					style = MaterialTheme.typography.displayMedium.copy(
						fontWeight = FontWeight.Black,
//						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				)
			}

			// REVEALED MESSAGE (Only shows on tap)
			AnimatedVisibility(
				visible = isExpanded,
				enter = expandVertically() + fadeIn(),
				exit = shrinkVertically() + fadeOut()
			) {
				Column {
					HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
					Text(
						text = alarmData.message.ifEmpty { " " },
						style = MaterialTheme.typography.bodyLarge,
						fontStyle = FontStyle.Italic,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					TextButton(
						onClick = { onEdit(alarmData) },
						modifier = Modifier.align(Alignment.End)
					) {
						Text("Edit Alarm", color = cyanAccent)
					}
				}
			}

			// BOTTOM ACTIONS: stop/reset
			Row(
				modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				val actionModifier = Modifier.weight(1f).height(56.dp)

				if (alarmData.isReadyToUse) {
					// STOP: High contrast, rounded square (Expressive style)
					Button(
						onClick = { onStop(alarmData) },
						modifier = actionModifier,
						colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black),
						shape = RoundedCornerShape(18.dp)
					) {
						Text("STOP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
					}
				} else {
					// RESET: Tonal, subtle
					FilledTonalButton(
						onClick = { onReset(alarmData) },
						modifier = actionModifier,
						shape = RoundedCornerShape(18.dp)
					) {
						Text("RESET", fontWeight = FontWeight.Bold)
					}
				}
			}
		}
	}
}

@Composable
fun AlarmCard4(
	alarmData: AlarmData,
	onEdit: (AlarmData) -> Unit = {},
	onStop: (AlarmData) -> Unit = {},
	onReset: (AlarmData) -> Unit = {},
	onDelete: (AlarmData) -> Unit = {}
) {
	var isExpanded by remember { mutableStateOf(false) }

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 10.dp)
			.animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
			.clip(RoundedCornerShape(32.dp))
			.clickable { isExpanded = !isExpanded },
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerLowest // Cleaner, lighter base
		),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
	) {
		Column(modifier = Modifier.padding(24.dp)) {
			// Top Row: Date & Frequency (The "Glance" Info)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = formatDate(alarmData.date).uppercase(),
					style = MaterialTheme.typography.labelLarge.copy(
						fontWeight = FontWeight.ExtraBold,
						letterSpacing = 2.sp
					),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				// Frequency Badge - Expressive Capsule
				Surface(
					shape = CircleShape,
					color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
				) {
					Text(
						text = "Repeat: ${alarmData.frequencyInMin }m",
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold
					)
				}
			}

			// Time Display: 12hr Format, Equal Weight
			Column(modifier = Modifier.padding(vertical = 16.dp)) {
				TimeRow(label = "START", time = formatTime12h(alarmData.startTime))
				Spacer(modifier = Modifier.height(8.dp))
				TimeRow(label = "END  ", time = formatTime12h(alarmData.endTime), isDimmed = true)
			}

			// Hidden Message Reveal
			AnimatedVisibility(visible = isExpanded) {
				Text(
					text = alarmData.message.ifEmpty { "No notes added" },
					modifier = Modifier.padding(bottom = 16.dp),
					style = MaterialTheme.typography.bodyLarge,
					fontStyle = FontStyle.Italic
				)
			}

			// Expressive Action Grid (Edit, Delete, and Main Action)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				// Main Action (Stop/Reset) - Largest Target
				Button(
					onClick = { if (alarmData.isReadyToUse) onStop(alarmData) else onReset(alarmData) },
					modifier = Modifier.weight(1.5f).height(64.dp),
					shape = RoundedCornerShape(24.dp),
					colors = ButtonDefaults.buttonColors(
						containerColor = if (alarmData.isReadyToUse) Color(0xFF00E5FF) else MaterialTheme.colorScheme.secondary,
						contentColor = Color.Black
					)
				) {
					Text(
						if (alarmData.isReadyToUse) "STOP" else "RESET",
						style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
					)
				}

				// Edit Button - Expressive Square-ish Rounding
				FilledTonalIconButton(
					onClick = { onEdit(alarmData) },
					modifier = Modifier.size(64.dp),
					shape = RoundedCornerShape(24.dp)
				) {
					Icon(Icons.Default.Edit, contentDescription = "Edit Alarm")
				}

				// Delete Button
				IconButton(
					onClick = { onDelete(alarmData) },
					modifier = Modifier.size(64.dp)
						.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
				) {
					Icon(Icons.Default.DeleteSweep, tint = MaterialTheme.colorScheme.error, contentDescription = "Delete Alarm")
				}
			}
		}
	}
}

@Composable
fun TimeRow4(label: String, time: String, isDimmed: Boolean = false) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			modifier = Modifier.width(45.dp),
			color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
		)
		Text(
			text = time,
			style = MaterialTheme.typography.displaySmall.copy(
				fontWeight = FontWeight.Black,
				fontSize = 32.sp
			),
			color = if (isDimmed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
		)
	}
}

// Helpers for 12hr format
fun formatTime12h4(millis: Long): String =
	java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(millis))

fun formatDate4(millis: Long): String =
	java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))


