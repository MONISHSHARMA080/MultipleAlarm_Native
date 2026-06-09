package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import android.app.TimePickerDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerEvent
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AlarmPickerScreen(
	alarm: AlarmData?,
	alarmSetGoBack: () -> Unit,
	onNavigateToSoundList: () -> Unit,
	viewModel: AlarmPickerViewModel
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val selectedAlarmSound by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val listOfAlarms by viewModel.listOfAlarms.collectAsStateWithLifecycle()
	val previewingSound by viewModel.previewingSound.collectAsStateWithLifecycle()

	val context = LocalContext.current
	LaunchedEffect(alarm) {
		viewModel.setInitialAlarmObject(alarm)
		viewModel.checkPermissions(context)
	}

	LaunchedEffect(Unit) {
		viewModel.events.collect { event ->
			when (event) {
				AlarmPickerEvent.NavigateBack -> alarmSetGoBack()
				is AlarmPickerEvent.ShowPermissionDialog -> {
					// keep your existing permission dialog handling here
				}
				AlarmPickerEvent.UpdateDataStoreGranted -> Unit
			}
		}
	}

	val alarmObject = uiState.alarmObject
	val isEditMode = uiState.initialAlarm != null
	val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
	val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

	val startTimeText = remember(alarmObject.startTime.timeInMillis) {
		timeFormat.format(alarmObject.startTime.time)
	}
	val endTimeText = remember(alarmObject.endTime.timeInMillis) {
		timeFormat.format(alarmObject.endTime.time)
	}
	val dateText = remember(alarmObject.date) {
		dateFormat.format(alarmObject.date)
	}

	val scrollState = rememberScrollState()

	Scaffold(
		topBar = {
			AlarmPickerTopBar(
				isEditMode = isEditMode,
				onBack = alarmSetGoBack,
				onDelete = {
					// hook your delete logic here if you want
				}
			)
		},
		bottomBar = {
			AlarmPickerBottomBar(
				isEditMode = isEditMode,
				onDelete = {
					// hook delete if needed
				},
				onSave = {
					viewModel.onSetAlarmClicked(uiState.initialAlarm, alarmObject)
				}
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(scrollState)
				.padding(horizontal = 20.dp)
				.padding(top = 8.dp, bottom = 24.dp)
				.animateContentSize(),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			AlarmHeroCard(
				startTimeText = startTimeText,
				endTimeText = endTimeText,
				dateText = dateText,
				onStartTimeClick = {
					showTimePicker(
						context = context,
						initialTime = alarmObject.startTime,
						onTimePicked = viewModel::updateStartTime
					)
				},
				onEndTimeClick = {
					showTimePicker(
						context = context,
						initialTime = alarmObject.endTime,
						onTimePicked = viewModel::updateEndTime
					)
				},
				onDateClick = {
					// hook your date picker here if you already have one
				}
			)

			SectionCard(title = "Alarm details", icon = Icons.Outlined.Alarm) {
				SettingRow(
					title = "Alarm name",
					value = alarmObject.message.ifBlank { "Alarm" },
					icon = Icons.Outlined.Edit,
					onClick = {
						// open your name input if needed
					}
				)

				Divider(modifier = Modifier.padding(start = 52.dp))

				SettingRow(
					title = "Sound",
					value = selectedAlarmSound?.title ?: "Default",
					icon = Icons.Outlined.Notifications,
					onClick = onNavigateToSoundList,
					trailingContent = {
						if (previewingSound != null) {
							AssistChip(
								onClick = { viewModel.stopPreview() },
								label = { Text("Stop") },
								leadingIcon = {
									Icon(Icons.Outlined.PlayArrow, contentDescription = null)
								}
							)
						}
					}
				)

				Divider(modifier = Modifier.padding(start = 52.dp))

				SettingRow(
					title = "Vibrate",
					value = if (alarmObject.alarmSoundUri != null) "On" else "Off",
					icon = Icons.Outlined.Notifications,
					onClick = {
						// wire vibrate toggle in your state if you have one
					},
					trailingContent = {
						Switch(
							checked = true,
							onCheckedChange = {
								// wire to your state
							}
						)
					}
				)
			}

			SectionCard(title = "Repeat", icon = Icons.Outlined.Schedule) {
				RepeatDaysRow(
					selectedDays = getSelectedDaysFromAlarm(alarmObject),
					onDayClick = { dayIndex ->
						// hook to your repeat/day logic
					}
				)
			}

			SectionCard(title = "More options", icon = Icons.Outlined.Tune) {
				SettingRow(
					title = "Schedule alarm",
					value = "Smart scheduling",
					icon = Icons.Outlined.CalendarMonth,
					onClick = {
						// navigate or open scheduling options
					}
				)

				Divider(modifier = Modifier.padding(start = 52.dp))

				SettingRow(
					title = "Snooze",
					value = "${alarmObject.freqGottenAfterCallback} min",
					icon = Icons.Outlined.Snooze,
					onClick = {
						// maybe expose increment/decrement UI
					},
					trailingContent = {
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							SmallIconButton(
								icon = Icons.AutoMirrored.Outlined.ArrowForward,
								onClick = { viewModel.incrementFrequency() }
							)
						}
					}
				)
			}

			if (uiState.validationResult is ValidationResult.Failure) {
				val error = uiState.validationResult as ValidationResult.Failure
				Text(
					text = error.message,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(horizontal = 4.dp)
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmPickerTopBar(
	isEditMode: Boolean,
	onBack: () -> Unit,
	onDelete: () -> Unit
) {
	CenterAlignedTopAppBar(
		title = {
			Text(
				text = if (isEditMode) "Edit alarm" else "New alarm",
				style = MaterialTheme.typography.titleLarge
			)
		},
		navigationIcon = {
			IconButton(onClick = onBack) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.ArrowForward,
					contentDescription = "Back"
				)
			}
		},
		actions = {
			if (isEditMode) {
				IconButton(onClick = onDelete) {
					Icon(
						imageVector = Icons.Outlined.DeleteOutline,
						contentDescription = "Delete alarm"
					)
				}
			}
		}
	)
}

@Composable
private fun AlarmHeroCard(
	startTimeText: String,
	endTimeText: String,
	dateText: String,
	onStartTimeClick: () -> Unit,
	onEndTimeClick: () -> Unit,
	onDateClick: () -> Unit
) {
	ElevatedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(32.dp),
		colors = CardDefaults.elevatedCardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerLow
		),
		elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Text(
				text = "Alarm time",
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)

			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				TimeHeroBlock(
					label = "Start",
					timeText = startTimeText,
					onClick = onStartTimeClick
				)

				Spacer(modifier = Modifier.width(8.dp))

				FilledIconButton(
					onClick = {},
					shape = CircleShape,
					colors = IconButtonDefaults.filledIconButtonColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						contentColor = MaterialTheme.colorScheme.onSecondaryContainer
					)
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
						contentDescription = null
					)
				}

				Spacer(modifier = Modifier.width(8.dp))

				TimeHeroBlock(
					label = "End",
					timeText = endTimeText,
					onClick = onEndTimeClick
				)
			}

			AssistChip(
				onClick = onDateClick,
				label = { Text(dateText) },
				leadingIcon = {
					Icon(
						imageVector = Icons.Outlined.CalendarMonth,
						contentDescription = null
					)
				}
			)
		}
	}
}

@Composable
private fun TimeHeroBlock(
	label: String,
	timeText: String,
	onClick: () -> Unit
) {
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(28.dp),
		color = MaterialTheme.colorScheme.surfaceContainerHigh,
		tonalElevation = 0.dp,
		modifier = Modifier
	) {
		Column(
			modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Text(
				text = timeText,
				style = MaterialTheme.typography.displaySmall
			)
		}
	}
}

@Composable
private fun SectionCard(
	title: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	content: @Composable ColumnScope.() -> Unit
) {
	ElevatedCard(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(28.dp),
		colors = CardDefaults.elevatedCardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainerLow
		),
		elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Surface(
					shape = CircleShape,
					color = MaterialTheme.colorScheme.primaryContainer,
					modifier = Modifier.size(36.dp)
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = icon,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.onPrimaryContainer
						)
					}
				}
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium
				)
			}

			content()
		}
	}
}

@Composable
private fun SettingRow(
	title: String,
	value: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	onClick: () -> Unit,
	trailingContent: @Composable (() -> Unit)? = null
) {
	Surface(
		onClick = onClick,
		color = Color.Transparent,
		modifier = Modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 6.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				shape = CircleShape,
				color = MaterialTheme.colorScheme.surfaceContainerHighest,
				modifier = Modifier.size(40.dp)
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = icon,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			Spacer(modifier = Modifier.width(12.dp))

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyLarge
				)
				Text(
					text = value,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}

			trailingContent?.invoke()
		}
	}
}

@Composable
private fun RepeatDaysRow(
	selectedDays: Set<Int>,
	onDayClick: (Int) -> Unit
) {
	val days = listOf("S", "M", "T", "W", "T", "F", "S")

	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		days.forEachIndexed { index, day ->
			val selected = selectedDays.contains(index)

			FilterChip(
				selected = selected,
				onClick = { onDayClick(index) },
				label = { Text(day) },
				modifier = Modifier.weight(1f),
				shape = RoundedCornerShape(14.dp)
			)
		}
	}
}

@Composable
private fun AlarmPickerBottomBar(
	isEditMode: Boolean,
	onDelete: () -> Unit,
	onSave: () -> Unit
) {
	Surface(
		tonalElevation = 0.dp,
		color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.navigationBarsPadding()
				.padding(16.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (isEditMode) {
				OutlinedButton(
					onClick = onDelete,
					modifier = Modifier.weight(1f),
					shape = RoundedCornerShape(18.dp)
				) {
					Text("Delete")
				}
			}

			Button(
				onClick = onSave,
				modifier = Modifier.weight(if (isEditMode) 1f else 1f),
				shape = RoundedCornerShape(18.dp)
			) {
				Text("Save")
			}
		}
	}
}

@Composable
private fun SmallIconButton(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	onClick: () -> Unit
) {
	FilledTonalIconButton(onClick = onClick) {
		Icon(imageVector = icon, contentDescription = null)
	}
}

private fun showTimePicker(
	context: android.content.Context,
	initialTime: Calendar,
	onTimePicked: (Calendar) -> Unit
) {
	val hour = initialTime.get(Calendar.HOUR_OF_DAY)
	val minute = initialTime.get(Calendar.MINUTE)

	TimePickerDialog(
		context,
		{ _, pickedHour, pickedMinute ->
			val newCalendar = (initialTime.clone() as Calendar).apply {
				set(Calendar.HOUR_OF_DAY, pickedHour)
				set(Calendar.MINUTE, pickedMinute)
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}
			onTimePicked(newCalendar)
		},
		hour,
		minute,
		false
	).show()
}

private fun getSelectedDaysFromAlarm(alarmObject: AlarmObject): Set<Int> {
	// Replace this with your real repeat-day source.
	return emptySet()
}
