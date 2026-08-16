package com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker

import MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component.LinearProgressForNewAlarm
import MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component.SettingsCard
import MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component.TimePickerWithoutDialog
import MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component.TimeRow
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.coolApps.MultipleAlarmClock.R
import com.example.MultipleAlarmClock.Ui.Permissions.AlarmPermissionDialog
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import com.example.MultipleAlarmClock.Ui.alarmPicker.Progress
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPickerScreen(
	alarmSetProceed: () -> Unit,
	settingAlarmCancelled: ()->Unit,
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
      alarmSetProceed()
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
	val startTimePickerState = key(currentProgress, uiState.alarmObject.startTime.timeInMillis) {
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

  val isCandidateInvalid = currentProgress != Progress.StartTime &&  candidateEnd.timeInMillis <= uiState.alarmObject.startTime.timeInMillis

  Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          topBar = {
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
              TopAppBar(
                      title = {
						  AnimatedContent(
							  targetState = currentProgress,
							  transitionSpec = {
								  fadeIn(
									  animationSpec = tween(210)
								  ) + slideInVertically(
									  initialOffsetY = { it / 2 },
									  animationSpec = tween(220)
								  ) togetherWith
										  fadeOut(
											  animationSpec = tween(150)
										  ) + slideOutVertically(
									  targetOffsetY = { -it / 2 },
									  animationSpec = tween(190)
								  )
							  },
							  label = "alarm_picker_title"
						  ) { progress ->
							  Text(
								  when (progress) {
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

						  }
                      },
                      navigationIcon = {
                        IconButton(
                                onClick = {
                                  settingAlarmCancelled()
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
                CancelAndDeleteButton(
                        currentProgress = currentProgress,
                        isNewAlarm = forNewAlarm,
                        onClick = {
                          when (currentProgress) {
                            Progress.StartTime -> settingAlarmCancelled()
                            Progress.EndTime -> viewModel.updateProgress(Progress.StartTime)
                            Progress.FullEditor -> {
                              if (forNewAlarm) {
                                viewModel.updateProgress(Progress.EndTime)
                              } else {
                                viewModel.onDeleteClicked()
                              }
                            }
                          }
                        }
                )

                PrimaryActionButton(
                        currentProgress = currentProgress,
                        uiState = uiState,
                        isCandidateInvalid = isCandidateInvalid,
                        onAction = {
                          when (currentProgress) {
                            Progress.StartTime -> {
                              val selectedStartTime = (uiState.alarmObject.startTime.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                                set(Calendar.MINUTE, startTimePickerState.minute)
                              }
                              viewModel.updateStartTime(selectedStartTime)
                              viewModel.updateProgress(Progress.EndTime)
                              view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            }

                            Progress.EndTime -> {
                              if (!isCandidateInvalid) {
                                viewModel.updateEndTime(candidateEnd)
                                viewModel.updateProgress(Progress.FullEditor)
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                              }
                            }

                            Progress.FullEditor -> {
//                              val isNotDiff = uiState.validationResult is ValidationResult.Failure &&
//                                      (uiState.validationResult as ValidationResult.Failure).field == AlarmErrorField.AlarmIsNotDiff
                              val isInactiveEdit =  uiState.initialAlarm?.isReadyToUse == false
                              val canSetAlarm = uiState.validationResult == ValidationResult.Success || isInactiveEdit

                              if (canSetAlarm) {
                                viewModel.onSetAlarmClicked()
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                              }
                            }
                          }
                        }
                )
              }
            }
          }
  ) { screenPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(screenPadding)
        .consumeWindowInsets(screenPadding)
        .animateContentSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      AnimatedVisibility(
        visible = currentProgress != Progress.FullEditor,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        LinearProgressForNewAlarm(
          progress = currentProgress,
          modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 5.dp)
        )
      }

      AnimatedContent(
        targetState = currentProgress,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        transitionSpec = {
			  val direction = if (targetState.ordinal > initialState.ordinal) {
				AnimatedContentTransitionScope.SlideDirection.Left
			  } else {
				AnimatedContentTransitionScope.SlideDirection.Right
			  }
			  slideIntoContainer(
				towards = direction,
				animationSpec = tween(
				  270,
				  easing = FastOutSlowInEasing
				)
			  ) + fadeIn(
				animationSpec = tween(250)
			  ) togetherWith
				  slideOutOfContainer(
					towards = direction,
					animationSpec = tween(
					  110,
					  easing = FastOutSlowInEasing
					)
				  ) + fadeOut(
				animationSpec = tween(180)
			  )
			},
        label = "alarm_picker_navigation"
      ) { progress ->
        when (progress) {
          Progress.StartTime -> {
            TimePickerWithoutDialog(
              state = startTimePickerState,
              modifier = Modifier.padding(horizontal = horizontalPadding),
              uiState = uiState
            )
          }

          Progress.EndTime -> {
            TimePickerWithoutDialog(
              state = endTimePickerState,
              isCandidateInvalid = isCandidateInvalid,
              modifier = Modifier.padding(horizontal = horizontalPadding),
              uiState = uiState
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
                { viewModel.updateStartTime(it) },
                { viewModel.updateEndTime(it) },
              )
              Spacer(modifier = Modifier.weight(0.44f))
              SettingsCard(
                uiState = uiState,
                updateFrequency = { viewModel.updateFrequency(it) },
                messageValueChanged = { viewModel.updateMessage(it) },
                calenderButtonClicked = { showCalendar = true },
                selectSoundButtonClicked = onNavigateToSoundList,
                selectedSoundName = selectedSound?.title ?: stringResource(R.string.alarm_picker_sound_random)
              )
            }
          }
        }
      }
    }
  }
}


@Composable
fun CancelAndDeleteButton(
        currentProgress: Progress,
        isNewAlarm: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
  val isDeleteMode = currentProgress == Progress.FullEditor && !isNewAlarm

  AnimatedContent(
          targetState = isDeleteMode,
          transitionSpec = {
            fadeIn() togetherWith fadeOut() using SizeTransform()
          },
          label = "cancel_delete_button_animation"
  ) { targetIsDelete ->
    if (targetIsDelete) {
      TextButton(
              onClick = onClick,
              modifier = modifier.height(56.dp),
              shape = RoundedCornerShape(32.dp),
              colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error)
      ) {
        Text(
                text = stringResource(R.string.alarm_picker_delete_alarm),
                style = typography.bodyLarge,
        )
      }
    } else {
      OutlinedButton(
              onClick = onClick,
              modifier = modifier.height(56.dp),
              contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
              shape = RoundedCornerShape(32.dp)
      ) {
        AnimatedContent(
                targetState = currentProgress,
                transitionSpec = {
                  fadeIn() togetherWith fadeOut() using SizeTransform()
                },
                label = "cancel_button_text"
        ) { progress ->
          Text(
                  text = when (progress) {
                    Progress.StartTime -> stringResource(R.string.alarm_picker_cancel)
                    Progress.EndTime -> stringResource(R.string.alarm_picker_previous)
                    Progress.FullEditor -> stringResource(R.string.alarm_picker_previous)
                  },
                  style = typography.bodyLarge,
          )
        }
      }
    }
  }
}

@Composable
fun PrimaryActionButton(
        currentProgress: Progress,
        uiState: com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerUiState,
        isCandidateInvalid: Boolean,
        onAction: () -> Unit,
        modifier: Modifier = Modifier
) {
  AnimatedContent(
          targetState = currentProgress,
          transitionSpec = {
            fadeIn() togetherWith fadeOut() using SizeTransform()
          },
          label = "primary_action_button_animation"
  ) { progress ->
    when (progress) {
      Progress.StartTime, Progress.EndTime -> {
        Button(
                onClick = onAction,
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
                modifier = modifier.height(56.dp),
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
//        val isNotDiff = uiState.validationResult is ValidationResult.Failure &&
//                uiState.validationResult.field == AlarmErrorField.AlarmIsNotDiff
        val isInactiveEdit =  uiState.initialAlarm?.isReadyToUse == false
        val canSetAlarm = uiState.validationResult == ValidationResult.Success || isInactiveEdit

        Button(
                onClick = onAction,
                colors = when {
                  canSetAlarm -> {
                    ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer
                    )
                  }
                  uiState.validationResult is ValidationResult.Failure -> {
                      ButtonDefaults.buttonColors(
                              containerColor = colorScheme.errorContainer,
                              contentColor = colorScheme.onErrorContainer
                      )
                  }
                  else -> ButtonDefaults.buttonColors()
                },
                modifier = modifier.height(56.dp),
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

@Composable fun rememberAdaptiveHorizontalPadding(percent: Float = 0.0066f, min: Dp = 14.dp, max: Dp = 30.dp): Dp {
  val density = LocalDensity.current
  val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
  return (screenWidthDp * percent).coerceIn(min, max)
}


