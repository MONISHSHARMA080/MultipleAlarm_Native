package com.coolApps.MultipleAlarmClock.presentation.onboarding.v1.components

import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.CreateFirstAlarmScreen

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.presentation.picker.TimePickerWithoutDialog
import com.coolApps.MultipleAlarmClock.presentation.picker.onDisabledTimeSelected
import com.coolApps.MultipleAlarmClock.presentation.picker.rememberAdaptiveHorizontalPadding
import java.util.Calendar

private enum class OnboardingAlarmStep {
	StartTime,
	EndTime,
	FullEditor,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTimeStepperAlarmScreen(
	onAlarmSetProceed: () -> Unit,
	modifier: Modifier = Modifier,
	linearProgressBar: (@Composable () -> Unit)? = null,
	onShowPaywall: (PaywallTriggerType) -> Unit = {},
	onBackOverrideChanged: ((() -> Unit)?) -> Unit = {},
	onCancelled: () -> Unit = {},
) {
	val alarmPickerViewModel: AlarmPickerViewModel =
		hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory ->
			factory.create(null)
		}
	val uiState by alarmPickerViewModel.uiState.collectAsStateWithLifecycle()
	var currentStep by rememberSaveable { mutableStateOf(OnboardingAlarmStep.StartTime) }
	var nestedEditorBackOverride by remember { mutableStateOf<(() -> Unit)?>(null) }
	val view = LocalView.current
	val horizontalPadding = rememberAdaptiveHorizontalPadding()

	val handleStepBack: () -> Unit = {
		val nested = nestedEditorBackOverride
		when {
			nested != null -> nested()
			currentStep == OnboardingAlarmStep.FullEditor -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
				}
				currentStep = OnboardingAlarmStep.EndTime
			}
			currentStep == OnboardingAlarmStep.EndTime -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
				}
				currentStep = OnboardingAlarmStep.StartTime
			}
			else -> onCancelled()
		}
	}

	LaunchedEffect(currentStep, nestedEditorBackOverride) {
		onBackOverrideChanged(
			if (nestedEditorBackOverride != null || currentStep != OnboardingAlarmStep.StartTime) {
				handleStepBack
			} else {
				null
			},
		)
	}
	DisposableEffect(Unit) {
		onDispose {
			onBackOverrideChanged(null)
		}
	}

	BackHandler(enabled = nestedEditorBackOverride != null || currentStep != OnboardingAlarmStep.StartTime) {
		handleStepBack()
	}

	val stepperSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
	val stepperEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

	AnimatedContent(
		targetState = currentStep,
		modifier = modifier.fillMaxSize(),
		transitionSpec = {
			val direction = if (targetState.ordinal > initialState.ordinal) {
				AnimatedContentTransitionScope.SlideDirection.Left
			} else {
				AnimatedContentTransitionScope.SlideDirection.Right
			}
			slideIntoContainer(
				towards = direction,
				animationSpec = stepperSpatialSpec,
			) + fadeIn(stepperEffectsSpec) togetherWith
				slideOutOfContainer(
					towards = direction,
					animationSpec = stepperSpatialSpec,
				) + fadeOut(stepperEffectsSpec)
		},
		label = "onboarding_time_stepper",
	) { step ->
		when (step) {
			OnboardingAlarmStep.StartTime -> {
				val startTimePickerState = key(uiState.alarmData.startTime) {
					rememberTimePickerState(
						initialHour = uiState.alarmData.startTimeCalendar.get(Calendar.HOUR_OF_DAY),
						initialMinute = uiState.alarmData.startTimeCalendar.get(Calendar.MINUTE),
						is24Hour = false,
					)
				}
				Scaffold(
					contentWindowInsets = WindowInsets(0),
					bottomBar = {
						StepperBottomBar(
							backText = stringResource(R.string.alarm_picker_cancel),
							confirmEnabled = true,
							onBack = onCancelled,
							onConfirm = {
								val selectedStartTime = (uiState.alarmData.startTimeCalendar.clone() as Calendar).apply {
									set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
									set(Calendar.MINUTE, startTimePickerState.minute)
								}
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								alarmPickerViewModel.updateStartTime(selectedStartTime)
								currentStep = OnboardingAlarmStep.EndTime
							},
						)
					},
				) { screenPadding ->
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(screenPadding),
						contentAlignment = Alignment.Center,
					) {
						TimePickerWithoutDialog(
							state = startTimePickerState,
							modifier = Modifier.padding(horizontal = horizontalPadding),
							uiState = uiState,
							onDisabledTimeSelected = { onDisabledTimeSelected(view) },
						)
					}
				}
			}

			OnboardingAlarmStep.EndTime -> {
				val startCal = uiState.alarmData.startTimeCalendar
				val startHour = startCal.get(Calendar.HOUR_OF_DAY)
				val startMinute = startCal.get(Calendar.MINUTE)
				val (minEndHour, minEndMinute) = if (startMinute == 59 && startHour < 23) {
					(startHour + 1) to 0
				} else {
					startHour to (startMinute + 1).coerceAtMost(59)
				}

				val endTimePickerState = key(uiState.alarmData.endTime) {
					rememberTimePickerState(
						initialHour = uiState.alarmData.endTimeCalendar.get(Calendar.HOUR_OF_DAY),
						initialMinute = uiState.alarmData.endTimeCalendar.get(Calendar.MINUTE),
						is24Hour = false,
					)
				}
				val candidateEnd = remember(
					endTimePickerState.hour,
					endTimePickerState.minute,
					uiState.alarmData.startTime,
				) {
					(startCal.clone() as Calendar).apply {
						set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
						set(Calendar.MINUTE, endTimePickerState.minute)
					}
				}
				val isCandidateInvalid = candidateEnd.timeInMillis <= uiState.alarmData.startTime

				Scaffold(
					contentWindowInsets = WindowInsets(0),
					bottomBar = {
						StepperBottomBar(
							backText = stringResource(R.string.alarm_picker_previous),
							confirmEnabled = !isCandidateInvalid,
							onBack = {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
									view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
								}
								currentStep = OnboardingAlarmStep.StartTime
							},
							onConfirm = {
								if (!isCandidateInvalid) {
									view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
									alarmPickerViewModel.updateEndTime(candidateEnd)
									currentStep = OnboardingAlarmStep.FullEditor
								}
							},
						)
					},
				) { screenPadding ->
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(screenPadding),
						contentAlignment = Alignment.Center,
					) {
						TimePickerWithoutDialog(
							state = endTimePickerState,
							isCandidateInvalid = isCandidateInvalid,
							modifier = Modifier.padding(horizontal = horizontalPadding),
							uiState = uiState,
							minHour = minEndHour,
							minMin = minEndMinute,
							onDisabledTimeSelected = { onDisabledTimeSelected(view) },
						)
					}
				}
			}

			OnboardingAlarmStep.FullEditor -> {
				CreateFirstAlarmScreen(
					onAlarmSetProceed = { onAlarmSetProceed() },
					linearProgressBar = linearProgressBar,
					onShowPaywall = onShowPaywall,
					alarmPickerViewModel = alarmPickerViewModel,
					onBackOverrideChanged = { nestedEditorBackOverride = it },
					onCancelled = {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
							view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
						}
						currentStep = OnboardingAlarmStep.EndTime
					},
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepperBottomBar(
	backText: String,
	confirmEnabled: Boolean,
	onBack: () -> Unit,
	onConfirm: () -> Unit,
) {
	val backInteraction = remember { MutableInteractionSource() }
	val isBackPressed by backInteraction.collectIsPressedAsState()
	val backCorner by animateDpAsState(
		targetValue = if (isBackPressed) 16.dp else 28.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "stepper_back_corner",
	)
	val backScale by animateFloatAsState(
		targetValue = if (isBackPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "stepper_back_scale",
	)

	val confirmInteraction = remember { MutableInteractionSource() }
	val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
	val confirmCorner by animateDpAsState(
		targetValue = if (isConfirmPressed) 16.dp else 28.dp,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "stepper_confirm_corner",
	)
	val confirmScale by animateFloatAsState(
		targetValue = if (isConfirmPressed) 0.96f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "stepper_confirm_scale",
	)

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(colorScheme.background)
			.padding(16.dp)
			.padding(bottom = 20.dp),
		contentAlignment = Alignment.Center,
	) {
		Row(
			modifier = Modifier
				.widthIn(max = 600.dp)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			OutlinedButton(
				onClick = onBack,
				interactionSource = backInteraction,
				modifier = Modifier
					.height(56.dp)
					.graphicsLayer {
						scaleX = backScale
						scaleY = backScale
					},
				contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
				shape = RoundedCornerShape(backCorner),
			) {
				Text(
					text = backText,
					style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
				)
			}

			Button(
				onClick = onConfirm,
				enabled = confirmEnabled,
				interactionSource = confirmInteraction,
				colors = if (confirmEnabled) {
					ButtonDefaults.buttonColors(
						containerColor = colorScheme.primary,
						contentColor = colorScheme.onPrimary,
					)
				} else {
					ButtonDefaults.buttonColors(
						containerColor = colorScheme.surfaceVariant,
						contentColor = colorScheme.onSurfaceVariant,
					)
				},
				modifier = Modifier
					.height(56.dp)
					.graphicsLayer {
						scaleX = confirmScale
						scaleY = confirmScale
					},
				contentPadding = PaddingValues(horizontal = 36.dp, vertical = 0.dp),
				shape = RoundedCornerShape(confirmCorner),
			) {
				Text(
					text = stringResource(R.string.alarm_picker_ok),
					style = typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
				)
			}
		}
	}
}
