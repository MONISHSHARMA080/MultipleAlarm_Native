package com.coolApps.MultipleAlarmClock.Ui.Navigation

import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.OnboardingScreen
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.AlarmContainer
import com.coolApps.MultipleAlarmClock.Ui.Navigation.NavigationViewModel
import com.coolApps.MultipleAlarmClock.Ui.Navigation.Screen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.AlarmFlowScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer.utils.SettingsScreen
import kotlinx.coroutines.launch


@Composable fun NavigationStack( navViewModel: NavigationViewModel ,  deepLinkScreen: Screen? ) {
	// -----------------------------------------------------------------------------------------------------------
	// 1.) if it's the first launch then I want to go to the app onboarding;
	// 2.) if the deppLink intent is there then I want to ignore isFirstLaunch and go straight to that screen
	// -----------------------------------------------------------------------------------------------------------
	val isFirstLaunch by navViewModel.isFirstLaunch.collectAsStateWithLifecycle()
	if (isFirstLaunch == null) return
//	val isFirstLaunch = false
	// debug
	val startKey = remember(deepLinkScreen, isFirstLaunch) {
		deepLinkScreen ?: if (isFirstLaunch == true) Screen.OnboardingScreen else Screen.AlarmContainer
	}
	val backStack = rememberNavBackStack(startKey)
	val coroutineScope = rememberCoroutineScope()

	LaunchedEffect(isFirstLaunch) {
		if (isFirstLaunch == false && deepLinkScreen == null) {
			backStack.clear()
			backStack.add(Screen.AlarmContainer)
		}
	}

	Scaffold(
		contentWindowInsets = WindowInsets.safeContent,
	) { _ ->

		NavDisplay(
			backStack = backStack,
			onBack = { backStack.removeLastOrNull() },

			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator()
			),

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

				entry<Screen.OnboardingScreen> {
							OnboardingScreen()
				}

				entry<Screen.SettingsScreen> {
					SettingsScreen(
						onNavigateBack = { backStack.removeLastOrNull() }
					)
				}

				entry<Screen.AlarmContainer> {
					AlarmContainer(
						onNavigateToEdit = { alarm ->
							backStack.add(Screen.AlarmFlow(alarm))
							coroutineScope.launch {
								navViewModel.screen(
									"AlarmPicker",
									mapOf(
										"is_to_edit_alarm" to true,
										"alarmData to edit" to alarm.toString()
									)
								)
							}
						},
						onNavigateToCreate = {
							backStack.add(Screen.AlarmFlow(null))
							coroutineScope.launch {
								navViewModel.screen(
									"AlarmPicker",
									mapOf("is_to_create_new_alarm" to true)
								)
							}
						},
						onNavigateToSettings = {
							backStack.add(Screen.SettingsScreen)
						}
					)

					LaunchedEffect(Unit) {
						navViewModel.screen("AlarmContainer")
					}
				}

				entry<Screen.AlarmFlow> { key ->

					AlarmFlowScreen(
						alarmData = key.alarmData,
						onCloseFlow = { backStack.removeLastOrNull() }
					)

					LaunchedEffect(key.alarmData) {
						navViewModel.screen(
							"AlarmFlow",
							mapOf(
								"alarmData" to (key.alarmData?.toString() ?: "null")
							)
						)
					}
				}
			}
		)
	}
}

