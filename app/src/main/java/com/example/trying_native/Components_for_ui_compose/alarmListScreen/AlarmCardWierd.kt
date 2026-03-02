package com.example.trying_native.Components_for_ui_compose.alarmListScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trying_native.dataBase.AlarmData
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun AlarmCardWierd(
	alarmData: AlarmData,
	onStop: () -> Unit = {},
	onSnooze: () -> Unit = {},
	onDelete: () -> Unit = {}
) {
	// Material 3 Expressive Colors - Modify these at the top
	val primaryColor = Color(0xFF00D9FF) // Cyan/Turquoise
	val backgroundColor = Color(0xFF0A0A0A) // Near black
	val surfaceColor = Color(0xFF1C1C1E) // Dark gray surface
	val onSurfaceColor = Color(0xFFE5E5E7) // Light gray text
	val onSurfaceVariant = Color(0xFF9E9E9E) // Muted gray
	val errorColor = Color(0xFFFF6B6B) // Red for delete

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(backgroundColor)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			// Date Display
			Text(
				text = formatDate(alarmData.startTime),
				color = onSurfaceVariant,
				fontSize = 14.sp,
				fontWeight = FontWeight.Medium,
				modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
			)

			// Time Range Display with Visual Connection
			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp),
				color = surfaceColor,
				shape = RoundedCornerShape(28.dp)
			) {
				Column(
					modifier = Modifier.padding(24.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					// Start Time
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = formatTime12Hour(alarmData.startTime),
							color = primaryColor,
							fontSize = 56.sp,
							fontWeight = FontWeight.Bold,
							letterSpacing = (-2).sp
						)
						Text(
							text = getAmPm(alarmData.startTime),
							color = primaryColor.copy(alpha = 0.7f),
							fontSize = 16.sp,
							fontWeight = FontWeight.SemiBold,
							modifier = Modifier.offset(y = (-8).dp)
						)
					}

					Spacer(modifier = Modifier.height(8.dp))

					// Visual Connector with Frequency
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.Center,
						modifier = Modifier.padding(vertical = 16.dp)
					) {
						// Left line
						Box(
							modifier = Modifier
								.width(40.dp)
								.height(2.dp)
								.background(primaryColor.copy(alpha = 0.5f))
						)

						// Frequency Indicator
						Surface(
							color = primaryColor.copy(alpha = 0.15f),
							shape = RoundedCornerShape(12.dp),
							modifier = Modifier.padding(horizontal = 12.dp)
						) {
							Row(
								modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									imageVector = Icons.Default.Notifications,
									contentDescription = null,
									tint = primaryColor,
									modifier = Modifier.size(16.dp)
								)
								Spacer(modifier = Modifier.width(8.dp))
								Text(
									text = formatFrequency(alarmData.frequencyInMin),
									color = primaryColor,
									fontSize = 14.sp,
									fontWeight = FontWeight.SemiBold
								)
							}
						}

						// Right line
						Box(
							modifier = Modifier
								.width(40.dp)
								.height(2.dp)
								.background(primaryColor.copy(alpha = 0.5f))
						)
					}

					Spacer(modifier = Modifier.height(8.dp))

					// End Time
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = formatTime12Hour(alarmData.endTime),
							color = onSurfaceColor,
							fontSize = 56.sp,
							fontWeight = FontWeight.Bold,
							letterSpacing = (-2).sp
						)
						Text(
							text = getAmPm(alarmData.endTime),
							color = onSurfaceColor.copy(alpha = 0.7f),
							fontSize = 16.sp,
							fontWeight = FontWeight.SemiBold,
							modifier = Modifier.offset(y = (-8).dp)
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(32.dp))

			// Message Display
			if (alarmData.message.isNotBlank()) {
				Text(
					text = alarmData.message,
					color = onSurfaceColor,
					fontSize = 16.sp,
					fontWeight = FontWeight.Normal,
					textAlign = TextAlign.Center,
					lineHeight = 24.sp,
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 32.dp)
				)
			}

			Spacer(modifier = Modifier.weight(1f))

			// Action Buttons
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 32.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				// Stop Button
				Button(
					onClick = onStop,
					modifier = Modifier
						.fillMaxWidth()
						.height(64.dp),
					colors = ButtonDefaults.buttonColors(
						containerColor = primaryColor
					),
					shape = RoundedCornerShape(32.dp)
				) {
					Text(
						text = "STOP",
						color = backgroundColor,
						fontSize = 18.sp,
						fontWeight = FontWeight.Bold,
						letterSpacing = 1.5.sp
					)
				}

				// Snooze and Delete Row
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(12.dp)
				) {
					// Snooze Button
					OutlinedButton(
						onClick = onSnooze,
						modifier = Modifier
							.weight(1f)
							.height(56.dp),
						colors = ButtonDefaults.outlinedButtonColors(
							contentColor = onSurfaceColor
						),
						border = BorderStroke(1.dp, onSurfaceVariant.copy(alpha = 0.3f)),
						shape = RoundedCornerShape(28.dp)
					) {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = "Snooze",
							modifier = Modifier.size(20.dp)
						)
						Spacer(modifier = Modifier.width(8.dp))
						Text(
							text = "Snooze",
							fontSize = 15.sp,
							fontWeight = FontWeight.SemiBold
						)
					}

					// Delete Button
					OutlinedButton(
						onClick = onDelete,
						modifier = Modifier
							.weight(1f)
							.height(56.dp),
						colors = ButtonDefaults.outlinedButtonColors(
							contentColor = errorColor
						),
						border = BorderStroke(1.dp, errorColor.copy(alpha = 0.3f)),
						shape = RoundedCornerShape(28.dp)
					) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = "Delete",
							modifier = Modifier.size(20.dp)
						)
						Spacer(modifier = Modifier.width(8.dp))
						Text(
							text = "Delete",
							fontSize = 15.sp,
							fontWeight = FontWeight.SemiBold
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(32.dp))
		}
	}
}

// Helper Functions
private fun formatTime12Hour(timeInMillis: Long): String {
	val calendar = Calendar.getInstance(Locale.getDefault()).apply {
		this.timeInMillis = timeInMillis
	}
	val hour = calendar.get(Calendar.HOUR)
	val minute = calendar.get(Calendar.MINUTE)
	val displayHour = if (hour == 0) 12 else hour
	return String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute)
}

private fun getAmPm(timeInMillis: Long): String {
	val calendar = Calendar.getInstance(Locale.getDefault()).apply {
		this.timeInMillis = timeInMillis
	}
	return if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
}

//private fun formatDate(dateInMillis: Long): String {
//	val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
//	return dateFormat.format(Date(dateInMillis))
//}

private fun formatFrequency(freqInMillis: Long): String {
	val minutes = TimeUnit.MILLISECONDS.toMinutes(freqInMillis)
	return when {
		minutes < 60 -> String.format(Locale.getDefault(), "Every %dm", minutes)
		minutes == 60L -> "Every hour"
		else -> {
			val hours = minutes / 60
			String.format(Locale.getDefault(), "Every %dh", hours)
		}
	}
}
