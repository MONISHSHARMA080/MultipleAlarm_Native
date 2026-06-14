package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
	val timeStyle = typography.headlineSmall

	val horizontalPadding = rememberAdaptiveHorizontalPadding()
	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(if (uiState.initialAlarm == null) "Set alarm" else "Edit alarm" ,
						style = timeStyle,
						color = colorScheme.onBackground,
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
		},
			bottomBar = {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(colorScheme.background)
						.padding(16.dp).padding(bottom = 20.dp)
						.animateContentSize()
					,
					contentAlignment = Alignment.Center
				) {
					Row(
						modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
						horizontalArrangement = Arrangement.End
					) {
						Button(
							onClick = {
								if (uiState.validationResult == ValidationResult.Success){
									viewModel.onSetAlarmClicked(uiState.initialAlarm, uiState.alarmObject)
									view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
									alarmSetGoBack()
								}
							},
							colors = when{
								uiState.validationResult == ValidationResult.Success ->{
									ButtonDefaults.buttonColors(
										containerColor = colorScheme.primaryContainer,
										contentColor = colorScheme.onPrimaryContainer
									)
								}else ->{
									ButtonDefaults.buttonColors(
										containerColor = colorScheme.errorContainer,
										contentColor = colorScheme.onErrorContainer
									)
								}
							} ,
							modifier = Modifier
								.height(56.dp)
								.animateContentSize(
									animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
								),
							contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
							shape = RoundedCornerShape(28.dp)
						) {
							AnimatedContent(
								targetState = uiState.validationResult == ValidationResult.Success,
								transitionSpec = {
									fadeIn() togetherWith fadeOut() using SizeTransform()
								},
								label = "button_text"
							) { isValid ->
								Text(
									when{
										isValid ->"Set alarm"
										uiState.validationResult is ValidationResult.Failure && (uiState.validationResult as ValidationResult.Failure).field == AlarmErrorField.AlarmIsNotDiff ->"Change something"
										else -> "Fix the error"
									},
									style = typography.bodyLarge ,
								)
							}
						}
					}
				}
			}
	) { screenPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(colorScheme.background)
				.padding(screenPadding)
				.padding(horizontal = horizontalPadding)
				.animateContentSize()
			,
			horizontalAlignment = Alignment.CenterHorizontally
		) {

			Spacer(modifier = Modifier.weight(0.45f))
			TimeRow(uiState, {viewModel.updateStartTime(it)}, {viewModel.updateEndTime(it)})
			Spacer(modifier = Modifier.weight(0.45f))
			DateList(
				{ viewModel.updateDate(it)}, uiState.alarmObject.startTime.time.time,
				weGood = currentError?.field != AlarmErrorField.DATE,
				allowSelectingPastDate = false,
			)

			Spacer(modifier = Modifier.weight(0.178f))

			// 5. Settings Card (Name & Sound)
			Surface(
				shape = RoundedCornerShape(28.dp),
				color = colorScheme.surfaceContainer,
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
						uiState,
					)
					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = colorScheme.outlineVariant,
					)

					SettingRow(
						icon = Icons.Rounded.Notifications,
						title = "sound",
						value = selectedSound?.title ?: "Random",
						onClick = onNavigateToSoundList
					)

					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = colorScheme.outlineVariant,
					)

					MessageRow(
						icon = Icons.AutoMirrored.Rounded.Message,
						title = "message",
						value = uiState.alarmObject.message,
						onValueChange = { viewModel.updateMessage(it) },
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

	val timeStyle = typography.displayLarge.copy(fontWeight = FontWeight.Bold)
	val amPmStyle = typography.labelLarge

	val configuration = LocalWindowInfo.current.containerSize
	// Calculate title spacing adaptively based on screen height
	val titleSpacing = (configuration.height.dp * 0.04f).coerceIn(12.dp, 36.dp)

	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker by remember { mutableStateOf(false) }

	val doWeHaveError = uiState.validationResult != ValidationResult.Success  && (uiState.validationResult as? ValidationResult.Failure)?.field == AlarmErrorField.Time
	val errorMessage = (uiState.validationResult as? ValidationResult.Failure)?.message
	val timeColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground
	val amPmColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground


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
						style = typography.titleMedium,
						color = colorScheme.onSurfaceVariant,
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
				color = timeColor,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
			Text(
				text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(startTime.time),
				style = amPmStyle,
				color = amPmColor,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
		}
		Icon(
			imageVector = Icons.AutoMirrored.Filled.ArrowForward,
			contentDescription = null,
			tint = colorScheme.onBackground,
			modifier = Modifier.size(32.dp)
		)
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier.weight(1f).clickable{showEndTimePicker = !showEndTimePicker},
			horizontalArrangement = Arrangement.End
		) {
			Text(
				text = SimpleDateFormat("h:mm ", LocalLocale.current.platformLocale).format(endTime.time),
				style = timeStyle,
				color = timeColor,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
			Text(
				text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(endTime.time),
				style = amPmStyle,
				color = amPmColor,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
		}
	}
		Column() {
			// text in center as I want to draw attention to it and left aligned one looked ugly
			Spacer(modifier = Modifier.padding(3.dp))
			AnimatedVisibility(
				visible = doWeHaveError,
				enter = expandVertically() + fadeIn(),
				exit = shrinkVertically() + fadeOut()
			) {
				Text(
					text = errorMessage.orEmpty(),
					style = typography.labelMedium,
					textAlign = TextAlign.Start,
					modifier = Modifier.padding( top = 5.dp),
					color = colorScheme.onErrorContainer
				)
			}
		}
}

@Composable fun rememberAdaptiveHorizontalPadding(percent: Float = 0.0062f, min: Dp = 14.dp, max: Dp = 30.dp): Dp {
	val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
	return (screenWidthDp * percent).coerceIn(min, max)
}


@Composable fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 20.dp)
			.animateContentSize()
		,
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = title,
			color = colorScheme.onBackground,
			style =typography.titleSmall,
		)
		Spacer(modifier = Modifier.weight(1f))
		Text(
			text = value,
			color = colorScheme.onSurfaceVariant,
			style = typography.labelLarge,
		)
	}
}

@Composable
private fun MessageRow(icon: ImageVector, title: String, value: String, onValueChange: (String) -> Unit) {

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp)
			.animateContentSize()
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint =  colorScheme.onSurfaceVariant,
				modifier = Modifier.size(20.dp)
			)
			Spacer(modifier = Modifier.width(12.dp))
			Text(
				text = title,
				color = colorScheme.onBackground,
				style =typography.titleSmall,
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
			textStyle = typography.bodyMedium,
			placeholder = {
				Text(
					text = "Add message...",
					style = typography.bodyMedium,
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
	}
}

@Composable
private fun FrequencyRow(
	icon: ImageVector,
	title: String,
	value: Long,
	onValueChange: (Long) -> Unit,
	previewText: String = "",
	uiState: AlarmPickerUiState,
) {
	val doWeHaveFrequencyError = (uiState.validationResult as? ValidationResult.Failure)?.field == AlarmErrorField.FREQUENCY
	val doWeHaveErrorOtherThanFrequency = (uiState.validationResult as? ValidationResult.Failure) != null &&  uiState.validationResult.field != AlarmErrorField.FREQUENCY


	logD("preview text is isNotEmpty:${previewText.isNotEmpty()}, and doWeHaveFrequencyError:$doWeHaveFrequencyError, validation result: ${uiState.validationResult}, doWeHaveErrorOtherThanFrequency: $doWeHaveErrorOtherThanFrequency ")
	val view = LocalView.current

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp)
			.animateContentSize()
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
				style =typography.titleSmall,
				modifier = Modifier.weight(1f)
			)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.background(
						color = if (doWeHaveFrequencyError) colorScheme.errorContainer else colorScheme.secondaryContainer ,
						shape = RoundedCornerShape(12.dp)
					)
					.padding(4.dp)
			) {
				IconButton(
					onClick = {
						if (value - 1 > 0) { onValueChange(value - 1) } else {
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
					textStyle = typography.titleMedium.copy(
						textAlign = TextAlign.Center,

						color = colorScheme.onPrimaryContainer,
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

		// check for failure as if we have a error  in time then we won't be able to produce correct sequence of preview text, so don't display
			AnimatedVisibility(
				visible = !doWeHaveErrorOtherThanFrequency && (previewText.isNotEmpty() || doWeHaveFrequencyError),
				enter = expandVertically() + fadeIn(),
				exit = shrinkVertically() + fadeOut()
			) {
				Spacer(modifier = Modifier.padding(3.dp))
				Text(
					text = previewText,
					style = typography.labelMedium,
					textAlign = TextAlign.Start,
					modifier = Modifier.padding( top = 5.dp, start = 2.dp).animateContentSize(),
					color = if (doWeHaveFrequencyError) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant
				)
			}
	}
}
