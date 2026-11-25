package com.coolApps.MultipleAlarmClock.presentation.picker

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.presentation.AlarmTimeBoundsTransform
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalMaterial3ExpressiveApi::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
fun TimeRow(
	uiState: AlarmPickerUiState,
	onStartTimeChange: (Calendar) -> Unit,
	onEndTimeChange: (Calendar) -> Unit,
	onDisabledTimeSelected: () -> Unit,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	alarmId: Int? = null,
	boundsTransform: BoundsTransform = AlarmTimeBoundsTransform
) {
	val startTime = uiState.alarmData.startTimeCalendar
	val endTime = uiState.alarmData.endTimeCalendar

	val density = LocalDensity.current
	val containerSize = LocalWindowInfo.current.containerSize
	val screenHeightDp = with(density) { containerSize.height.toDp() }
	val screenWidthDp = with(density) { containerSize.width.toDp() }
	val baseTimeStyle = if (screenWidthDp < 380.dp) typography.displaySmall else typography.displayMedium
	val timeStyle = baseTimeStyle.copy(fontWeight = FontWeight.Black)
	val amPmStyle = typography.titleSmall.copy(fontWeight = FontWeight.Bold)

	val titleSpacing = (screenHeightDp * 0.03f).coerceIn(12.dp, 28.dp)
	val locale = LocalLocale.current.platformLocale
	val zoneId = remember { ZoneId.systemDefault() }
	val fullTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }
	val clockFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm ", locale) }
	val amPmFormatter = remember(locale) { DateTimeFormatter.ofPattern("a", locale) }

	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker by remember { mutableStateOf(false) }

	val doWeHaveError = uiState.validationResult is AlarmDataValidationResult.TimeIntervalError
	val errorMessage = if (uiState.validationResult is AlarmDataValidationResult.TimeIntervalError) {
		uiState.validationResult.errorMessageToDisplayToUser.asString()
	} else {
		""
	}

	val spatialIntSizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
	val effectsFloatSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

	if (showStartTimePicker) {
		val timePickerState = rememberTimePickerState(
			initialHour = startTime.get(Calendar.HOUR_OF_DAY),
			initialMinute = startTime.get(Calendar.MINUTE),
			is24Hour = false
		)
		val confirmInteraction = remember { MutableInteractionSource() }
		val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
		val confirmCorner by animateDpAsState(
			targetValue = if (isConfirmPressed) 12.dp else 20.dp,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "start_confirm_corner"
		)

		val dismissInteraction = remember { MutableInteractionSource() }
		val isDismissPressed by dismissInteraction.collectIsPressedAsState()
		val dismissCorner by animateDpAsState(
			targetValue = if (isDismissPressed) 12.dp else 20.dp,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "start_dismiss_corner"
		)

		TimePickerDialog(
			onDismissRequest = { showStartTimePicker = false },
			shape = RoundedCornerShape(32.dp),
			containerColor = colorScheme.surfaceContainerHigh,
			confirmButton = {
				Button(
					onClick = {
						val newTime =
							(startTime.clone() as Calendar).apply {
								set(Calendar.HOUR_OF_DAY, timePickerState.hour)
								set(Calendar.MINUTE, timePickerState.minute)
							}
						onStartTimeChange(newTime)
						showStartTimePicker = false
					},
					interactionSource = confirmInteraction,
					shape = RoundedCornerShape(confirmCorner),
					contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
				) {
					Text(
						text = stringResource(R.string.alarm_picker_ok),
						style = typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
					)
				}
			},
			dismissButton = {
				FilledTonalButton(
					onClick = { showStartTimePicker = false },
					interactionSource = dismissInteraction,
					shape = RoundedCornerShape(dismissCorner),
					contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
				) {
					Text(
						text = stringResource(R.string.alarm_picker_cancel),
						style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
					)
				}
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
		) {
			val view = LocalView.current
			var isFirstRun by remember { mutableStateOf(true) }
			LaunchedEffect(timePickerState.hour, timePickerState.minute) {
				if (isFirstRun) {
					isFirstRun = false
				} else {
					view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
				}
			}
			TimePicker(timePickerState)
		}
	}

	if (showEndTimePicker) {
		val endTimePickerState = rememberTimePickerState(
			initialHour = endTime.get(Calendar.HOUR_OF_DAY),
			initialMinute = endTime.get(Calendar.MINUTE),
			is24Hour = false
		)
		val startHour = startTime.get(Calendar.HOUR_OF_DAY)
		val startMinute = startTime.get(Calendar.MINUTE)
		val (minEndHour, minEndMinute) = if (startMinute == 59 && startHour < 23) {
			(startHour + 1) to 0
		} else {
			startHour to (startMinute + 1).coerceAtMost(59)
		}

		val candidateEnd = (endTime.clone() as Calendar).apply {
			set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
			set(Calendar.MINUTE, endTimePickerState.minute)
		}
		val isCandidateInvalid = candidateEnd.timeInMillis <= startTime.timeInMillis

		val confirmInteraction = remember { MutableInteractionSource() }
		val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
		val confirmCorner by animateDpAsState(
			targetValue = if (isConfirmPressed) 12.dp else 20.dp,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "end_confirm_corner"
		)

		val dismissInteraction = remember { MutableInteractionSource() }
		val isDismissPressed by dismissInteraction.collectIsPressedAsState()
		val dismissCorner by animateDpAsState(
			targetValue = if (isDismissPressed) 12.dp else 20.dp,
			animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
			label = "end_dismiss_corner"
		)

		TimePickerDialog(
			onDismissRequest = { showEndTimePicker = false },
			shape = RoundedCornerShape(32.dp),
			containerColor = colorScheme.surfaceContainerHigh,
			confirmButton = {
				Button(
					onClick = {
						onEndTimeChange(candidateEnd)
						showEndTimePicker = false
					},
					enabled = !isCandidateInvalid,
					interactionSource = confirmInteraction,
					shape = RoundedCornerShape(confirmCorner),
					contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
				) {
					Text(
						text = stringResource(R.string.alarm_picker_ok),
						style = typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
					)
				}
			},
			dismissButton = {
				FilledTonalButton(
					onClick = { showEndTimePicker = false },
					interactionSource = dismissInteraction,
					shape = RoundedCornerShape(dismissCorner),
					contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
				) {
					Text(
						text = stringResource(R.string.alarm_picker_cancel),
						style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
					)
				}
			},
			title = {
				Column {
					Text(
						text = stringResource(R.string.alarm_picker_select_end_time),
						style = typography.titleMedium,
						color = colorScheme.onSurfaceVariant,
					)
					AnimatedVisibility(
						visible = isCandidateInvalid,
						enter = expandVertically(spatialIntSizeSpec) + fadeIn(effectsFloatSpec),
						exit = shrinkVertically(spatialIntSizeSpec) + fadeOut(effectsFloatSpec)
					) {
						val startTimeString = remember(uiState.alarmData.startTime, fullTimeFormatter, zoneId) {
							Instant.ofEpochMilli(uiState.alarmData.startTime).atZone(zoneId).format(fullTimeFormatter)
						}
						Text(
							text = stringResource(R.string.alarm_error_fix_alarm_time, startTimeString),
							style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
							color = colorScheme.error,
							modifier = Modifier.padding(top = 6.dp)
						)
					}
					Spacer(modifier = Modifier.height(titleSpacing))
				}
			}
		) {
			val view = LocalView.current
			var isFirstRun by remember { mutableStateOf(true) }
			LaunchedEffect(endTimePickerState.hour, endTimePickerState.minute) {
				if (isFirstRun) {
					isFirstRun = false
				} else {
					view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
				}
			}
			TimePicker(
				state = endTimePickerState,
				minHour = minEndHour,
				minMinute = minEndMinute,
				onDisabledTimeSelected = onDisabledTimeSelected
			)
		}
	}

	// Helper to conditionally apply sharedElement modifier
	val canAnimate = sharedTransitionScope != null && animatedVisibilityScope != null && alarmId != null

	val timeColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground
	val amPmColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground

	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier
				.weight(1f)
				.clickable { showStartTimePicker = !showStartTimePicker },
			horizontalArrangement = Arrangement.Start
		) {
			Row(
				verticalAlignment = Alignment.Bottom,
				modifier = Modifier.then(
					if (canAnimate) {
						with(sharedTransitionScope) {
							Modifier.sharedElement(
								sharedContentState = rememberSharedContentState(key = "start_time_${alarmId}"),
								animatedVisibilityScope = animatedVisibilityScope,
								boundsTransform = boundsTransform
							)
						}
					} else Modifier
				)
			) {
				val startClock = remember(uiState.alarmData.startTime, clockFormatter, zoneId) {
					Instant.ofEpochMilli(uiState.alarmData.startTime).atZone(zoneId).format(clockFormatter)
				}
				val startAmPm = remember(uiState.alarmData.startTime, amPmFormatter, zoneId) {
					Instant.ofEpochMilli(uiState.alarmData.startTime).atZone(zoneId).format(amPmFormatter)
				}
				Text(
					text = startClock,
					style = timeStyle,
					color = timeColor,
					maxLines = 1,
					softWrap = false,
					modifier = Modifier.alignByBaseline()
				)
				Text(
					text = startAmPm,
					style = amPmStyle,
					color = amPmColor,
					maxLines = 1,
					softWrap = false,
					modifier = Modifier.alignByBaseline()
				)
			}
		}
		Icon(
			imageVector = Icons.AutoMirrored.Filled.ArrowForward,
			contentDescription = null,
			tint = colorScheme.onBackground,
			modifier = Modifier
				.size(32.dp)
				.then(
					if (canAnimate) {
						with(sharedTransitionScope) {
							Modifier.sharedElement(
								sharedContentState = rememberSharedContentState(key = "arrow_${alarmId}"),
								animatedVisibilityScope = animatedVisibilityScope,
								boundsTransform = boundsTransform
							)
						}
					} else Modifier
				)
		)
		Row(
			verticalAlignment = Alignment.Bottom,
			modifier = Modifier
				.weight(1f)
				.clickable { showEndTimePicker = !showEndTimePicker },
			horizontalArrangement = Arrangement.End
		) {
			Row(
				verticalAlignment = Alignment.Bottom,
				modifier = Modifier.then(
					if (canAnimate) {
						with(sharedTransitionScope) {
							Modifier.sharedElement(
								sharedContentState = rememberSharedContentState(key = "end_time_${alarmId}"),
								animatedVisibilityScope = animatedVisibilityScope,
								boundsTransform = boundsTransform
							)
						}
					} else Modifier
				)
			) {
				val endClock = remember(uiState.alarmData.endTime, clockFormatter, zoneId) {
					Instant.ofEpochMilli(uiState.alarmData.endTime).atZone(zoneId).format(clockFormatter)
				}
				val endAmPm = remember(uiState.alarmData.endTime, amPmFormatter, zoneId) {
					Instant.ofEpochMilli(uiState.alarmData.endTime).atZone(zoneId).format(amPmFormatter)
				}
				Text(
					text = endClock,
					style = timeStyle,
					color = timeColor,
					maxLines = 1,
					softWrap = false,
					modifier = Modifier.alignByBaseline()
				)
				Text(
					text = endAmPm,
					style = amPmStyle,
					color = amPmColor,
					maxLines = 1,
					softWrap = false,
					modifier = Modifier.alignByBaseline()
				)
			}
		}
	}

	Column(modifier = Modifier.fillMaxWidth()) {
		// text in center as I want to draw attention to it and left aligned one looked ugly
		Spacer(modifier = Modifier.padding(3.dp))
		AnimatedVisibility(
			visible = doWeHaveError,
			enter = expandVertically(spatialIntSizeSpec) + fadeIn(effectsFloatSpec),
			exit = shrinkVertically(spatialIntSizeSpec) + fadeOut(effectsFloatSpec)
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
