package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.listAlarmRingtone.ListAlarmSoundScreen

@Composable
fun CreateFirstAlarmScreen(
	alarmPickerViewModel: AlarmPickerViewModel,
	onAlarmSetProceed: () -> Unit,
	onSettingAlarmCancelled: () -> Unit,
	modifier: Modifier = Modifier,
	linearProgressBar: @Composable () -> Unit = {},
) {
	var showAlarmSoundList by remember { mutableStateOf(false) }
	val selected by alarmPickerViewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val previewing by alarmPickerViewModel.previewingSound.collectAsStateWithLifecycle()

	AnimatedContent(
		targetState = showAlarmSoundList,
		modifier = modifier,
		transitionSpec = {
			if (targetState) {
				// Going forward
				slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(150, easing = FastOutSlowInEasing)
				) togetherWith slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(150, easing = FastOutSlowInEasing)
				)
			} else {
				// Going back
				slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(150, easing = FastOutSlowInEasing)
				) togetherWith slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(150, easing = FastOutSlowInEasing)
				)
			}
		},
		label = "alarm screen navigation"
	) { shouldWeShowAlarmScreen ->
		if (shouldWeShowAlarmScreen) {
			ListAlarmSoundScreen(
				alarmPickerViewModel,
				previewingUri = previewing?.soundUri,
				selectedUri = selected?.soundUri,
				onBack = {
					showAlarmSoundList = false
				},
				onSelected = { sound ->
					alarmPickerViewModel.onAlarmSoundSelected(sound)
				}
			)
		} else {
			AlarmPickerScreen(
				alarmSetProceed = onAlarmSetProceed,
				forNewAlarm = true,
				viewModel = alarmPickerViewModel,
				onNavigateToSoundList = {
					showAlarmSoundList = true
				},
				settingAlarmCancelled = onSettingAlarmCancelled,
				onNavigateToPaywall = {},
				linearProgressBar = linearProgressBar
			)
		}
	}
}
