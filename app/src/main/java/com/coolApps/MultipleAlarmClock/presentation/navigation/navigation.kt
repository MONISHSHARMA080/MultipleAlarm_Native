package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.presentation.home.AlarmContainer
import com.coolApps.MultipleAlarmClock.presentation.onboarding.OnboardingScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.AlarmFlowScreen
import com.coolApps.MultipleAlarmClock.presentation.settings.PremiumPaywallDialog
import com.coolApps.MultipleAlarmClock.presentation.settings.SettingsScreen
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import kotlinx.coroutines.launch


@Composable fun NavigationStack(navViewModel: NavigationViewModel, deepLinkScreen: Screen?) {
	val isFirstLaunch by navViewModel.isFirstLaunch.collectAsStateWithLifecycle()
	if (isFirstLaunch == null) return
	val startKey = remember(deepLinkScreen, isFirstLaunch) {
		deepLinkScreen ?: if (isFirstLaunch == true) Screen.OnboardingScreen else Screen.AlarmContainer
	}
	val backStack = rememberNavBackStack(startKey)
	val coroutineScope = rememberCoroutineScope()
	var showPaywall by remember { mutableStateOf(false) }

	LaunchedEffect(isFirstLaunch) {
		if (isFirstLaunch == false && deepLinkScreen == null) {
			backStack.clear()
			backStack.add(Screen.AlarmContainer)
		}
	}
	val currentScreen = backStack.lastOrNull() as? Screen
	LaunchedEffect(currentScreen) {
		currentScreen?.let { navViewModel.screen(it.screenName) }
	}
	LaunchedEffect(showPaywall) {
		if (showPaywall) {
			navViewModel.screen("Paywall")
		}
	}
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.background
	) {

		NavDisplay(
			backStack = backStack,
			onBack = { backStack.removeLastOrNull() },

			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
				rememberResultEventBusNavEntryDecorator()
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
					OnboardingScreen(onNavigateToPaywall = { showPaywall = it })
				}

				entry<Screen.SettingsScreen> {
					SettingsScreen(
						onNavigateBack = {
							backStack.removeLastOrNull() ?: backStack.add(Screen.AlarmContainer)
						},
						onNavigateToPaywall = {
							showPaywall = it
						},

						onNavigateToCustomerCenter = {
							backStack.add(Screen.CustomerCenter)
						}
					)
				}

				entry<Screen.AlarmContainer> {
					AlarmContainer(
						onNavigateToEdit = { alarm ->
							backStack.add(Screen.AlarmFlow(alarm))
						},
						onNavigateToCreate = {
							backStack.add(Screen.AlarmFlow(null))
						},
						onNavigateToSettings = {
							backStack.add(Screen.SettingsScreen)
						}
					)
				}

				entry<Screen.AlarmFlow> { key ->

					AlarmFlowScreen(
						alarmData = key.alarmData,
						onCloseFlow = { backStack.removeLastOrNull() },
						onNavigateToPaywall = {
							showPaywall = it
						}
					)
				}

				entry<Screen.Paywall> {
					Paywall(
						options = PaywallOptions.Builder(
							dismissRequest = { backStack.removeLastOrNull() }
						).build()
					)
				}

				entry<Screen.CustomerCenter> {
					CustomerCenter(
						onDismiss = { backStack.removeLastOrNull() }
					)
				}
			}
		)
	}
	AnimatedVisibility(
		showPaywall,
		enter = slideInVertically(
			animationSpec = tween(420, easing = FastOutSlowInEasing),
			initialOffsetY = { it }
		) + fadeIn(
			animationSpec = tween(300, delayMillis = 10, easing = LinearEasing)
		),
		exit = slideOutVertically(
			animationSpec = tween(420, easing = FastOutSlowInEasing),
			targetOffsetY = { it }
		) + fadeOut(
			animationSpec = tween(250, easing = LinearEasing)
		)
	) {
		PremiumPaywallDialog(false,
			onPurchaseCompletedEvent = {customerInfo, storeTransaction -> navViewModel.onPurchaseCompletedEvent(customerInfo,storeTransaction) },
			onRestoreCompletedEvent = { navViewModel.onRestoreCompletedEvent(it) }
			) {
			showPaywall = false
			coroutineScope.launch {
				navViewModel.captureEvent("paywall_dismissed", mapOf())
			}
		}
	}
}
