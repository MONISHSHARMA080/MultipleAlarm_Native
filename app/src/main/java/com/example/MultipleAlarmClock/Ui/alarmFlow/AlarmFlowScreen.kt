package com.example.MultipleAlarmClock.Ui.alarmFlow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.example.MultipleAlarmClock.Ui.Navigation.AlarmFlowRoute
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import com.example.MultipleAlarmClock.Ui.alarmPicker.listAlarmRingtone.ListAlarmScreen

@Composable
fun AlarmFlowScreen(
	alarmData: AlarmData?,
	onCloseFlow: () -> Unit
) {
	val viewModel: AlarmPickerViewModel = hiltViewModel()

	val flowBackStack = rememberNavBackStack(AlarmFlowRoute.AlarmPicker)

	LaunchedEffect(alarmData) {
		viewModel.initialize(alarmData)
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
					alarm = alarmData,
					viewModel = viewModel,
					alarmSetGoBack = onCloseFlow,
					onNavigateToSoundList = {
						flowBackStack.add(AlarmFlowRoute.AlarmSoundListScreen)
					}
				)
			}

			entry<AlarmFlowRoute.AlarmSoundListScreen> {
				val sounds by viewModel.alarms.collectAsStateWithLifecycle()
				val selected by viewModel.selectedAlarmSound.collectAsStateWithLifecycle()

				ListAlarmScreen(
					sounds = sounds,
					selectedUri = selected?.soundUri,
					onSelected = { sound ->
						viewModel.onAlarmSoundSelected(sound)
						flowBackStack.removeLastOrNull()
					}
				)
			}
		}
	)
}
