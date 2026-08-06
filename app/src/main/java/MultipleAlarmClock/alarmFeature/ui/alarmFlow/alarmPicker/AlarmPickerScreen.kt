package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import MultipleAlarmClock.alarmFeature.domain.model.AlarmErrorField
import MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Ui.Permissions.AlarmPermissionDialog
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerUiState
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar


enum class Progress{StartTime, EndTime, FullEditor}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPickerScreen(
        alarmSetGoBack: () -> Unit,
        onNavigateToSoundList: () -> Unit,
		forNewAlarm: Boolean,
        viewModel: AlarmPickerViewModel
) {

  val uiState by viewModel.uiState.collectAsState()
  val selectedSound by viewModel.selectedAlarmSound.collectAsState()

  val view = LocalView.current
  val timeStyle = typography.headlineSmall
  val context = LocalContext.current


  LaunchedEffect(Unit) { viewModel.screen("AlarmPickerScreen") }

  LaunchedEffect(uiState.alarmOperationCompletedGoBack) {
    if (uiState.alarmOperationCompletedGoBack) {
      alarmSetGoBack()
      viewModel.updateUi(
              uiState.copy(alarmOperationCompletedGoBack = false)
      ) // Reset so it doesn't re-trigger
    }
  }

  LifecycleResumeEffect(Unit) {
    viewModel.checkPermissions(context)
    onPauseOrDispose {
      // Optional cleanup when the screen pauses/disposes
    }
  }

  LaunchedEffect(uiState.showPermissionDialog) {
    if (uiState.showPermissionDialog) {
      viewModel.captureEvent("ask for permission dialog opened", mapOf())
    }
  }

  if (uiState.showPermissionDialog) {
    AlarmPermissionDialog(
            uiState.missingSteps,
            onAllCriticalGranted = { viewModel.dismissPermissionDialog() },
            onDismiss = { viewModel.dismissPermissionDialog() },
            onTrackEvent = { event, prop -> viewModel.captureEvent(event, prop) }
    )
  }

  val horizontalPadding = rememberAdaptiveHorizontalPadding()
  var showCalendar by remember { mutableStateOf(false) }

  if (showCalendar) {
    DatePickerModal(
            onDateSelected = { date ->
              if (date != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                viewModel.updateDate(cal)
              }
              showCalendar = false
            },
            onDismiss = { showCalendar = false }
    )
  }

  val currentProgress = if (!forNewAlarm) Progress.FullEditor else uiState.progress

  val startTimePickerState = key(currentProgress) {
    rememberTimePickerState(
            initialHour = uiState.alarmObject.startTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = uiState.alarmObject.startTime.get(Calendar.MINUTE),
            is24Hour = false
    )
  }

  val endTimePickerState = key(currentProgress) {
    rememberTimePickerState(
            initialHour = uiState.alarmObject.endTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = uiState.alarmObject.endTime.get(Calendar.MINUTE),
            is24Hour = false
    )
  }

  val candidateEnd = remember(endTimePickerState.hour, endTimePickerState.minute, uiState.alarmObject.startTime) {
    (uiState.alarmObject.endTime.clone() as Calendar).apply {
      set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
      set(Calendar.MINUTE, endTimePickerState.minute)
    }
  }
  val isCandidateInvalid = candidateEnd.timeInMillis <= uiState.alarmObject.startTime.timeInMillis

  Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          topBar = {
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
              TopAppBar(
                      title = {
                        Text(
                                when (currentProgress) {
                                  Progress.StartTime -> stringResource(R.string.alarm_picker_select_start_time)
                                  Progress.EndTime -> stringResource(R.string.alarm_picker_select_end_time)
                                  Progress.FullEditor -> if (uiState.initialAlarm == null) stringResource(R.string.alarm_picker_title_set) else stringResource(R.string.alarm_picker_title_edit)
                                },
                                style = timeStyle,
                                color = colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 7.dp),
                                maxLines = 1,
                                softWrap = false,
                        )
                      },
                      navigationIcon = {
                        IconButton(
                                onClick = {
                                  alarmSetGoBack()
                                }
                        ) {
                          Icon(
                                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                  contentDescription = stringResource(R.string.alarm_picker_back_desc)
                          )
                        }
                      },
              )
            }
          },
          bottomBar = {
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(colorScheme.background)
                                    .navigationBarsPadding()
                                    .padding(16.dp)
                                    .padding(bottom = 20.dp)
                                    .animateContentSize(),
                    contentAlignment = Alignment.Center
            ) {
              Row(
                      modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                        onClick = {
                          when (currentProgress) {
                            Progress.StartTime -> alarmSetGoBack()
                            Progress.EndTime -> viewModel.updateProgress(Progress.StartTime)
                            Progress.FullEditor -> {
                              if (forNewAlarm) {
                                viewModel.updateProgress(Progress.EndTime)
                              } else {
								  viewModel.onDeleteClicked()
                              }
                            }
                          }
                        },
                        modifier = Modifier.height(56.dp).animateContentSize(),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(28.dp)
                ) {
                  Text(
                          text = when (currentProgress) {
                            Progress.StartTime -> stringResource(R.string.alarm_picker_cancel)
                            Progress.EndTime -> stringResource(R.string.alarm_picker_previous)
                            Progress.FullEditor ->{
								if (forNewAlarm) stringResource(R.string.alarm_picker_previous) else stringResource(R.string.alarm_picker_delete_alarm)
							}
                          },
                          style = typography.bodyLarge,
                  )
                }

                when (currentProgress) {
                  Progress.StartTime -> {
                    Button(
                            onClick = {
                              val selectedStartTime = (uiState.alarmObject.startTime.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                                set(Calendar.MINUTE, startTimePickerState.minute)
                              }
                              viewModel.updateStartTime(selectedStartTime)
                              viewModel.updateProgress(Progress.EndTime)
                              view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            },
                            colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primaryContainer,
                                    contentColor = colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.height(56.dp).animateContentSize(),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(28.dp)
                    ) {
                      Text(
                              stringResource(R.string.alarm_picker_ok),
                              style = typography.bodyLarge,
                      )
                    }
                  }
                  Progress.EndTime -> {
                    Button(
                            onClick = {
                              if (!isCandidateInvalid) {
                                viewModel.updateEndTime(candidateEnd)
                                viewModel.updateProgress(Progress.FullEditor)
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                              }
                            },
                            enabled = !isCandidateInvalid,
                            colors = if (!isCandidateInvalid) {
                              ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.primaryContainer,
                                      contentColor = colorScheme.onPrimaryContainer
                              )
                            } else {
                              ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.surfaceVariant,
                                      contentColor = colorScheme.onSurfaceVariant
                              )
                            },
                            modifier = Modifier.height(56.dp).animateContentSize(),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(28.dp)
                    ) {
                      Text(
                              stringResource(R.string.alarm_picker_ok),
                              style = typography.bodyLarge,
                      )
                    }
                  }
                  Progress.FullEditor -> {
                    val isNotDiff = uiState.validationResult is ValidationResult.Failure &&
                            (uiState.validationResult as ValidationResult.Failure).field == AlarmErrorField.AlarmIsNotDiff
                    val isInactiveEdit = isNotDiff && uiState.initialAlarm?.isReadyToUse == false
                    val canSetAlarm = uiState.validationResult == ValidationResult.Success || isInactiveEdit

                    Button(
                            onClick = {
                              if (canSetAlarm) {
                                viewModel.onSetAlarmClicked(uiState.initialAlarm, uiState.alarmObject)
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                              }
                            },
                            colors =
                                    when {
                                      canSetAlarm -> {
                                        ButtonDefaults.buttonColors(
                                                containerColor = colorScheme.primaryContainer,
                                                contentColor = colorScheme.onPrimaryContainer
                                        )
                                      }
                                      uiState.validationResult is ValidationResult.Failure -> {
                                        if (isNotDiff) {
                                          ButtonDefaults.buttonColors(
                                                  containerColor = colorScheme.surfaceVariant,
                                                  contentColor = colorScheme.onSurfaceVariant
                                          )
                                        } else {
                                          ButtonDefaults.buttonColors(
                                                  containerColor = colorScheme.errorContainer,
                                                  contentColor = colorScheme.onErrorContainer
                                          )
                                        }
                                      }
                                      else -> ButtonDefaults.buttonColors()
                                    },
                            modifier =
                                Modifier.height(56.dp)
                                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(28.dp)
                    ) {
                      AnimatedContent(
                              targetState = canSetAlarm,
                              transitionSpec = {
                                fadeIn() togetherWith fadeOut() using SizeTransform()
                              },
                              label = "button_text"
                      ) { isValid ->
                        Text(
                                when {
                                  isValid -> stringResource(R.string.alarm_picker_btn_set)
                                  isNotDiff -> stringResource(R.string.alarm_picker_btn_change)
                                  else -> stringResource(R.string.alarm_picker_btn_fix)
                                },
                                style = typography.bodyLarge,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
  ) { screenPadding ->
    AnimatedContent(
            targetState = currentProgress,
            modifier = Modifier.fillMaxSize().padding(screenPadding).consumeWindowInsets(screenPadding),
            transitionSpec = {
              fadeIn() togetherWith fadeOut() using SizeTransform()
            },
            label = "progress_content"
    ) { currentProgress ->

		if (currentProgress != Progress.FullEditor) {
			LinearProgressForNewAlarm(
				progress = currentProgress,
				modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 5.dp)
			)
		}

      when (currentProgress) {

        Progress.StartTime -> {
          TimePickerWithoutDialog(
                  state = startTimePickerState,
                  modifier = Modifier.padding(horizontal = horizontalPadding)
          )
        }

        Progress.EndTime -> {
          TimePickerWithoutDialog(
                  state = endTimePickerState,
                  isCandidateInvalid = isCandidateInvalid,
                  errorMessage = stringResource(R.string.alarm_error_time_range),
                  modifier = Modifier.padding(horizontal = horizontalPadding)
          )
        }

        Progress.FullEditor -> {
          Column(
                  modifier = Modifier.fillMaxSize()
							  .padding(horizontal = horizontalPadding)
							  .animateContentSize(),
                  horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Spacer(modifier = Modifier.weight(0.44f))
            TimeRow(
                    uiState,
                    {
                      viewModel.updateUi(
                              uiState.copy(alarmObject = uiState.alarmObject.copy(startTime = it))
                      )
                    },
                    {
                      viewModel.updateUi(
                              uiState.copy(alarmObject = uiState.alarmObject.copy(endTime = it))
                      )
                    }
            )
            Spacer(modifier = Modifier.weight(0.44f))

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
                        value = uiState.alarmObject.freqGottenAfterCallback,
                        onValueChange = { newValue ->
                          newValue.let {
                            logD("new freq: value is $it")
                            if (it in 0 ..< 720) {
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
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.alarm_picker_date),
                        value = SimpleDateFormat("EEE, MMM d, yyyy", LocalLocale.current.platformLocale).format(uiState.alarmObject.startTime.time),
                        onClick = { showCalendar = true }
                )

                HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colorScheme.outlineVariant,
                )

                SettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.alarm_picker_sound),
                        value = selectedSound?.title ?: stringResource(R.string.alarm_picker_sound_random),
                        onClick = onNavigateToSoundList
                )

                HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = colorScheme.outlineVariant,
                )

                MessageRow(
                        icon = Icons.AutoMirrored.Rounded.Message,
                        title = stringResource(R.string.alarm_picker_message),
                        value = uiState.alarmObject.message,
                        onValueChange = { viewModel.updateMessage(it) },
                )
              }
            }
          }
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

  val density = LocalDensity.current
  val containerSize = LocalWindowInfo.current.containerSize
  val screenWidthDp = with(density) { containerSize.width.toDp() }
  val screenHeightDp = with(density) { containerSize.height.toDp() }
val timeStyle = typography.displayMedium.copy(fontWeight = FontWeight.Bold)
val amPmStyle = typography.bodyMedium  // was labelLarge — bump up to match M3 TimePicker spec


  val titleSpacing = (screenHeightDp * 0.04f).coerceIn(12.dp, 36.dp)

  var showStartTimePicker by remember { mutableStateOf(false) }
  var showEndTimePicker by remember { mutableStateOf(false) }

  val doWeHaveError = uiState.validationResult != ValidationResult.Success && (uiState.validationResult as? ValidationResult.Failure)?.field == AlarmErrorField.Time
  val errorMessage = (uiState.validationResult as? ValidationResult.Failure)?.message
  val timeColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground
  val amPmColor = if (doWeHaveError) colorScheme.error else colorScheme.onBackground

  if (showStartTimePicker) {
    val timePickerState =
            rememberTimePickerState(
                    initialHour = startTime.get(Calendar.HOUR_OF_DAY),
                    initialMinute = startTime.get(Calendar.MINUTE),
                    is24Hour = false
            )
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
	  val endTimePickerState = rememberTimePickerState(
		  initialHour = endTime.get(Calendar.HOUR_OF_DAY),
		  initialMinute = endTime.get(Calendar.MINUTE),
		  is24Hour = false
	  )

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
					  Text(
						  text = stringResource(R.string.alarm_error_time_range), // "Must be after start time"
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

@Composable
fun rememberAdaptiveHorizontalPadding(
        percent: Float = 0.0062f,
        min: Dp = 14.dp,
        max: Dp = 30.dp
): Dp {
  val density = LocalDensity.current
  val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
  return (screenWidthDp * percent).coerceIn(min, max)
}

@Composable
fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
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

@Composable
private fun MessageRow(
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

@Composable
private fun FrequencyRow(
        icon: ImageVector,
        title: String,
        value: Long,
        onValueChange: (Long) -> Unit,
        previewText: String = "",
        uiState: AlarmPickerUiState,
) {
  val doWeHaveFrequencyError =
          (uiState.validationResult as? ValidationResult.Failure)?.field ==
                  AlarmErrorField.FREQUENCY
  val doWeHaveErrorOtherThanFrequency =
          (uiState.validationResult as? ValidationResult.Failure) != null &&
                  uiState.validationResult.field != AlarmErrorField.FREQUENCY

  logD(
          "preview text is isNotEmpty:${previewText.isNotEmpty()}, and doWeHaveFrequencyError:$doWeHaveFrequencyError, validation result: ${uiState.validationResult}, doWeHaveErrorOtherThanFrequency: $doWeHaveErrorOtherThanFrequency "
  )
  val view = LocalView.current

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

        BasicTextField(
                value = if (value == 0L) "" else value.toString(),
                onValueChange = { newValue ->
                  newValue.toLongOrNull()?.let { onValueChange(it) } ?: onValueChange(0)
                },
                modifier = Modifier.width(45.dp),
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
    AnimatedVisibility(
            visible =
                    !doWeHaveErrorOtherThanFrequency &&
                            (previewText.isNotEmpty() || doWeHaveFrequencyError),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
    ) {
      Spacer(modifier = Modifier.padding(3.dp))
      Text(
              text = previewText,
              style = typography.labelMedium,
              textAlign = TextAlign.Start,
              modifier = Modifier.padding(top = 5.dp, start = 2.dp).animateContentSize(),
              color =
                      if (doWeHaveFrequencyError) colorScheme.onErrorContainer
                      else colorScheme.onSurfaceVariant
      )
    }
  }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerWithoutDialog(
        state: TimePickerState,
        modifier: Modifier = Modifier,
        isCandidateInvalid: Boolean = false,
        errorMessage: String? = null,
) {
  Column(
          modifier = modifier
			  .fillMaxSize()
			  .padding(vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
  ) {
    TimePicker(state = state)

    AnimatedVisibility(visible = isCandidateInvalid) {
      Text(
              text = errorMessage ?: stringResource(R.string.alarm_error_time_range),
              style = typography.bodyMedium,
              color = colorScheme.error,
              modifier = Modifier.padding(top = 16.dp),
              textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun LinearProgressForNewAlarm(modifier: Modifier = Modifier, progress: Progress) {
  val (step, total) =
          when (progress) {
            Progress.StartTime -> 1 to 3
            Progress.EndTime -> 2 to 3
            Progress.FullEditor -> 3 to 3
          }
  val progressFraction = step / total.toFloat()

  Column(
          modifier =
                  modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
              text = "Step $step of $total",
              style = typography.labelSmall,
              fontWeight = FontWeight.Normal,
              color = colorScheme.onSurface
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth().height(5.dp).animateContentSize(),
            color = colorScheme.secondary,
            trackColor = colorScheme.surfaceContainerLow,
            strokeCap = StrokeCap.Round
    )
  }
}

