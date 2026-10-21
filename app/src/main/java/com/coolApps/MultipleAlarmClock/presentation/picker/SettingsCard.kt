package com.coolApps.MultipleAlarmClock.presentation.picker

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private val SegmentedTopShape = RoundedCornerShape(
	topStart = 24.dp,
	topEnd = 24.dp,
	bottomStart = 6.dp,
	bottomEnd = 6.dp
)

private val SegmentedMiddleShape = RoundedCornerShape(6.dp)

private val SegmentedBottomShape = RoundedCornerShape(
	topStart = 6.dp,
	topEnd = 6.dp,
	bottomStart = 24.dp,
	bottomEnd = 24.dp
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsCard(
		uiState: AlarmPickerUiState,
		selectedSoundName: String,
		messageValueChanged: (String) -> Unit,
		updateIsForceLoudVolume: (Boolean) -> Unit,
		updateFrequency: (Long) -> Unit,
		calenderButtonClicked: () -> Unit,
		selectSoundButtonClicked: () -> Unit,
		repeatDayToggled: (DayOfWeek) -> Unit,
		modifier: Modifier = Modifier,
		animateScroll: Boolean = true
) {
	Surface(
		shape = RoundedCornerShape(28.dp),
		color = colorScheme.surfaceContainerLow,
		modifier = modifier.fillMaxWidth()
	) {
		val density = LocalDensity.current
		val initialScroll = remember(animateScroll) {
			if (animateScroll) with(density) { 62.dp.toPx().toInt() } else 0
		}
		val scrollState = rememberScrollState(initial = initialScroll)
		val scrollSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()

		LaunchedEffect(animateScroll) {
			if (animateScroll && scrollState.value > 0) {
				scrollState.animateScrollTo(
					value = 0,
					animationSpec = scrollSpec
				)
			}
		}

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.imePadding()
				.verticalScroll(scrollState)
				.padding(4.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Surface(
				shape = SegmentedTopShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				RepeatDaysRow(
					selectedDays =
						uiState.alarmData.repeatDays?.toSet()
							?: emptySet(),
					onDayToggled = repeatDayToggled
				)
			}

			Surface(
				shape = SegmentedMiddleShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				FrequencyRow(
					icon = Icons.Rounded.Timer,
					title = stringResource(
						R.string.alarm_picker_repeat_every
					),
					value = uiState.alarmData.frequencyInMin,
					onValueChange = { newValue ->
						if (newValue in 0..<720) {
							updateFrequency(newValue)
						}
					},
					uiState = uiState
				)
			}

			Surface(
				shape = SegmentedMiddleShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				SettingRow(
					icon = Icons.Rounded.CalendarMonth,
					title = stringResource(
						R.string.alarm_picker_date
					),
					value = SimpleDateFormat(
						"EEE, MMM d, yyyy",
						LocalLocale.current.platformLocale
					).format(uiState.alarmData.startTime),
					onClick = calenderButtonClicked
				)
			}

			Surface(
				shape = SegmentedMiddleShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				SettingRow(
					icon = Icons.Rounded.Notifications,
					title = stringResource(
						R.string.alarm_picker_sound
					),
					value = selectedSoundName,
					onClick = selectSoundButtonClicked
				)
			}

			Surface(
				shape = SegmentedMiddleShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				MessageRow(
					icon = Icons.AutoMirrored.Rounded.Message,
					title = stringResource(
						R.string.alarm_picker_message
					),
					value = uiState.alarmData.message,
					onValueChange = messageValueChanged
				)
			}

			Surface(
				shape = SegmentedBottomShape,
				color = colorScheme.surfaceContainer,
				modifier = Modifier.fillMaxWidth()
			) {
				SwitchRow(
					icon = Icons.AutoMirrored.Rounded.VolumeUp,
					title = stringResource(
						R.string.alarm_picker_force_loud_volume
					),
					description = stringResource(
						R.string.alarm_picker_force_loud_volume_desc
					),
					checked = uiState.alarmData.isForceLoudVolume,
					onCheckedChange = { isChecked ->
						updateIsForceLoudVolume(isChecked)
					}
				)
			}
		}
	}
}

@Composable
private fun ExpressiveSettingIconBadge(
	icon: ImageVector,
	modifier: Modifier = Modifier
) {
	Surface(
		shape = RoundedCornerShape(12.dp),
		color = colorScheme.secondaryContainer.copy(alpha = 0.65f),
		contentColor = colorScheme.onSecondaryContainer,
		modifier = modifier.size(36.dp)
	) {
		Box(contentAlignment = Alignment.Center) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = colorScheme.onSecondaryContainer,
				modifier = Modifier.size(20.dp)
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwitchRow(
	icon: ImageVector,
	title: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	description: String? = null
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.toggleable(
				value = checked,
				role = Role.Switch,
				onValueChange = onCheckedChange
			)
			.padding(horizontal = 16.dp, vertical = 14.dp)
			.animateContentSize(
				animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
			),
		verticalAlignment = Alignment.CenterVertically
	) {
		ExpressiveSettingIconBadge(icon = icon)
		Spacer(modifier = Modifier.width(14.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				color = colorScheme.onSurface,
				style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
			)
			if (description != null) {
				Text(
					text = description,
					color = colorScheme.onSurfaceVariant,
					style = typography.bodySmall,
					modifier = Modifier.padding(top = 1.dp)
				)
			}
		}
		Switch(
			checked = checked,
			onCheckedChange = null // Handled by Row toggleable
		)
	}
}



@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageRow(
	icon: ImageVector,
	title: String,
	value: String,
	onValueChange: (String) -> Unit
) {
	val colorScheme = colorScheme
	val typography = typography
	val coroutineScope = rememberCoroutineScope()
	val focusManager = LocalFocusManager.current

	var isEditing by rememberSaveable { mutableStateOf(false) }
	// Track focus state to avoid immediate collapse during composition
	var isFocused by remember { mutableStateOf(false) }
	val textFieldState = rememberTextFieldState(value)

	// Sync value from parent (e.g. from AlarmPickerUiState)
	LaunchedEffect(value) {
		if (value != textFieldState.text.toString()) {
			textFieldState.setTextAndPlaceCursorAtEnd(value)
		}
	}

	// Sync state back to parent
	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text.toString() }
			.collectLatest { onValueChange(it) }
	}

	val focusRequester = remember { FocusRequester() }
	val bringIntoViewRequester = remember { BringIntoViewRequester() }

	val titleStyle = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
	val messageStyle = typography.bodyMedium

	if (isEditing) {
		BackHandler {
			isEditing = false
			focusManager.clearFocus()
		}
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(
				enabled = !isEditing,
				onClick = { isEditing = true }
			)
			.padding(horizontal = 16.dp, vertical = 14.dp)
			.animateContentSize(
				animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
			),
		verticalAlignment = Alignment.CenterVertically
	) {
		ExpressiveSettingIconBadge(icon = icon)

		Spacer(modifier = Modifier.width(14.dp))

		FlowRow(
			modifier = Modifier
				.weight(1f)
				.bringIntoViewRequester(bringIntoViewRequester),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = title,
				color = colorScheme.onSurface,
				style = titleStyle,
				modifier = Modifier.padding(top = 1.dp),
				maxLines = 1,
				softWrap = false
			)
			Spacer(modifier = Modifier.width(12.dp))

			if (isEditing) {
				BasicTextField(
					state = textFieldState,
					textStyle = messageStyle.copy(
						color = colorScheme.onSurface.copy(alpha = 0.85f),
						textAlign = TextAlign.Start
					),
					cursorBrush = SolidColor(colorScheme.secondary),
					keyboardOptions = KeyboardOptions(
						imeAction = ImeAction.Done,
						keyboardType = KeyboardType.Text
					),
					onKeyboardAction = {
						isEditing = false
						focusManager.clearFocus()
					},
					lineLimits = TextFieldLineLimits.MultiLine(1, 3),
					modifier = Modifier
						.widthIn(min = 60.dp)
						.focusRequester(focusRequester)
						.onFocusChanged { focusState ->
							val wasFocused = isFocused
							isFocused = focusState.isFocused
							if (isFocused) {
								coroutineScope.launch {
									bringIntoViewRequester.bringIntoView()
								}
							} else if (wasFocused) {
								// Only collapse if we just lost focus
								isEditing = false
							}
						}
				)
			} else {
				Text(
					text = value.ifBlank {
						stringResource(R.string.alarm_picker_message_placeholder)
					},
					color = if (value.isBlank()) colorScheme.onSurfaceVariant else colorScheme.onSurface.copy(alpha = 0.85f),
					style = messageStyle,
					modifier = Modifier.padding(top = 1.dp),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
	}

	LaunchedEffect(isEditing) {
		if (isEditing) {
			withFrameNanos { }
			focusRequester.requestFocus()
			bringIntoViewRequester.bringIntoView()
		}
	}
}


@Composable
fun RepeatDaysRow(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
	val bounceSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

	val cornerRadius by animateDpAsState(
		targetValue = if (isSelected) 12.dp else 20.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "day_corner_radius"
	)

	val containerColor by animateColorAsState(
		targetValue = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerHighest,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "day_container_color"
	)
	val contentColor by animateColorAsState(
		targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
		label = "day_content_color"
	)

	Surface(
		onClick = {
			view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
			coroutineScope.launch {
				scale.snapTo(1f)
				scale.animateTo(
					targetValue = 1.18f,
					animationSpec = bounceSpec
				)
				scale.animateTo(
					targetValue = 1f,
					animationSpec = bounceSpec
				)
			}
			onToggle(day)
		},
		shape = RoundedCornerShape(cornerRadius),
		color = if (isSelected) containerColor else containerColor.copy(alpha = 0.75f),
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
				style = typography.labelLarge.copy(
					fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
				),
				color = contentColor
			)
		}
	}
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
	Row(
		modifier =
			Modifier.fillMaxWidth()
				.clickable(onClick = onClick)
				.padding(horizontal = 16.dp, vertical = 14.dp)
				.animateContentSize(
					animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
				),
		verticalAlignment = Alignment.CenterVertically
	) {
		ExpressiveSettingIconBadge(icon = icon)
		Spacer(modifier = Modifier.width(14.dp))
		Text(
			text = title,
			color = colorScheme.onSurface,
			style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
		)
		Spacer(modifier = Modifier.weight(1f))
		Surface(
			shape = RoundedCornerShape(12.dp),
			color = colorScheme.surfaceContainerHighest
		) {
			Text(
				text = value,
				color = colorScheme.onSurfaceVariant,
				style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
				modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
			)
		}
	}
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FrequencyRow(
			icon: ImageVector,
			title: String,
			value: Long,
			onValueChange: (Long) -> Unit,
			uiState: AlarmPickerUiState,
		) {
		val doWeHaveFrequencyError = uiState.validationResult is AlarmDataValidationResult.Frequency

		val view = LocalView.current
		var textValue by remember(value) { mutableStateOf(if (value == 0L) "" else value.toString()) }
		val spatialIntSizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
		val spatialIntOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
		val effectsFloatSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

		Column(
			modifier =
				Modifier.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp)
					.animateContentSize(animationSpec = spatialIntSizeSpec)
		) {
			Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				ExpressiveSettingIconBadge(icon = icon)
				Spacer(modifier = Modifier.width(14.dp))
				Text(
					text = title,
					color = colorScheme.onSurface,
					style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
					modifier = Modifier.weight(1f)
				)
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier =
						Modifier.background(
							color =
								if (doWeHaveFrequencyError) colorScheme.errorContainer
								else colorScheme.secondaryContainer,
							shape = RoundedCornerShape(16.dp)
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
							tint = colorScheme.onSecondaryContainer
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

					val currentNum = textValue.toLongOrNull() ?: 0L
					var previousNum by remember { mutableLongStateOf(currentNum) }
					val isIncreasing = currentNum >= previousNum
					LaunchedEffect(currentNum) {
						previousNum = currentNum
					}

					Box(modifier = Modifier.width(58.dp), contentAlignment = Alignment.Center) {
						Row(
							modifier = Modifier.animateContentSize(
								animationSpec = spatialIntSizeSpec
							),
							horizontalArrangement = Arrangement.Center,
							verticalAlignment = Alignment.CenterVertically
						) {
							textValue.forEachIndexed { index, char ->
								val digitWeight = textValue.length - index
								key(digitWeight) {
									AnimatedContent(
										targetState = char,
										transitionSpec = {
											val slideIn = slideInVertically(
												animationSpec = spatialIntOffsetSpec,
												initialOffsetY = { if (isIncreasing) (it * 0.75).toInt() else -(it * 0.75).toInt() }
											)
											val slideOut = slideOutVertically(
												animationSpec = spatialIntOffsetSpec,
												targetOffsetY = { if (isIncreasing) -(it * 0.75).toInt() else (it * 0.75).toInt() }
											)
											(slideIn + fadeIn(
												animationSpec = effectsFloatSpec
											)).togetherWith(
												slideOut + fadeOut(
													animationSpec = effectsFloatSpec
												)
											).using(SizeTransform(clip = false))
										},
										label = "char_anim_$digitWeight"
									) { targetChar ->
										Text(
											text = targetChar.toString(),
											style = typography.titleMedium.copy(
												textAlign = TextAlign.Center,
												color = colorScheme.onSecondaryContainer,
												fontWeight = FontWeight.ExtraBold
											)
										)
									}
								}
							}
							if (textValue.isNotEmpty()) {
								Text(
									text = " min",
									style = typography.titleMedium.copy(
										textAlign = TextAlign.Center,
										color = colorScheme.onSecondaryContainer,
										fontWeight = FontWeight.ExtraBold
									)
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
							modifier = Modifier.fillMaxWidth(),
							textStyle = typography.titleMedium.copy(
								textAlign = TextAlign.Center,
								color = Color.Transparent,
								fontWeight = FontWeight.ExtraBold
							),
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
							singleLine = true,
							cursorBrush = SolidColor(colorScheme.onSecondaryContainer)
						)
					}

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
							tint = colorScheme.onSecondaryContainer
						)
					}
				}
			}
		}
}