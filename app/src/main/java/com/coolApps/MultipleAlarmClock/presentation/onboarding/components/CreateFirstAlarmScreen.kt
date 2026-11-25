package com.coolApps.MultipleAlarmClock.presentation.onboarding.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.presentation.picker.NewAlarmScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.listAlarmRingtone.ListAlarmSoundScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.missions.AlarmMissionListScreen

private enum class CreateFirstAlarmStep {
	Picker,
	SoundList,
	MissionList
}

@Composable
fun CreateFirstAlarmScreen(
		onAlarmSetProceed: () -> Unit,
		modifier: Modifier = Modifier,
		linearProgressBar: (@Composable () -> Unit)? = null,
		onShowPaywall: (PaywallTriggerType) -> Unit = {},
		presetAlarm: AlarmData? = null,
		presetKey: String? = null,
		existingAlarmId: Int? = null,
		alarmPickerViewModel: AlarmPickerViewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory -> factory.create(null) },
		onBackOverrideChanged: ((() -> Unit)?) -> Unit = {},
		onCancelled: () -> Unit = {}
) {
	if (presetAlarm != null && presetKey != null) {
		LaunchedEffect(presetKey) {
			alarmPickerViewModel.applyPresetIfNeeded(presetAlarm, presetKey)
		}
	}
	LaunchedEffect(existingAlarmId) {
		alarmPickerViewModel.syncSavedAlarmId(existingAlarmId)
	}

	var currentStep by rememberSaveable { mutableStateOf(CreateFirstAlarmStep.Picker) }
	val selected by alarmPickerViewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val previewing by alarmPickerViewModel.previewingSound.collectAsStateWithLifecycle()

	LaunchedEffect(currentStep) {
		onBackOverrideChanged(
			if (currentStep != CreateFirstAlarmStep.Picker) {
				{ currentStep = CreateFirstAlarmStep.Picker }
			} else {
				null
			}
		)
	}
	DisposableEffect(Unit) {
		onDispose {
			onBackOverrideChanged(null)
		}
	}

	BackHandler(enabled = currentStep != CreateFirstAlarmStep.Picker) {
		currentStep = CreateFirstAlarmStep.Picker
	}

	AnimatedContent(
		targetState = currentStep,
		modifier = modifier,
		transitionSpec = {
			val isForward = targetState.ordinal > initialState.ordinal
			if (isForward) {
				(slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(320, easing = FastOutSlowInEasing)
				) + fadeIn(tween(250))) togetherWith (slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(220, easing = FastOutSlowInEasing)
				) + fadeOut(tween(190)))
			} else {
				(slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(320, easing = FastOutSlowInEasing)
				) + fadeIn(tween(250))) togetherWith (slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(220, easing = FastOutSlowInEasing)
				) + fadeOut(tween(190)))
			}
		},
	) { step ->
		when (step) {
			CreateFirstAlarmStep.Picker -> {
				NewAlarmScreen(
					alarmSetProceed = onAlarmSetProceed,
					
					viewModel = alarmPickerViewModel,
					onNavigateToSoundList = {
						currentStep = CreateFirstAlarmStep.SoundList
					},
					onNavigateToMissionList = {
						currentStep = CreateFirstAlarmStep.MissionList
					},
					settingAlarmCancelled = onCancelled,
					onShowPaywall = onShowPaywall,
					linearProgressBar = linearProgressBar
				)
			}

			CreateFirstAlarmStep.SoundList -> {
				DisposableEffect(Unit) {
					onDispose {
						alarmPickerViewModel.stopPreview()
					}
				}
				ListAlarmSoundScreen(
					alarmPickerViewModel,
					previewingUri = previewing?.soundUri,
					selectedUri = selected?.soundUri,
					onBack = {
						currentStep = CreateFirstAlarmStep.Picker
					},
					onProceed = { sound ->
						alarmPickerViewModel.onAlarmSoundSelected(sound)
					},
					linearProgressBar = linearProgressBar,
					onShowPaywall = onShowPaywall
				)
			}

			CreateFirstAlarmStep.MissionList -> {
				AlarmMissionListScreen(
					onBack = {
						currentStep = CreateFirstAlarmStep.Picker
					},
					onTrackEvent = { event, props -> alarmPickerViewModel.captureEvent(event, props) },
					linearProgressBar = linearProgressBar
				)
			}
		}
	}
}
