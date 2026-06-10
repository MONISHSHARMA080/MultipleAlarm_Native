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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
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
	val endTime = uiState.alarmObject.endTime.time

	val timeStyle = MaterialTheme.typography.displayLarge.copy(
		fontWeight = FontWeight.Bold,
		lineHeight = MaterialTheme.typography.displayLarge.fontSize,
	)

	val amPmStyle = MaterialTheme.typography.labelLarge.copy(
		fontWeight = FontWeight.Medium,
		lineHeight = MaterialTheme.typography.labelLarge.fontSize,
	)
	Scaffold { screenPadding->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
				.padding(screenPadding)
				.padding(horizontal = 12.dp)
		) {
			// startTime -> endTime
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Row(
					modifier = Modifier.weight(1f),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = timeFormat.format(startTime),
						style = timeStyle,
						color = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier.alignByBaseline()
					)

					Text(
						text = amPmFormat.format(startTime),
						style = amPmStyle,
						color = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.alignByBaseline()
							.padding(start = 6.dp)
					)
				}

				Icon(
					imageVector = Icons.AutoMirrored.Filled.ArrowForward,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onBackground,
					modifier = Modifier.padding(horizontal = 12.dp).size(30.dp)
				)

				Row(
					modifier = Modifier.weight(1f),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.End
				) {
					Text(
						text = timeFormat.format(endTime),
						style = timeStyle,
						color = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier.alignByBaseline()
					)

					Text(
						text = amPmFormat.format(endTime),
						style = amPmStyle,
						color = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.alignByBaseline()
							.padding(start = 6.dp)
					)
				}
			}

			Spacer(modifier = Modifier.height(20.dp))

			// 3. Days of the Week Selector
			val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				daysOfWeek.forEach { day ->
					Surface(
						shape = RoundedCornerShape(12.dp),
						color = MaterialTheme.colorScheme.surfaceVariant,
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


			Spacer(modifier = Modifier.height(16.dp))

			// 5. Settings Card (Name & Sound)
			Surface(
				shape = RoundedCornerShape(25.dp),
				color = MaterialTheme.colorScheme.surfaceVariant,
				modifier = Modifier.fillMaxWidth()
			) {
				Column {
					SettingRow(
						icon = Icons.AutoMirrored.Rounded.Label,
						title = "Alarm name",
						value = uiState.alarmObject.message.ifEmpty { "Alarm" },
						onClick = { /* Open Name Dialog */ }
					)

					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = MaterialTheme.colorScheme.outlineVariant,
					)

					SettingRow(
						icon = Icons.Rounded.Notifications,
						title = "Sound",
						value = selectedSound?.title ?: "Random",
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
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
