package com.coolApps.MultipleAlarmClock.presentation.picker

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.AlarmPermissionDialog
import java.util.Calendar

// ─── Spring physics shared across all shared-element transitions ───────────────

private val AlarmMotionSpring = spring<Rect>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val AlarmBoundsTransform = BoundsTransform { _initialBounds, _targetBounds -> AlarmMotionSpring }

// ─── Public entry-point screens ───────────────────────────────────────────────

/**
 * Edit-alarm route entry point.
 * Owns the Scaffold with sharedBounds so the card-to-screen hero transition
 * works. Always shows Progress.FullEditor — no stepper inside.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun EditAlarmScreen(
		alarmSetProceed: () -> Unit,
		settingAlarmCancelled: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		onNavigateToPaywall: (Boolean) -> Unit,
		alarmData: AlarmData,
		viewModel: AlarmPickerViewModel,
		sharedTransitionScope: SharedTransitionScope? = null,
		animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val uiState by viewModel.uiState.collectAsState()
	val isPremium by viewModel.isPremium.collectAsState()
	val selectedSound by viewModel.selectedAlarmSound.collectAsState()
	val view = LocalView.current
	val context = LocalContext.current

	LaunchedEffect(uiState, isPremium) {
		logD("ui state:$uiState")
		logD("isPremium:$isPremium")
	}
	LaunchedEffect(uiState.alarmOperationCompletedGoBack) {
		if (uiState.alarmOperationCompletedGoBack) {
			viewModel.consumeAlarmOperationCompleted()
			alarmSetProceed()
		}
	}
	LaunchedEffect(uiState.showPaywall) {
		if (uiState.showPaywall) {
			onNavigateToPaywall(true)
			viewModel.navigationToPaywallComplete()
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkPermissions(context)
		onPauseOrDispose { }
	}

	if (uiState.showPermissionDialog) {
		AlarmPermissionDialog(
			uiState.missingSteps,
			onAllCriticalGranted = { viewModel.dismissPermissionDialog() },
			onDismiss = { viewModel.dismissPermissionDialog() },
			onTrackEvent = { event, prop -> viewModel.captureEvent(event, prop) }
		)
	}

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

	val horizontalPadding = rememberAdaptiveHorizontalPadding()

	// Build the optional sharedBounds scaffold modifier
	val scaffoldModifier: Modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
		with(sharedTransitionScope) {
			Modifier.sharedBounds(
				sharedContentState = rememberSharedContentState(key = "alarm_card_${alarmData.id}"),
				animatedVisibilityScope = animatedVisibilityScope,
				clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp)),
				boundsTransform = AlarmBoundsTransform
			)
		}
	} else {
		Modifier
	}

	Scaffold(
		modifier = scaffoldModifier,
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.statusBarsPadding()
					.padding(horizontal = 8.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				IconButton(onClick = { settingAlarmCancelled() }) {
					Icon(
						imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
						contentDescription = stringResource(R.string.alarm_picker_back_desc)
					)
				}
			}
		},
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
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
					// Delete button (edit mode — always FullEditor, never new alarm)
					CancelAndDeleteButton(
						currentProgress = Progress.FullEditor,
						isNewAlarm = false,
						onClick = { viewModel.onDeleteClicked() }
					)

					val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
					val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
					PrimaryActionButton(
						currentProgress = Progress.FullEditor,
						uiState = uiState,
						isCandidateInvalid = false,
						onAction = {
							if (canSetAlarm) {
								viewModel.onSetAlarmClicked()
								view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
							}
						}
					)
				}
			}
		}
	) { screenPadding ->
		AlarmEditorBody(
			modifier = Modifier
				.fillMaxSize()
				.padding(screenPadding)
				.consumeWindowInsets(screenPadding)
				.animateContentSize()
				.padding(horizontal = horizontalPadding),
			uiState = uiState,
			viewModel = viewModel,
			selectedSoundTitle = selectedSound?.title,
			alarmId = alarmData.id,
			showCalendarRequest = { showCalendar = true },
			onNavigateToSoundList = onNavigateToSoundList,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			animateScroll = false
		)
	}
}

/**
 * New-alarm route entry point.
 * Owns the Scaffold with StartTime → EndTime → FullEditor stepper.
 * No sharedBounds on the scaffold. AnimatedContent stepper is kept as-is.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NewAlarmScreen(
		alarmSetProceed: () -> Unit,
		settingAlarmCancelled: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		onNavigateToPaywall: (Boolean) -> Unit,
		linearProgressBar: (@Composable () -> Unit)? = null,
		viewModel: AlarmPickerViewModel,
		// kept for API compatibility but unused in this route (no shared-element)
		sharedTransitionScope: SharedTransitionScope? = null,
		animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val fromOnboarding = linearProgressBar != null
	logD("fromOnboarding:$fromOnboarding")

	val uiState by viewModel.uiState.collectAsState()
	val isPremium by viewModel.isPremium.collectAsState()
	val selectedSound by viewModel.selectedAlarmSound.collectAsState()
	val view = LocalView.current
	val context = LocalContext.current

	LaunchedEffect(uiState, isPremium) {
		logD("ui state:$uiState")
		logD("isPremium:$isPremium")
	}
	LaunchedEffect(uiState.alarmOperationCompletedGoBack) {
		if (uiState.alarmOperationCompletedGoBack) {
			viewModel.consumeAlarmOperationCompleted()
			alarmSetProceed()
		}
	}
	LaunchedEffect(uiState.showPaywall) {
		if (uiState.showPaywall) {
			onNavigateToPaywall(true)
			viewModel.navigationToPaywallComplete()
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkPermissions(context)
		onPauseOrDispose { }
	}

	if (uiState.showPermissionDialog) {
		AlarmPermissionDialog(
			uiState.missingSteps,
			onAllCriticalGranted = { viewModel.dismissPermissionDialog() },
			onDismiss = { viewModel.dismissPermissionDialog() },
			onTrackEvent = { event, prop -> viewModel.captureEvent(event, prop) }
		)
	}

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

	val horizontalPadding = rememberAdaptiveHorizontalPadding()
	val currentProgress = if (fromOnboarding) uiState.progress else Progress.FullEditor

	val startTimePickerState = key(currentProgress, uiState.alarmData.startTime) {
		rememberTimePickerState(
			initialHour = uiState.alarmData.startTimeCalendar.get(Calendar.HOUR_OF_DAY),
			initialMinute = uiState.alarmData.startTimeCalendar.get(Calendar.MINUTE),
			is24Hour = false
		)
	}
	val endTimePickerState = key(currentProgress) {
		rememberTimePickerState(
			initialHour = uiState.alarmData.endTimeCalendar.get(Calendar.HOUR_OF_DAY),
			initialMinute = uiState.alarmData.endTimeCalendar.get(Calendar.MINUTE),
			is24Hour = false
		)
	}
	val candidateEnd = remember(endTimePickerState.hour, endTimePickerState.minute, uiState.alarmData.startTime) {
		(uiState.alarmData.endTimeCalendar.clone() as Calendar).apply {
			set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
			set(Calendar.MINUTE, endTimePickerState.minute)
		}
	}
	val isCandidateInvalid = currentProgress != Progress.StartTime && candidateEnd.timeInMillis <= uiState.alarmData.startTime

	Scaffold(
		contentWindowInsets = WindowInsets.safeDrawing,
		topBar = {
			if (!fromOnboarding) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.statusBarsPadding()
						.padding(horizontal = 8.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					IconButton(onClick = { settingAlarmCancelled() }) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = stringResource(R.string.alarm_picker_back_desc)
						)
					}
				}
			}
		},
		bottomBar = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
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
						isNewAlarm = true,
						fromOnboarding = fromOnboarding,
						onClick = {
							when (currentProgress) {
								Progress.StartTime -> settingAlarmCancelled()
								Progress.EndTime -> {
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
										view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
									}
									viewModel.updateProgress(Progress.StartTime)
								}
								Progress.FullEditor -> {
									if (fromOnboarding) {
										if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
											view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
										}
										viewModel.updateProgress(Progress.EndTime)
									} else {
										settingAlarmCancelled()
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
							if (fromOnboarding) {
								when (currentProgress) {
									Progress.StartTime -> {
										val selectedStartTime = (uiState.alarmData.startTimeCalendar.clone() as Calendar).apply {
											set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
											set(Calendar.MINUTE, startTimePickerState.minute)
										}
										view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
										viewModel.updateStartTime(selectedStartTime)
										viewModel.updateProgress(Progress.EndTime)
									}
									Progress.EndTime -> {
										if (!isCandidateInvalid) {
											view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
											viewModel.updateEndTime(candidateEnd)
											viewModel.updateProgress(Progress.FullEditor)
										}
									}
									Progress.FullEditor -> {
										val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
										val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
										if (canSetAlarm) {
											viewModel.onSetAlarmClicked()
											view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
										}
									}
								}
							} else {
								when (currentProgress) {
									Progress.StartTime -> {
										val selectedStartTime = (uiState.alarmData.startTimeCalendar.clone() as Calendar).apply {
											set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
											set(Calendar.MINUTE, startTimePickerState.minute)
										}
										view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
										viewModel.updateStartTime(selectedStartTime)
										viewModel.updateProgress(Progress.EndTime)
									}
									Progress.EndTime -> {
										if (!isCandidateInvalid) {
											view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
											viewModel.updateEndTime(candidateEnd)
											viewModel.updateProgress(Progress.FullEditor)
										}
									}
									Progress.FullEditor -> {
										val isInactiveEdit = uiState.initialAlarm?.isReadyToUse == false
										val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit
										if (canSetAlarm) {
											viewModel.onSetAlarmClicked()
											view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
										}
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
		) {
			// Existing stepper AnimatedContent — leave transition as-is per spec
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
						animationSpec = tween(270, easing = FastOutSlowInEasing)
					) + fadeIn(
						animationSpec = tween(250)
					) togetherWith
							slideOutOfContainer(
								towards = direction,
								animationSpec = tween(110, easing = FastOutSlowInEasing)
							) + fadeOut(
						animationSpec = tween(190)
					)
				},
				contentAlignment = Alignment.Center,
				label = "alarm_picker_navigation"
			) { progress ->
				when (progress) {
					Progress.StartTime -> {
						TimePickerWithoutDialog(
							state = startTimePickerState,
							modifier = Modifier.padding(horizontal = horizontalPadding),
							uiState = uiState,
							onDisabledTimeSelected = { onDisabledTimeSelected(view) }
						)
					}
					Progress.EndTime -> {
						TimePickerWithoutDialog(
							state = endTimePickerState,
							isCandidateInvalid = isCandidateInvalid,
							modifier = Modifier.padding(horizontal = horizontalPadding),
							uiState = uiState,
							minHour = startTimePickerState.hour,
							minMin = if (startTimePickerState.minute == 59) 59 else startTimePickerState.minute + 1,
							onDisabledTimeSelected = { onDisabledTimeSelected(view) }
						)
					}
					Progress.FullEditor -> {
						AlarmEditorBody(
							modifier = Modifier
								.fillMaxSize()
								.padding(horizontal = horizontalPadding)
								.animateContentSize(),
							uiState = uiState,
							viewModel = viewModel,
							selectedSoundTitle = selectedSound?.title,
							alarmId = null,
							showCalendarRequest = { showCalendar = true },
							onNavigateToSoundList = onNavigateToSoundList,
							// NewAlarmScreen never participates in the card hero transition
							sharedTransitionScope = null,
							animatedVisibilityScope = null,
							animateScroll = true
						)
					}
				}
			}
		}
	}
}

// ─── Private body composable ──────────────────────────────────────────────────

/**
 * The TimeRow + SettingsCard + spacers layout, shared between both screens.
 * sharedTransitionScope / animatedVisibilityScope are forwarded to TimeRow
 * only when this is called from EditAlarmScreen (non-null).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AlarmEditorBody(
		modifier: Modifier = Modifier,
		uiState: AlarmPickerUiState,
		viewModel: AlarmPickerViewModel,
		selectedSoundTitle: String?,
		alarmId: Int?,
		showCalendarRequest: () -> Unit,
		onNavigateToSoundList: () -> Unit,
		sharedTransitionScope: SharedTransitionScope? = null,
		animatedVisibilityScope: AnimatedVisibilityScope? = null,
		animateScroll: Boolean
) {
	val view = LocalView.current

	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.weight(0.44f))
		TimeRow(
			uiState = uiState,
			onStartTimeChange = { viewModel.updateStartTime(it) },
			onEndTimeChange = { viewModel.updateEndTime(it) },
			onDisabledTimeSelected = { onDisabledTimeSelected(view) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			alarmId = alarmId,
			boundsTransform = AlarmBoundsTransform
		)
		Spacer(modifier = Modifier.weight(0.45f))
		SettingsCard(
			uiState = uiState,
			updateFrequency = { viewModel.updateFrequency(it) },
			messageValueChanged = { viewModel.updateMessage(it) },
			updateIsForceLoudVolume = { viewModel.updateIsForceLoudVolume(it) },
			calenderButtonClicked = showCalendarRequest,
			selectSoundButtonClicked = onNavigateToSoundList,
			repeatDayToggled = { day -> viewModel.onRepeatDayClicked(day) },
			selectedSoundName = selectedSoundTitle ?: stringResource(R.string.alarm_picker_sound_random),
			modifier = Modifier.weight(1f, fill = false),
			animateScroll = animateScroll
		)
		Spacer(modifier = Modifier.weight(0.04f))
	}
}

// ─── Shared bottom-bar buttons (unchanged logic) ──────────────────────────────

@Composable
fun CancelAndDeleteButton(
        currentProgress: Progress,
        modifier: Modifier = Modifier,
        isNewAlarm: Boolean,
        fromOnboarding: Boolean = false,
        onClick: () -> Unit,
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
				Progress.FullEditor -> if (isNewAlarm && !fromOnboarding) {
					stringResource(R.string.alarm_picker_cancel)
				} else {
					stringResource(R.string.alarm_picker_previous)
				}
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
        uiState: AlarmPickerUiState,
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
                          containerColor = colorScheme.primary,
                          contentColor = colorScheme.onPrimary
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
        val isInactiveEdit =  uiState.initialAlarm?.isReadyToUse == false
        val canSetAlarm = uiState.validationResult == AlarmDataValidationResult.Success || isInactiveEdit

        Button(
                onClick = onAction,
                colors = when {
                  canSetAlarm -> {
                    ButtonDefaults.buttonColors(
						containerColor = colorScheme.primary,
						contentColor = colorScheme.onPrimary
                    )
                  }
                  uiState.validationResult.isFailure() -> {
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

fun onDisabledTimeSelected(view: View) {
	view.performHapticFeedback(HapticFeedbackConstants.REJECT)
}

@Composable fun rememberAdaptiveHorizontalPadding(percent: Float = 0.0066f, min: Dp = 14.dp, max: Dp = 30.dp): Dp {
  val density = LocalDensity.current
  val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
  return (screenWidthDp * percent).coerceIn(min, max)
}
