package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPickerScreen(
	alarm: AlarmData?,
	alarmSetGoBack: () -> Unit,
	onNavigateToSoundList: () -> Unit,
	viewModel: AlarmPickerViewModel
) {
	val uiState by viewModel.uiState.collectAsState()
	val selectedSound by viewModel.selectedAlarmSound.collectAsState()

	LaunchedEffect(alarm) {
		viewModel.setInitialAlarmObject(alarm)
	}

	val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
	val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
	val startTime = uiState.alarmObject.startTime.time

	val cleanTextStyle = TextStyle(
		platformStyle = PlatformTextStyle(includeFontPadding = false)
	)
	val timeStyle = MaterialTheme.typography.displayLarge.copy(
		fontWeight = FontWeight.Light,
		lineHeight = MaterialTheme.typography.displayLarge.fontSize,
		platformStyle = PlatformTextStyle(includeFontPadding = false)
	)

	val amPmStyle = MaterialTheme.typography.labelLarge.copy(
		fontWeight = FontWeight.Normal,
		lineHeight = MaterialTheme.typography.labelLarge.fontSize,
		platformStyle = PlatformTextStyle(includeFontPadding = false)
	)


	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.statusBarsPadding()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 16.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = timeFormat.format(startTime),
				fontSize = 64.sp,
				lineHeight = 64.sp,
				fontWeight = FontWeight.Light,
				style = timeStyle,
				color = MaterialTheme.colorScheme.onBackground,
				modifier = Modifier.alignByBaseline()
			)

			Text(
				text = amPmFormat.format(startTime),
				fontSize = 20.sp,
				lineHeight = 20.sp,
				fontWeight = FontWeight.Normal,
				style = amPmStyle,
				color = MaterialTheme.colorScheme.onBackground,
				modifier = Modifier
					.alignByBaseline()
					.padding(start = 6.dp)
			)

			Spacer(modifier = Modifier.weight(1f))

			Surface(
				shape = CircleShape,
				color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
				modifier = Modifier.clickable { }
			) {
				Text(
					text = "Edit",
					fontSize = 14.sp,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		// 2. Arrow Controls

		Spacer(modifier = Modifier.height(16.dp))

		// 3. Days of the Week Selector
		val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			daysOfWeek.forEach { day ->
				Surface(
					shape = RoundedCornerShape(12.dp),
					color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
					modifier = Modifier
						.size(44.dp)
						.clickable { /* Toggle day logic */ }
				) {
					Box(contentAlignment = Alignment.Center) {
						Text(
							text = day,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							fontWeight = FontWeight.Medium
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.height(32.dp))

		// 4. Status and Schedule Header
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "Alarm is off",
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				fontSize = 15.sp
			)
			Spacer(modifier = Modifier.weight(1f))
			Icon(
				imageVector = Icons.Rounded.CalendarToday,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.size(18.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = "Schedule alarm",
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				fontSize = 15.sp,
				fontWeight = FontWeight.Medium
			)
		}

		Spacer(modifier = Modifier.height(16.dp))

		// 5. Settings Card (Name & Sound)
		Surface(
			shape = RoundedCornerShape(24.dp),
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
			modifier = Modifier.fillMaxWidth()
		) {
			Column {
				SettingRow(
					icon = Icons.Rounded.Label,
					title = "Alarm name",
					value = uiState.alarmObject.message.ifEmpty { "Alarm" },
					onClick = { /* Open Name Dialog */ }
				)

				HorizontalDivider(
					modifier = Modifier.padding(horizontal = 16.dp),
					color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
				)

				SettingRow(
					icon = Icons.Rounded.Notifications,
					title = "Sound",
					value = selectedSound?.title ?: "Default",
					onClick = onNavigateToSoundList
				)
			}
		}

		Spacer(modifier = Modifier.weight(1f))

		// 6. Bottom Actions (Delete & Save)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 16.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(
				onClick = { /* Handle delete logic */ }
			) {
				Text(
					text = "Delete",
					color = MaterialTheme.colorScheme.error,
					fontSize = 16.sp,
					fontWeight = FontWeight.Medium
				)
			}

			Button(
				onClick = {
					viewModel.onSetAlarmClicked(uiState.initialAlarm, uiState.alarmObject)
					alarmSetGoBack()
				},
				colors = ButtonDefaults.buttonColors(
					containerColor = Color(0xFF6ED0D8), // Matching the cyan accent from your screenshot
					contentColor = Color.Black
				),
				contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
			) {
				Text(
					text = "Save",
					fontSize = 16.sp,
					fontWeight = FontWeight.Medium
				)
			}
		}
	}
}

@Composable
private fun SettingRow(
	icon: ImageVector,
	title: String,
	value: String,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 20.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = title,
			color = MaterialTheme.colorScheme.onBackground,
			fontSize = 16.sp
		)
		Spacer(modifier = Modifier.weight(1f))
		Text(
			text = value,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			fontSize = 14.sp
		)
	}
}
