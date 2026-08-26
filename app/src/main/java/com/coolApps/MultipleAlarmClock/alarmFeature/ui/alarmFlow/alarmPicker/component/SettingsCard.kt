package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDataValidationResult
//import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.AlarmErrorField
//import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerUiState
import java.text.SimpleDateFormat


@Composable fun SettingsCard(
	uiState: AlarmPickerUiState,
	selectedSoundName:String,
	messageValueChanged: (String) -> Unit,
	updateFrequency: (Long) -> Unit,
	calenderButtonClicked: () -> Unit,
	selectSoundButtonClicked: () -> Unit,

){
	// 5. Settings Card (Name & Sound)
	Surface(
		shape = RoundedCornerShape(29.dp),
		color = colorScheme.surfaceContainer,
		modifier = Modifier.fillMaxWidth()
	) {
		Column {
			FrequencyRow(
				icon = Icons.Rounded.Timer,
				title = stringResource(R.string.alarm_picker_repeat_every),
				value = uiState.alarmData.frequencyInMin,
				onValueChange = { newValue ->
					newValue.let {
						if (it in 0..<720) {
							updateFrequency(it)
						}
					}
				},
//				previewText = frequencyText,
				uiState,
			)
			HorizontalDivider(
				modifier = Modifier.padding(horizontal = 16.dp),
				color = colorScheme.outlineVariant,
			)

			SettingRow(
				icon = Icons.Rounded.CalendarMonth,
				title = stringResource(R.string.alarm_picker_date),
				value = SimpleDateFormat(
					"EEE, MMM d, yyyy",
					LocalLocale.current.platformLocale
				).format(uiState.alarmData.startTime),
				onClick = calenderButtonClicked
			)

			HorizontalDivider(
				modifier = Modifier.padding(horizontal = 16.dp),
				color = colorScheme.outlineVariant,
			)

			SettingRow(
				icon = Icons.Rounded.Notifications,
				title = stringResource(R.string.alarm_picker_sound),
				value = selectedSoundName,
				onClick = selectSoundButtonClicked
			)

			HorizontalDivider(
				modifier = Modifier.padding(horizontal = 16.dp),
				color = colorScheme.outlineVariant,
			)

			MessageRow(
				icon = Icons.AutoMirrored.Rounded.Message,
				title = stringResource(R.string.alarm_picker_message),
				value = uiState.alarmData.message,
				onValueChange = messageValueChanged,
			)

		}
	}
}

@Composable fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
	Row(
		modifier =
			Modifier.fillMaxWidth()
				.clickable(onClick = onClick)
				.padding(horizontal = 16.dp, vertical = 20.dp)
				.animateContentSize(),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(imageVector = icon, contentDescription = null, tint = colorScheme.onSurfaceVariant)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = title,
			color = colorScheme.onBackground,
			style = typography.titleSmall,
		)
		Spacer(modifier = Modifier.weight(1f))
		Text(
			text = value,
			color = colorScheme.onSurfaceVariant,
			style = typography.labelLarge,
		)
	}
}

@Composable fun MessageRow(
			icon: ImageVector,
			title: String,
			value: String,
			onValueChange: (String) -> Unit
		) {

			Column(
				modifier =
					Modifier.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 12.dp)
						.imePadding()
						.animateContentSize()
			) {
				Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
					Icon(
						imageVector = icon,
						contentDescription = null,
						tint = colorScheme.onSurfaceVariant,
						modifier = Modifier.size(20.dp)
					)
					Spacer(modifier = Modifier.width(12.dp))
					Text(
						text = title,
						color = colorScheme.onBackground,
						style = typography.titleSmall,
						modifier = Modifier.weight(1f)
					)
				}
				Spacer(modifier = Modifier.height(8.dp))

				TextField(
					value = value,
					onValueChange = onValueChange,
					modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
					textStyle = typography.bodyMedium,
					placeholder = {
						Text(
							text = stringResource(R.string.alarm_picker_message_placeholder),
							style = typography.bodyMedium,
							color = colorScheme.onSurfaceVariant
						)
					},
					minLines = 1,
					maxLines = 3,
					shape = RoundedCornerShape(20.dp),
					colors =
						TextFieldDefaults.colors(
							focusedIndicatorColor = Color.Transparent,
							unfocusedIndicatorColor = Color.Transparent,
							disabledIndicatorColor = Color.Transparent,
							errorIndicatorColor = Color.Transparent
						)
				)
			}
		}

@Composable fun FrequencyRow(
			icon: ImageVector,
			title: String,
			value: Long,
			onValueChange: (Long) -> Unit,
			uiState: AlarmPickerUiState,
		) {
		val doWeHaveFrequencyError =  uiState.validationResult is AlarmDataValidationResult.Frequency

		val view = LocalView.current
		var textValue by remember(value) { mutableStateOf(if (value == 0L) "" else value.toString()) }

		Column(
			modifier =
				Modifier.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp)
					.animateContentSize()
		) {
			Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					text = title,
					color = colorScheme.onBackground,
					style = typography.titleSmall,
					modifier = Modifier.weight(1f)
				)
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier =
						Modifier.background(
							color =
								if (doWeHaveFrequencyError) colorScheme.errorContainer
								else colorScheme.secondaryContainer,
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
							contentDescription = stringResource(R.string.alarm_picker_decrease_desc),
							tint = colorScheme.onPrimaryContainer
						)
					}

					val minSuffixTransformation = remember {
						VisualTransformation { text ->
							val display = if (text.isEmpty()) text else AnnotatedString(text.text + " min")
							TransformedText(
								display,
								object : OffsetMapping {
									override fun originalToTransformed(offset: Int) = offset
									override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(text.text.length)
								}
							)
						}
					}

					BasicTextField(
						value = textValue,
						onValueChange = { newValue ->
							val digitsOnly = newValue.filter { it.isDigit() }
//							digitsOnly.toLongOrNull()?.let { onValueChange(it) } ?: onValueChange(0)
							// Only propagate when we actually have a parseable value.
							// Empty (e.g. user backspaced everything) stays local-only.
							textValue = digitsOnly
							digitsOnly.toLongOrNull()?.let { onValueChange(it) }
						},
						visualTransformation = minSuffixTransformation,
						modifier = Modifier.width(55.dp),
						textStyle =
							typography.titleMedium.copy(
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
							contentDescription = stringResource(R.string.alarm_picker_increase_desc),
							tint = colorScheme.onPrimaryContainer
						)
					}
				}
			}

			// check for failure as if we have a error  in time then we won't be able to produce correct
			// sequence of preview text, so don't display
//			AnimatedVisibility(
//				visible =
//					!doWeHaveErrorOtherThanFrequency &&
//							(previewText.isNotEmpty() || doWeHaveFrequencyError),
//				enter = expandVertically() + fadeIn(),
//				exit = shrinkVertically() + fadeOut()
//			) {
//				Spacer(modifier = Modifier.padding(3.dp))
//				Text(
//					text = previewText,
//					style = typography.labelMedium,
//					textAlign = TextAlign.Start,
//					modifier = Modifier.padding(top = 5.dp, start = 2.dp).animateContentSize(),
//					color =
//						if (doWeHaveFrequencyError) colorScheme.onErrorContainer
//						else colorScheme.onSurfaceVariant
//				)
//			}
		}
}