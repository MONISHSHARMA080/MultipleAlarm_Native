package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPickerScreen(
	alarmSetGoBack: () -> Unit,
	onNavigateToSoundList: () -> Unit,
	viewModel: AlarmPickerViewModel
) {
	val uiState by viewModel.uiState.collectAsState()
	val selectedSound by viewModel.selectedAlarmSound.collectAsState()

	val startTime = uiState.alarmObject.startTime.time
	val endTime = uiState.alarmObject.endTime.time

	val timeStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
	val amPmStyle = MaterialTheme.typography.labelLarge
	val currentError by remember(uiState) { mutableStateOf(  uiState.validationResult as? ValidationResult.Failure) }

	Scaffold { screenPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
				.padding(screenPadding)
				.padding(horizontal = 10.dp), // Expressive margin
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Spacer(modifier = Modifier.weight(0.6f))
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center
			) {
				Row(
					verticalAlignment = Alignment.Bottom,
					horizontalArrangement = Arrangement.spacedBy(4.dp)
				) {
					Text(
						text = SimpleDateFormat("h:mm", LocalLocale.current.platformLocale).format(startTime),
						style = timeStyle,
						color = MaterialTheme.colorScheme.onBackground,
						maxLines = 1,
						softWrap = false,
						modifier = Modifier.alignByBaseline()
					)
					Text(
						text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(startTime),
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
					tint = MaterialTheme.colorScheme.primary, // Primary color for emphasis
					modifier = Modifier.padding(horizontal = 24.dp).size(32.dp)
				)


				Row(
					verticalAlignment = Alignment.Bottom,
					horizontalArrangement = Arrangement.spacedBy(4.dp)
				) {
					Text(
						text = SimpleDateFormat("h:mm", LocalLocale.current.platformLocale).format(endTime),
						style = timeStyle,
						color = MaterialTheme.colorScheme.onBackground,
						maxLines = 1,
						softWrap = false,
						modifier = Modifier.alignByBaseline()
					)
					Text(
						text = SimpleDateFormat("a", LocalLocale.current.platformLocale).format(endTime),
						style = amPmStyle,
						color = MaterialTheme.colorScheme.onBackground,
						maxLines = 1,
						softWrap = false,
						modifier = Modifier.alignByBaseline()
					)
				}
			}

			Spacer(modifier = Modifier.weight(0.6f))

			DateList(
				{}, startTime.time,
				weGood = currentError?.field != AlarmErrorField.DATE,
				allowSelectingPastDate = false,
			)

			Spacer(modifier = Modifier.weight(0.4f))

			// 5. Settings Card (Name & Sound)
			Surface(
				shape = RoundedCornerShape(28.dp), // More expressive corner radius
				color = MaterialTheme.colorScheme.surfaceContainer, // Updated M3 token
				modifier = Modifier.fillMaxWidth()
			) {
				Column {

					FrequencyRow(
						icon = Icons.Rounded.Timer,
						title = "Repeat every",
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
						title = "Sound",
						value = selectedSound?.title ?: "Random",
						onClick = onNavigateToSoundList
					)

					HorizontalDivider(
						modifier = Modifier.padding(horizontal = 16.dp),
						color = MaterialTheme.colorScheme.outlineVariant,
					)

					MessageRow(
						icon = Icons.AutoMirrored.Rounded.Message,
						title = "Alarm message",
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
						style = MaterialTheme.typography.bodyLarge ,
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
						style = MaterialTheme.typography.bodyLarge ,
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
			minLines = 3, // Support 2-3 lines comfortably
			maxLines = 5,
			shape = RoundedCornerShape(18.dp),
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

			// Expressive Frequency Picker
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.background(
						color = if (doWeHaveError) colorScheme.errorContainer else colorScheme.primaryContainer ,
						shape = RoundedCornerShape(12.dp)
					)
					.padding(4.dp)
			) {
				IconButton(
					onClick = { if(value - 1 > 0 )onValueChange(value -1) },
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
					onClick = { if(value + 1 <= 700 )onValueChange(value + 1) },
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
