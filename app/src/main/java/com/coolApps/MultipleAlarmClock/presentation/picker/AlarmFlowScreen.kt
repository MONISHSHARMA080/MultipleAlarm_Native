package com.coolApps.MultipleAlarmClock.presentation.picker

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.presentation.navigation.AlarmFlowRoute
import com.coolApps.MultipleAlarmClock.presentation.navigation.Screen
import com.coolApps.MultipleAlarmClock.presentation.picker.listAlarmRingtone.ListAlarmSoundScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlarmFlowScreen(
	alarmData: AlarmData?,
	onCloseFlow: () -> Unit,
	onNavigateToPaywall: (Boolean) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val viewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory ->
		factory.create(alarmData)
	}
	val flowBackStack = rememberNavBackStack(AlarmFlowRoute.AlarmPicker)
	val currentScreen = flowBackStack.lastOrNull() as? Screen
	LaunchedEffect(currentScreen) {
		currentScreen?.let { viewModel.screen(it.screenName) }
	}


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
				animationSpec = tween(330, easing = FastOutSlowInEasing),
				initialOffsetX = { it }
			) + fadeIn(tween(210, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(330, easing = FastOutSlowInEasing),
						targetOffsetX = { -it }
					) + fadeOut(tween(210, easing = LinearEasing))
		},
		popTransitionSpec = {
			slideInHorizontally(
				animationSpec = tween(240, easing = FastOutSlowInEasing),
				initialOffsetX = { -it }
			) + fadeIn(tween(180, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(240, easing = FastOutSlowInEasing),
						targetOffsetX = { it }
					) + fadeOut(tween(140, easing = LinearEasing))
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(
				animationSpec = tween(240, easing = FastOutSlowInEasing),
				initialOffsetX = { (-it * 0.3f).toInt() }
			) + fadeIn(tween(150, easing = LinearEasing)) togetherWith
					slideOutHorizontally(
						animationSpec = tween(190, easing = FastOutSlowInEasing),
						targetOffsetX = { it }
					) + fadeOut(tween(120, easing = LinearEasing))
		},
		entryProvider = entryProvider {
			entry<AlarmFlowRoute.AlarmPicker> {
				val resultBus = LocalResultEventBus.current
				AlarmPickerScreen(
					viewModel = viewModel,
					alarmSetProceed = {
						if (alarmData == null) {
							resultBus.sendResult(true)
						}
						onCloseFlow()
					},
					settingAlarmCancelled = onCloseFlow,
					forNewAlarm = alarmData == null,
					onNavigateToSoundList = {
						flowBackStack.add(AlarmFlowRoute.AlarmSoundListScreen)
					}, onNavigateToPaywall = onNavigateToPaywall,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope,
					alarmId = alarmData?.id
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
						viewModel.onAlarmSoundSelected(sound)
					},
					onNavigateToPaywall = onNavigateToPaywall
				)
			}
		}
	)
}
