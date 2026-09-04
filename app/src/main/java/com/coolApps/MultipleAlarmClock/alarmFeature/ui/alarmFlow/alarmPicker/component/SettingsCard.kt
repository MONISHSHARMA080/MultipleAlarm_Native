package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

//import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.AlarmErrorField
//import com.coolApps.MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.imePadding()
			.padding(horizontal = 16.dp, vertical = 16.dp)
			.animateContentSize(),
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
			style = typography.titleSmall
		)
		Spacer(modifier = Modifier.weight(0.85f))
		BasicTextField(
			value = value,
			maxLines = 1,
			minLines = 1,
			onValueChange = onValueChange,
			modifier = Modifier.weight(1f)
//				.background(colorScheme.secondaryContainer.copy(alpha = 0.40f))
			,
			textStyle = typography.bodyMedium.copy(
				color = colorScheme.onSurface,
			),
			cursorBrush = SolidColor(colorScheme.secondary),

			singleLine = true,
			decorationBox = { innerTextField ->
				Box(contentAlignment = Alignment.CenterStart) {
					if (value.isEmpty()) {
						Text(
							text = stringResource(R.string.alarm_picker_message_placeholder),
							style = typography.bodyMedium,
							color = colorScheme.onSurfaceVariant
						)
					}
					innerTextField()
				}
			}
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
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								onValueChange(value - 1)
							} else {
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
		}
}