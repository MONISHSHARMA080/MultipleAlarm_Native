package com.coolApps.MultipleAlarmClock.presentation.picker

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.IntOffset
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

/**
 * Host for the edit-alarm sub-navigation (AlarmPicker → SoundList).
 * alarmData is non-null — this screen is only reached via Screen.AlarmEditRoute.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun EditAlarmFlowScreen(
	alarmData: AlarmData,
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

	val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
	val fastSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
	val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
	val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

	// Inner NavDisplay — the sound-list sub-screen nav; uses expressive motion scheme.
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
			if (initialState == targetState) {
				EnterTransition.None togetherWith ExitTransition.None
			} else {
				slideInHorizontally(
					animationSpec = spatialSpec,
					initialOffsetX = { it }
				) + fadeIn(effectsSpec) togetherWith
						slideOutHorizontally(
							animationSpec = spatialSpec,
							targetOffsetX = { -it }
						) + fadeOut(effectsSpec)
			}
		},
		popTransitionSpec = {
			slideInHorizontally(
				animationSpec = fastSpatialSpec,
				initialOffsetX = { -it }
			) + fadeIn(fastEffectsSpec) togetherWith
					slideOutHorizontally(
						animationSpec = fastSpatialSpec,
						targetOffsetX = { it }
					) + fadeOut(fastEffectsSpec)
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(
				animationSpec = fastSpatialSpec,
				initialOffsetX = { (-it * 0.3f).toInt() }
			) + fadeIn(fastEffectsSpec) togetherWith
					slideOutHorizontally(
						animationSpec = fastSpatialSpec,
						targetOffsetX = { it }
					) + fadeOut(fastEffectsSpec)
		},
		entryProvider = entryProvider {
			entry<AlarmFlowRoute.AlarmPicker> {
				EditAlarmScreen(
					viewModel = viewModel,
					alarmData = alarmData,
					alarmSetProceed = { onCloseFlow() },
					settingAlarmCancelled = onCloseFlow,
					onNavigateToSoundList = { flowBackStack.add(AlarmFlowRoute.AlarmSoundListScreen) },
					onNavigateToPaywall = onNavigateToPaywall,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope
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

/**
 * Host for the new-alarm sub-navigation (AlarmPicker → SoundList).
 * No alarmData — reached via Screen.AlarmCreateRoute.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NewAlarmFlowScreen(
	onCloseFlow: () -> Unit,
	onNavigateToPaywall: (Boolean) -> Unit
) {
	val viewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory ->
		factory.create(null)
	}
	val flowBackStack = rememberNavBackStack(AlarmFlowRoute.AlarmPicker)
	val currentScreen = flowBackStack.lastOrNull() as? Screen
	LaunchedEffect(currentScreen) {
		currentScreen?.let { viewModel.screen(it.screenName) }
	}

	val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
	val fastSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
	val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
	val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

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
			if (initialState == targetState) {
				EnterTransition.None togetherWith ExitTransition.None
			} else {
				slideInHorizontally(
					animationSpec = spatialSpec,
					initialOffsetX = { it }
				) + fadeIn(effectsSpec) togetherWith
						slideOutHorizontally(
							animationSpec = spatialSpec,
							targetOffsetX = { -it }
						) + fadeOut(effectsSpec)
			}
		},
		popTransitionSpec = {
			slideInHorizontally(
				animationSpec = fastSpatialSpec,
				initialOffsetX = { -it }
			) + fadeIn(fastEffectsSpec) togetherWith
					slideOutHorizontally(
						animationSpec = fastSpatialSpec,
						targetOffsetX = { it }
					) + fadeOut(fastEffectsSpec)
		},
		predictivePopTransitionSpec = {
			slideInHorizontally(
				animationSpec = fastSpatialSpec,
				initialOffsetX = { (-it * 0.3f).toInt() }
			) + fadeIn(fastEffectsSpec) togetherWith
					slideOutHorizontally(
						animationSpec = fastSpatialSpec,
						targetOffsetX = { it }
					) + fadeOut(fastEffectsSpec)
		},
		entryProvider = entryProvider {
			entry<AlarmFlowRoute.AlarmPicker> {
				val resultBus = LocalResultEventBus.current
				NewAlarmScreen(
					viewModel = viewModel,
					alarmSetProceed = {
						resultBus.sendResult(true)
						onCloseFlow()
					},
					settingAlarmCancelled = onCloseFlow,
					onNavigateToSoundList = { flowBackStack.add(AlarmFlowRoute.AlarmSoundListScreen) },
					onNavigateToPaywall = onNavigateToPaywall
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
