package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventRepeat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.time.Duration.Companion.seconds

@Composable fun SettingsCard(
	uiState: AlarmPickerUiState,
	selectedSoundName:String,
	messageValueChanged: (String) -> Unit,
	updateFrequency: (Long) -> Unit,
	calenderButtonClicked: () -> Unit,
	selectSoundButtonClicked: () -> Unit,
	repeatDayToggled: (DayOfWeek) -> Unit
){
	// 5. Settings Card (Name & Sound)
	Surface(
		shape = RoundedCornerShape(29.dp),
		color = colorScheme.surfaceContainer,
		modifier = Modifier.fillMaxWidth()
	) {
		Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

			RepeatDaysRow(
				icon = Icons.Rounded.EventRepeat,
				title = stringResource(R.string.alarm_picker_repeat_days),
				selectedDays = uiState.alarmData.repeatDays?.toSet() ?: emptySet(),
				onDayToggled = repeatDayToggled
			)
			HorizontalDivider(
				modifier = Modifier.padding(horizontal = 16.dp),
				color = colorScheme.outlineVariant,
			)

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


@Composable
fun RepeatDaysRow(
		icon: ImageVector,
		title: String,
		selectedDays: Set<DayOfWeek>,
		onDayToggled: (DayOfWeek) -> Unit,
) {
	val locale = LocalLocale.current.platformLocale
	val orderedDays = remember(locale) {
		val first = WeekFields.of(locale).firstDayOfWeek
		(0..6).map { first.plus(it.toLong()) }
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp)
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(imageVector = icon, contentDescription = null, tint = colorScheme.onSurfaceVariant)
			Spacer(modifier = Modifier.width(16.dp))
			Text(text = title, color = colorScheme.onBackground, style = typography.titleSmall)
		}
		Spacer(modifier = Modifier.height(12.dp))
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			orderedDays.forEach { day ->
				RepeatDayButton(
					day = day,
					isSelected = day in selectedDays,
					onToggle = onDayToggled
				)
			}
		}
	}
}

@Composable
private fun RepeatDayButton(
	day: DayOfWeek,
	isSelected: Boolean,
	onToggle: (DayOfWeek) -> Unit,
	modifier: Modifier = Modifier
) {
	val locale = LocalLocale.current.platformLocale
	val narrowLabel = remember(day, locale) { day.getDisplayName(TextStyle.NARROW, locale) }
	val fullLabel = remember(day, locale) { day.getDisplayName(TextStyle.FULL, locale) }
	val view = LocalView.current
	val coroutineScope = rememberCoroutineScope()
	val scale = remember { Animatable(1f) }
	val tweenDuration = 10


	val containerColor by animateColorAsState(
		targetValue = if (isSelected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHighest,
		animationSpec = tween(durationMillis = tweenDuration, easing = FastOutLinearInEasing),
		label = "day_container_color"
	)
	val contentColor by animateColorAsState(
		targetValue = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
		animationSpec = tween(durationMillis = tweenDuration, easing = FastOutSlowInEasing),
		label = "day_content_color"
	)

	Surface(
		onClick = {
			view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
			coroutineScope.launch {
				scale.snapTo(1f)
				scale.animateTo(
					targetValue = 1.18f,
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioMediumBouncy,
						stiffness = Spring.StiffnessMedium
					)
				)
				scale.animateTo(
					targetValue = 1f,
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioMediumBouncy,
						stiffness = Spring.StiffnessMediumLow
					)
				)
			}
			onToggle(day)
		},
		shape = CircleShape,
		color = containerColor.copy(alpha = 0.7f),
		modifier = modifier
			.size(40.dp)
			.graphicsLayer {
				scaleX = scale.value
				scaleY = scale.value
			}
			.semantics { contentDescription = fullLabel }
	) {
		Box(contentAlignment = Alignment.Center) {
			Text(
				text = narrowLabel,
				style = typography.labelLarge,
				color = contentColor
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
	var isTyping by remember { mutableStateOf(false) }
	LaunchedEffect(value) {
		if (value.isEmpty()) {
			isTyping = false
			return@LaunchedEffect
		}
		isTyping = true
		delay(3.seconds)
		isTyping = false
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.imePadding()
			.padding(horizontal = 16.dp, vertical = 20.dp)
			.animateContentSize(
				animationSpec = spring(
					dampingRatio = Spring.DampingRatioNoBouncy,
					stiffness = Spring.StiffnessMediumLow
				)
			),
		verticalAlignment = if (isTyping) {
			Alignment.Top
		} else {
			Alignment.CenterVertically
		}
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

		Spacer(modifier = Modifier.width(16.dp))

		BasicTextField(
			value = value,
			onValueChange = onValueChange,

			// Collapsed when idle, expandable while actively typing.
			minLines = 1,
			maxLines = if (isTyping) 4 else 1,

			singleLine = !isTyping,

			modifier = Modifier
				.weight(1f)
				.padding(start = 8.dp),

			textStyle = typography.bodyMedium.copy(
				color = colorScheme.onSurface,
				textAlign = TextAlign.End
			),

			cursorBrush = SolidColor(colorScheme.secondary),

			decorationBox = { innerTextField ->
				Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = if (isTyping) {
						Alignment.TopEnd
					} else {
						Alignment.CenterEnd
					}
				) {
					if (value.isEmpty()) {
						Text(
							text = stringResource(
								R.string.alarm_picker_message_placeholder
							),
							style = typography.bodyMedium,
							color = colorScheme.onSurfaceVariant,
							textAlign = TextAlign.End,
							modifier = Modifier.fillMaxWidth()
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
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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