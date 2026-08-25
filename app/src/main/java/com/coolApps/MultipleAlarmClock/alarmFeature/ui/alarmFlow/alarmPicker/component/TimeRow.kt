package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.AlarmErrorField
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerUiState
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRow(
	uiState: AlarmPickerUiState,
	onStartTimeChange: (Calendar) -> Unit,
	onEndTimeChange: (Calendar) -> Unit
) {
	val startTime = uiState.alarmObject.startTime
	val endTime = uiState.alarmObject.endTime

	val density = LocalDensity.current
	val containerSize = LocalWindowInfo.current.containerSize
	val screenWidthDp = with(density) { containerSize.width.toDp() }
	val screenHeightDp = with(density) { containerSize.height.toDp() }
	val timeStyle = typography.displayMedium.copy(fontWeight = FontWeight.Bold)
	val amPmStyle = typography.bodyMedium


	val titleSpacing = (screenHeightDp * 0.04f).coerceIn(12.dp, 36.dp)

	val endTimePickerState = rememberTimePickerState(
		initialHour = endTime.get(Calendar.HOUR_OF_DAY),
		initialMinute = endTime.get(Calendar.MINUTE),
		is24Hour = false
	)

	val timePickerState =
		rememberTimePickerState(
			initialHour = startTime.get(Calendar.HOUR_OF_DAY),
			initialMinute = startTime.get(Calendar.MINUTE),
			is24Hour = false
		)

	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker by remember { mutableStateOf(false) }

	val doWeHaveError = uiState.validationResult != ValidationResult.Success && (uiState.validationResult as? ValidationResult.Failure)?.field == AlarmErrorField.Time
	val errorMessage = (uiState.validationResult as? ValidationResult.Failure)?.message
	val timeColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground
	val amPmColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground

	if (showStartTimePicker) {
		TimePickerDialog(
			onDismissRequest = { showStartTimePicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						val newTime =
							(startTime.clone() as Calendar).apply {
								set(Calendar.HOUR_OF_DAY, timePickerState.hour)
								set(Calendar.MINUTE, timePickerState.minute)
							}
						onStartTimeChange(newTime)
						showStartTimePicker = false
					}
				) { Text(stringResource(R.string.alarm_picker_ok)) }
			},
			dismissButton = {
				TextButton(onClick = { showStartTimePicker = false }) { Text(stringResource(R.string.alarm_picker_cancel)) }
			},
			title = {
				Column {
					Text(
						text = stringResource(R.string.alarm_picker_select_start_time),
						style = typography.titleMedium,
						color = colorScheme.onSurfaceVariant,
						maxLines = 1,
						softWrap = false,
					)
					Spacer(modifier = Modifier.height(titleSpacing))
				}
			}
		) { TimePicker(state = timePickerState) }
	}

	if (showEndTimePicker) {

		// live-compute validity as the user spins the dial
		val candidateEnd = (endTime.clone() as Calendar).apply {
			set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
			set(Calendar.MINUTE, endTimePickerState.minute)
		}
		val isCandidateInvalid = candidateEnd.timeInMillis <= startTime.timeInMillis

		TimePickerDialog(
			onDismissRequest = { showEndTimePicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						onEndTimeChange(candidateEnd)
						showEndTimePicker = false
					},
					enabled = !isCandidateInvalid
				) { Text(stringResource(R.string.alarm_picker_ok)) }
			},
			dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text(stringResource(R.string.alarm_picker_cancel)) } },
			title = {
				Column {
					Text(
						text = stringResource(R.string.alarm_picker_select_end_time),
						style = typography.titleMedium,
						color = colorScheme.onSurfaceVariant,
					)
					AnimatedVisibility(visible = isCandidateInvalid) {
						val locale = LocalLocale.current.platformLocale
						val startTimeString = remember(startTime, locale) {
							SimpleDateFormat("h:mm a", locale).format(startTime.time)
						}
						Text(
							text = stringResource(R.string.alarm_error_fix_alarm_time, startTimeString),
							style = typography.labelMedium,
							color = colorScheme.error,
							modifier = Modifier.padding(top = 4.dp)
						)
					}
					Spacer(modifier = Modifier.height(titleSpacing))
				}
			}
		) { TimePicker(state = endTimePickerState) }
	}

	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier.weight(1f).clickable { showStartTimePicker = !showStartTimePicker },
			horizontalArrangement = Arrangement.Start
		) {
			Text(
				text =
					SimpleDateFormat("h:mm ", LocalLocale.current.platformLocale)
						.format(startTime.time),
				style = timeStyle,
				color = timeColor,
				maxLines = 1,
				softWrap = false,
				modifier = Modifier.alignByBaseline()
			)
			Text(
				text =
					SimpleDateFormat("a", LocalLocale.current.platformLocale)
						.format(startTime.time),
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
			modifier = Modifier.weight(1f).clickable { showEndTimePicker = !showEndTimePicker },
			horizontalArrangement = Arrangement.End
		) {
			Text(
				text =
					SimpleDateFormat("h:mm ", LocalLocale.current.platformLocale).format(endTime.time),
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
				modifier = Modifier.padding(top = 5.dp),
				color = colorScheme.onErrorContainer
			)
		}
	}
}
