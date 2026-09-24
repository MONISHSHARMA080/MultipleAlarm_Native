package com.coolApps.MultipleAlarmClock.presentation.picker

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
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
import com.coolApps.MultipleAlarmClock.presentation.navigation.AlarmFlowRoute
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.presentation.picker.listAlarmRingtone.ListAlarmSoundScreen

@Composable
fun AlarmFlowScreen(
	alarmData: AlarmData?,
	onCloseFlow: () -> Unit,
	onNavigateToPaywall: (Boolean) -> Unit
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
				animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
				initialOffsetX = { it }
			) + fadeIn(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) togetherWith
					slideOutHorizontally(
						animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
						targetOffsetX = { -it }
					) + fadeOut(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
		},
		popTransitionSpec = {
			slideInHorizontally(
				animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
				initialOffsetX = { -it }
			) + fadeIn(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) togetherWith
					slideOutHorizontally(
						animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
						targetOffsetX = { it }
					) + fadeOut(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(
				animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
				initialOffsetX = { (-it * 0.3f).toInt() }
			) + fadeIn(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) togetherWith
					slideOutHorizontally(
						animationSpec = spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
						targetOffsetX = { it }
					) + fadeOut(spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
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
					}, onNavigateToPaywall = onNavigateToPaywall,
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
					onProceed = { sound ->
						if (sound == null) {
							viewModel.onAlarmSoundSelected(null)
							flowBackStack.removeLastOrNull()
						} else {
							if (viewModel.isPremium.value) {
								viewModel.onAlarmSoundSelected(sound)
								flowBackStack.removeLastOrNull()
							} else {
								onNavigateToPaywall(true)
							}
						}
					}
				)
			}
		}
	)
}
