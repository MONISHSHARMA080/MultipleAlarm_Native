package com.coolApps.MultipleAlarmClock.Components_for_ui_compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmListScreen.AlarmContainer
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.logD
import com.example.MultipleAlarmClock.Ui.Navigation.NavigationViewModel
import com.example.MultipleAlarmClock.Ui.utils.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
	@Serializable
	data object OnboardingScreen: Screen

	@Serializable
	data object AlarmContainer : Screen
	@Serializable
	data class AlarmPicker(val alarmData: AlarmData? = null) : Screen
	@Serializable
	data object SettingsScreen: Screen

}

@Composable fun NavigationStack( navViewModel: NavigationViewModel ,  deepLinkScreen: Screen? ) {
	// -----------------------------------------------------------------------------------------------------------
	// 1.) if it's the first launch then I want to go to the app onboarding;
	// 2.) if the deppLink intent is there then I want to ignore isFirstLaunch and go straight to that screen
	// -----------------------------------------------------------------------------------------------------------
//	val isFirstLaunch by navViewModel.isFirstLaunch.collectAsStateWithLifecycle()
//	if (isFirstLaunch == null) return
	logD("meow")
	val isFirstLaunch = false
	val startKey = remember(deepLinkScreen, isFirstLaunch) {
		deepLinkScreen ?: if (isFirstLaunch == true) Screen.OnboardingScreen else Screen.AlarmContainer
	}
	logD("the startKey screen is $startKey")
	val backStack = rememberNavBackStack(startKey)
	val coroutineScope = rememberCoroutineScope()

	Scaffold(
		contentWindowInsets = WindowInsets.safeContent,
	) { _->
		NavDisplay(
			backStack=backStack,
			onBack = { backStack.removeLastOrNull()},
			transitionSpec = {
				slideInHorizontally(
					animationSpec = tween(200, easing = FastOutSlowInEasing), initialOffsetX = { it }
				) + fadeIn(tween(150, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing), targetOffsetX = { -it }) +
						fadeOut(tween(100, easing = LinearEasing))
			},
			popTransitionSpec = {
				slideInHorizontally(
					animationSpec = tween(200, easing = FastOutSlowInEasing), initialOffsetX = { -it }) +
						fadeIn(tween(150, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing), targetOffsetX = { it }) +
						fadeOut(tween(100, easing = LinearEasing))
			},
			predictivePopTransitionSpec = {
				slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), initialOffsetX = { (-it * 0.3f).toInt() }) +
						fadeIn(tween(130, easing = LinearEasing)) togetherWith
						slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), targetOffsetX = { it }) +
						fadeOut(tween(90, easing = LinearEasing))
			},

			entryProvider = { key ->
				when(key) {
					// --------------------------------------------------------
					//		move the stop alarm and the reset alarm
					//		etc. func to the respective viewModel, that we we clean
					// --------------------------------------------------------

					is Screen.SettingsScreen ->NavEntry(key)	{
						SettingsScreen(onNavigateBack = {backStack.removeLastOrNull()}, {})
					}

					is Screen.OnboardingScreen -> NavEntry(key)	{
						Scaffold(contentWindowInsets = WindowInsets.safeContent) { edgeToEdgePadding ->
							Column(Modifier.padding(edgeToEdgePadding )) {
								Text("Hi from onboarding screen")
							}
						}
					}

					is Screen.AlarmContainer -> NavEntry(key) {
							AlarmContainer(
								onNavigateToEdit = { alarm ->
									backStack.add(Screen.AlarmPicker(alarm))
									coroutineScope.launch {
										navViewModel.screen("AlarmPicker", mapOf("is_to_edit_alarm" to true, "alarmData to edit" to alarm.toString()) )
									}
							   },
								onNavigateToCreate = {
									backStack.add(Screen.AlarmPicker(null))
									coroutineScope.launch {
										navViewModel.screen("AlarmPicker", mapOf("is_to_create_new_alarm" to true) )
									}
								 }, onNavigateToSettings = {
									 backStack.add(Screen.SettingsScreen)
								}
							)
							LaunchedEffect(Unit) {
								navViewModel.screen("AlarmContainer")
							}
						}
					is Screen.AlarmPicker ->						NavEntry(key) {
						/**[ onAlarmSet] - here [ AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
						AlarmPickerScreen(key.alarmData,   alarmSetGoBack = { backStack.removeLastOrNull() })
						LaunchedEffect(Unit) {
							navViewModel.screen("AlarmPicker")
						}
					}
					else ->
						NavEntry(key) {
							AlarmContainer(
								onNavigateToEdit = { alarm ->
									backStack.add(Screen.AlarmPicker(alarm))
									coroutineScope.launch {
										navViewModel.screen("AlarmPicker", mapOf("is_to_edit_alarm" to true, "alarmData to edit" to alarm.toString()) )
									}
								},
								onNavigateToCreate = {
									backStack.add(Screen.AlarmPicker(null))
									coroutineScope.launch {
										navViewModel.screen("AlarmPicker", mapOf("is_to_create_new_alarm" to true) )
									}
								}, onNavigateToSettings = {
									backStack.add(Screen.SettingsScreen)
								}
							)
							LaunchedEffect(Unit) {
								navViewModel.screen("AlarmContainer", mapOf("from_else_branch_in_navigation" to true))
							}
						}
				}
			}
		)
	}
}

