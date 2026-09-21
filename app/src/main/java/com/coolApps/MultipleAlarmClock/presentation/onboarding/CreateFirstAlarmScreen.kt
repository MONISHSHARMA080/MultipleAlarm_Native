@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.presentation.picker.listAlarmRingtone.ListAlarmSoundScreen

private enum class CreateFirstAlarmStep {
	Picker,
	SoundList
}


@Composable
fun CreateFirstAlarmScreen(
		onAlarmSetProceed: () -> Unit,
		modifier: Modifier = Modifier,
		linearProgressBar: @Composable () -> Unit = {},
		onButtonStateChange: (ButtonState) -> Unit = {}
) {
	onButtonStateChange(ButtonState.Hidden)
	val alarmPickerViewModel : AlarmPickerViewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory -> factory.create(null) }
	var currentStep by remember { mutableStateOf(CreateFirstAlarmStep.Picker) }
	val selected by alarmPickerViewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val previewing by alarmPickerViewModel.previewingSound.collectAsStateWithLifecycle()

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
				AlarmPickerScreen(
					alarmSetProceed = onAlarmSetProceed,
					forNewAlarm = true,
					viewModel = alarmPickerViewModel,
					onNavigateToSoundList = {
						currentStep = CreateFirstAlarmStep.SoundList
					},
					settingAlarmCancelled = {
						// Optionally handle cancellation, maybe go back to previous Onboarding screen
					},
					onNavigateToPaywall = {},
					linearProgressBar = linearProgressBar
				)
			}

			CreateFirstAlarmStep.SoundList -> {
				ListAlarmSoundScreen(
					alarmPickerViewModel,
					previewingUri = previewing?.soundUri,
					selectedUri = selected?.soundUri,
					onBack = {
						currentStep = CreateFirstAlarmStep.Picker
					},
					onProceed = { sound ->
						alarmPickerViewModel.onAlarmSoundSelected(sound)
						currentStep = CreateFirstAlarmStep.Picker
					},
					linearProgressBar = linearProgressBar
				)
			}
		}
	}
}
