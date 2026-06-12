package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerUiState
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPickerScreen(
	alarmSetGoBack: () -> Unit,
	onNavigateToSoundList: () -> Unit,
	viewModel: AlarmPickerViewModel
) {
	val uiState by viewModel.uiState.collectAsState()
	val selectedSound by viewModel.selectedAlarmSound.collectAsState()

	val currentError by remember(uiState) { mutableStateOf(  uiState.validationResult as? ValidationResult.Failure) }
	val view = LocalView.current
	val timeStyle = MaterialTheme.typography.headlineSmall

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(if (uiState.initialAlarm == null) "Set alarm" else "Edit alarm" ,
						style = timeStyle,
						color = MaterialTheme.colorScheme.onBackground,
						maxLines = 1,
						softWrap = false,

					)
				},
				navigationIcon = {
					IconButton(onClick = alarmSetGoBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = "Back"
						)
					}
				},
			)
		}
	) { screenPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
				.padding(screenPadding)
				.padding(horizontal = 6.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Spacer(modifier = Modifier.weight(0.6f))

			TimeRow(uiState, {viewModel.updateStartTime(it)}, {viewModel.updateEndTime(it)})

			Spacer(modifier = Modifier.weight(0.6f))

			DateList(
				{ viewModel.updateDate(it)}, uiState.alarmObject.startTime.time.time,
				weGood = currentError?.field != AlarmErrorField.DATE,
				allowSelectingPastDate = false,
			)

			Spacer(modifier = Modifier.weight(0.4f))

			// 5. Settings Card (Name & Sound)
			Surface(
				shape = RoundedCornerShape(28.dp),
				color = MaterialTheme.colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				Column {

					FrequencyRow(
						icon = Icons.Rounded.Timer,
						title = "repeat every",
						value = uiState.alarmObject.freqGottenAfterCallback,
						onValueChange = { newValue ->
							newValue.let {
								logD("new freq: value is $it")
								if (it in 0..<720){
									viewModel.updateFrequency(it)
								}
							}
						},
						previewText = viewModel.getFrequencyPreviewText(),
						validationResult = currentError,
					)
					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = MaterialTheme.colorScheme.outlineVariant,
					)

					SettingRow(
						icon = Icons.Rounded.Notifications,
						title = "sound",
						value = selectedSound?.title ?: "Random",
						onClick = onNavigateToSoundList
					)

					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = MaterialTheme.colorScheme.outlineVariant,
					)

					MessageRow(
						icon = Icons.AutoMirrored.Rounded.Message,
						title = "message",
						value = uiState.alarmObject.message,
						onValueChange = { viewModel.updateMessage(it) },
						validationResult = currentError
					)


				}
			}

			Spacer(modifier = Modifier.weight(1f))

			// 6. Bottom Actions (Delete & Save)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 18.dp),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				Button(
					onClick = {
						viewModel.onSetAlarmClicked(uiState.initialAlarm, uiState.alarmObject)
						view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
						alarmSetGoBack()
					},
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
					),
					contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
				) {
					Text(
						text = "Set alarm",
						style = MaterialTheme.typography.bodyLarge ,
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRow(
	uiState: AlarmPickerUiState,
	onStartTimeChange: (Calendar) -> Unit,
	onEndTimeChange: (Calendar) -> Unit
) {
	val startTime = uiState.alarmObject.startTime
	val endTime = uiState.alarmObject.endTime

	val timeStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
	val amPmStyle = MaterialTheme.typography.labelLarge

	val configuration = LocalWindowInfo.current.containerSize
	// Calculate title spacing adaptively based on screen height
	val titleSpacing = (configuration.height.dp * 0.04f).coerceIn(12.dp, 36.dp)

	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker by remember { mutableStateOf(false) }

	if (showStartTimePicker) {
		val timePickerState = rememberTimePickerState(
			initialHour = startTime.get(Calendar.HOUR_OF_DAY),
			initialMinute = startTime.get(Calendar.MINUTE),
			is24Hour = false
		)
		TimePickerDialog(
			onDismissRequest = { showStartTimePicker = false },
			confirmButton = {
				TextButton(onClick = {
					val newTime = (startTime.clone() as Calendar).apply {
						set(Calendar.HOUR_OF_DAY, timePickerState.hour)
						set(Calendar.MINUTE, timePickerState.minute)
					}
					onStartTimeChange(newTime)
					showStartTimePicker = false
				}) { Text("OK") }
			},
			dismissButton = {
				TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
			}, title = {
				Column {
					Text(
						text = "Select start time",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1,
						softWrap = false,
					)
					Spacer(modifier = Modifier.height(titleSpacing))
				}
			}
		) {
			TimePicker(state = timePickerState)
		}
	}

	if (showEndTimePicker) {
		val endTimePickerState = rememberTimePickerState(
			initialHour = endTime.get(Calendar.HOUR_OF_DAY),
			initialMinute = endTime.get(Calendar.MINUTE),
			is24Hour = false
		)
		TimePickerDialog(
			onDismissRequest = { showEndTimePicker = false },
			confirmButton = {
				TextButton(onClick = {
					val newTime = (endTime.clone() as Calendar).apply {
						set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
						set(Calendar.MINUTE, endTimePickerState.minute)
					}
					onEndTimeChange(newTime)
					showEndTimePicker = false
				}) { Text("OK") }
			},
			dismissButton = {
				TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
			}, title = {
				Column {
					Text(
						text = "Select end time",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1,
						softWrap = false,
					)
					Spacer(modifier = Modifier.height(titleSpacing))
				}
			}
		) {
			TimePicker(state = endTimePickerState)
		}
	}

	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier.weight(1f).clickable{showStartTimePicker = !showStartTimePicker},
			horizontalArrangement = Arrangement.Start
		) {
			Text(
				text = SimpleDateFormat("h:mm ", LocalLocale.current.platformLocale).format(startTime.time),
				style = timeStyle,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
			Text(
				text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(startTime.time),
				style = amPmStyle,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
		}
		Icon(
			imageVector = Icons.AutoMirrored.Filled.ArrowForward,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onBackground,
			modifier = Modifier.size(32.dp)
		)
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier.weight(1f).clickable{showEndTimePicker = !showEndTimePicker},
			horizontalArrangement = Arrangement.End
		) {
			Text(
				text = SimpleDateFormat("h:mm", LocalLocale.current.platformLocale).format(endTime.time),
				style = timeStyle,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
			Text(
				text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(endTime.time),
				style = amPmStyle,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
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

@Composable
private fun MessageRow(
	icon: ImageVector,
	title: String,
	value: String,
	onValueChange: (String) -> Unit,
	validationResult: ValidationResult.Failure? = null
) {
	val colorScheme = MaterialTheme.colorScheme
	val doWeHaveError = validationResult?.field == AlarmErrorField.MESSAGE

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = if (doWeHaveError) colorScheme.error else colorScheme.onSurfaceVariant,
				modifier = Modifier.size(20.dp)
			)
			Spacer(modifier = Modifier.width(12.dp))
			Text(
				text = title,
				color = if (doWeHaveError) colorScheme.error else colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.labelLarge,
				modifier = Modifier.weight(1f)
			)
		}

		Spacer(modifier = Modifier.height(8.dp))

		TextField(
			value = value,
			onValueChange = onValueChange,
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 32.dp),
			textStyle = MaterialTheme.typography.bodyMedium,
			placeholder = {
				Text(
					text = "Add message...",
					style = MaterialTheme.typography.bodyMedium,
					color = colorScheme.onSurfaceVariant
				)
			},
			minLines = 1, // Support 2-3 lines comfortably
			maxLines = 3,
			shape = RoundedCornerShape(20.dp),
			colors = TextFieldDefaults.colors(
				focusedIndicatorColor = Color.Transparent,
				unfocusedIndicatorColor = Color.Transparent,
				disabledIndicatorColor = Color.Transparent,
				errorIndicatorColor = Color.Transparent
			)
		)

		if (doWeHaveError) {
			Spacer(Modifier.padding(3.dp))
			Text(
				text = validationResult.message,
				color = colorScheme.error,
				style = MaterialTheme.typography.labelSmall,
				modifier = Modifier.padding(start = 32.dp, top = 4.dp)
			)
		}
	}
}

@Composable
private fun FrequencyRow(
	icon: ImageVector,
	title: String,
	value: Long,
	onValueChange: (Long) -> Unit,
	previewText: String = "",
	validationResult: ValidationResult.Failure?
) {
	val colorScheme = MaterialTheme.colorScheme
	val doWeHaveError =validationResult?.field == AlarmErrorField.FREQUENCY
	val view = LocalView.current

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
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
				fontSize = 16.sp,
				modifier = Modifier.weight(1f)
			)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.background(
						color = if (doWeHaveError) colorScheme.errorContainer else colorScheme.secondaryContainer ,
						shape = RoundedCornerShape(12.dp)
					)
					.padding(4.dp)
			) {
				IconButton(
					onClick = {
						if (value - 1 > 0) {
							onValueChange(value - 1)
						} else {
							// Semantic "Expressive" Reject haptic for limit reached (Android 14/15+)
							view.performHapticFeedback(HapticFeedbackConstants.REJECT)
						}
					},
					modifier = Modifier.size(36.dp)
				) {
					Icon(
						imageVector = Icons.Rounded.Remove,
						contentDescription = "Decrease",
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}

				BasicTextField(
					value = if (value == 0L) "" else value.toString(),
					onValueChange = {newValue -> newValue.toLongOrNull()?.let { onValueChange(it)} ?: onValueChange(0);  },
					modifier = Modifier.width(45.dp),
					textStyle = MaterialTheme.typography.titleMedium.copy(
						textAlign = TextAlign.Center,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						fontWeight = FontWeight.Bold
					),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true
				)

				IconButton(
					onClick = {
						if (value + 1 <= 700) {
							onValueChange(value + 1)
						} else {
							view.performHapticFeedback(HapticFeedbackConstants.REJECT)
						}
					},
					modifier = Modifier.size(36.dp)
				) {
					Icon(
						imageVector = Icons.Rounded.Add,
						contentDescription = "Increase",
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}
			}
		}

		if (previewText.isNotEmpty()) {
			// add a slight animation here
			Spacer(modifier = Modifier.padding(3.dp))
			Text(
				text = previewText,
				style = MaterialTheme.typography.labelSmall,
				color = if (doWeHaveError) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(start = 40.dp, top = 2.dp)
			)
		}
	}
}
