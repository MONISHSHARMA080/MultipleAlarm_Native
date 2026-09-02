package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.Ui.Navigation.AlarmFlowRoute
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.listAlarmRingtone.ListAlarmSoundScreen

@Composable
fun AlarmFlowScreen(
	alarmData: AlarmData?,
	onCloseFlow: () -> Unit
) {
	val viewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory ->
		factory.create(alarmData)
	}
	val flowBackStack = rememberNavBackStack(AlarmFlowRoute.AlarmPicker)

	NavDisplay(
		backStack = flowBackStack,
		onBack = {
			if (flowBackStack.lastOrNull() == AlarmFlowRoute.AlarmPicker) {
				onCloseFlow()
			} else {
				flowBackStack.removeLastOrNull()
			}
		},
		transitionSpec = {
			slideInHorizontally(
				animationSpec = tween(200, easing = FastOutSlowInEasing),
				initialOffsetX = { it }
			) + fadeIn(tween(150, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(200, easing = FastOutSlowInEasing),
						targetOffsetX = { -it }
					) + fadeOut(tween(100, easing = LinearEasing))
		},
		popTransitionSpec = {
			slideInHorizontally(
				animationSpec = tween(200, easing = FastOutSlowInEasing),
				initialOffsetX = { -it }
			) + fadeIn(tween(150, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(200, easing = FastOutSlowInEasing),
						targetOffsetX = { it }
					) + fadeOut(tween(100, easing = LinearEasing))
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(
				animationSpec = tween(180, easing = FastOutSlowInEasing),
				initialOffsetX = { (-it * 0.3f).toInt() }
			) + fadeIn(tween(130, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(180, easing = FastOutSlowInEasing),
						targetOffsetX = { it }
					) + fadeOut(tween(90, easing = LinearEasing))
		},
		entryProvider = entryProvider {
			entry<AlarmFlowRoute.AlarmPicker> {
				AlarmPickerScreen(
					viewModel = viewModel,
					alarmSetProceed = onCloseFlow,
					settingAlarmCancelled = onCloseFlow,
					forNewAlarm = alarmData == null,
					onNavigateToSoundList = {
						flowBackStack.add(AlarmFlowRoute.AlarmSoundListScreen)
					}
				)
			}

			entry<AlarmFlowRoute.AlarmSoundListScreen> {
				val selected by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()
				val previewing by viewModel.previewingSound.collectAsStateWithLifecycle()


				DisposableEffect(Unit) {
					onDispose {
						viewModel.stopPreview()
					}
				}

				ListAlarmSoundScreen(
					viewModel,
					previewingUri = previewing?.soundUri,
					selectedUri = selected?.soundUri,
					onBack = {
						flowBackStack.removeLastOrNull()
					},
					onSelected = { sound ->
						viewModel.onAlarmSoundSelected(sound)
					}
				)
			}
		}
	)
}
