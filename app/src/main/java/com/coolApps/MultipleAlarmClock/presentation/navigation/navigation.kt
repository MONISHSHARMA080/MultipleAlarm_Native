package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.data.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.presentation.home.AlarmContainer
import com.coolApps.MultipleAlarmClock.presentation.onboarding.OnboardingScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.OnboardingScreen2
import com.coolApps.MultipleAlarmClock.presentation.picker.EditAlarmFlowScreen
import com.coolApps.MultipleAlarmClock.presentation.picker.NewAlarmFlowScreen
import com.coolApps.MultipleAlarmClock.presentation.settings.PremiumPaywallDialog
import com.coolApps.MultipleAlarmClock.presentation.settings.SettingsScreen
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable fun NavigationStack(navViewModel: NavigationViewModel, deepLinkScreen: Screen?) {
	val isFirstLaunch by navViewModel.isFirstLaunch.collectAsStateWithLifecycle()
	val onboardingFlow by navViewModel.onboardingFlow.collectAsStateWithLifecycle()
	if (isFirstLaunch == null) return
	if (isFirstLaunch == true && onboardingFlow == OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED) return
	val startKey = remember(deepLinkScreen, isFirstLaunch, onboardingFlow) {
		when {
			deepLinkScreen != null -> deepLinkScreen
			isFirstLaunch == true -> when (onboardingFlow) {
				OnboardingFlow.ONBOARDING_FLOW_VARIANT_SHORT -> Screen.OnboardingScreen2
				else -> Screen.OnboardingScreen
			}
			else -> Screen.AlarmContainer
		}
	}
	val backStack = rememberNavBackStack(startKey)
	var paywallTriggerType by remember { mutableStateOf<PaywallTriggerType?>(null) }
	val offeringsByPlacement by navViewModel.offeringsByPlacement.collectAsStateWithLifecycle()

	// Offering state (fetch/resolution) for the trigger currently shown.
	val billingState = paywallTriggerType
		?.let { trigger -> offeringsByPlacement[trigger.placementId] }
		?: OfferingState.Loading

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
	LaunchedEffect(paywallTriggerType) {
		val trigger = paywallTriggerType ?: return@LaunchedEffect
		navViewModel.screen("Paywall", mapOf("placement" to trigger.placementId))
		navViewModel.requestPaywallOffering(trigger)
	}
	// When RevenueCat can't serve an offering for the trigger's Placement
	// close the overlay instead of showing a stuck loader.
	LaunchedEffect(billingState) {
		when (billingState) {
			is OfferingState.Loaded -> if (billingState.offering == null) paywallTriggerType = null
			OfferingState.Failed -> paywallTriggerType = null
			OfferingState.Loading -> {}
		}
	}
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.background
	) {

		SharedTransitionLayout {
			val sharedTransitionScope = this

			NavDisplay(
				backStack = backStack,
				onBack = {
					when {
						backStack.size > 1 -> backStack.removeLastOrNull()
						else -> {
							backStack.clear()
							backStack.add(Screen.AlarmContainer)
						}
					}
				},

				entryDecorators = listOf(
					rememberSaveableStateHolderNavEntryDecorator(),
					rememberViewModelStoreNavEntryDecorator(),
					rememberResultEventBusNavEntryDecorator()
				),

				// Default transitions used for all routes that don't override via metadata
				transitionSpec = { NavTransitions.forward() },
				popTransitionSpec = { NavTransitions.pop() },
				predictivePopTransitionSpec = { NavTransitions.pop() },

				entryProvider = entryProvider {

					entry<Screen.OnboardingScreen> {
						OnboardingScreen(onShowPaywall = { paywallTriggerType = it })
					}

					entry<Screen.OnboardingScreen2> {
						OnboardingScreen2()
					}

					entry<Screen.SettingsScreen> {
						SettingsScreen(
							onNavigateBack = {
								backStack.removeLastOrNull() ?: backStack.add(Screen.AlarmContainer)
							},
							onShowPaywall = {
								paywallTriggerType = it
							},

							onNavigateToCustomerCenter = {
								backStack.add(Screen.CustomerCenter)
							}
						)
					}

					entry<Screen.AlarmContainer>(
						metadata = NavDisplay.popTransitionSpec {
							if (initialState.key is Screen.AlarmEditRoute) {
								EnterTransition.None togetherWith ExitTransition.None
							} else null
						} + NavDisplay.predictivePopTransitionSpec {
							if (initialState.key is Screen.AlarmEditRoute) {
								EnterTransition.None togetherWith ExitTransition.None
							} else null
						}
					) {
						AlarmContainer(
							onNavigateToEdit = { alarm ->
								backStack.add(Screen.AlarmEditRoute(alarm))
							},
							onNavigateToCreate = {
								backStack.add(Screen.AlarmCreateRoute)
							},
							onNavigateToSettings = {
								backStack.add(Screen.SettingsScreen)
							},
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}

					// Edit route — suppress nav transition so the shared-element hero plays unobstructed
					entry<Screen.AlarmEditRoute>(
						metadata = NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None }
								+ NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
								+ NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }
					) { key ->
						EditAlarmFlowScreen(
							alarmData = key.alarmData,
							onCloseFlow = { backStack.removeLastOrNull() },
							onShowPaywall = { paywallTriggerType = it },
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}

					// Create route — default forward-slide transition (no metadata override needed)
					entry<Screen.AlarmCreateRoute> {
						NewAlarmFlowScreen(
							onCloseFlow = { backStack.removeLastOrNull() },
							onShowPaywall = { paywallTriggerType = it }
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
	}
	AnimatedVisibility(
		paywallTriggerType != null,
		enter = slideInVertically(
			animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
			initialOffsetY = { it }
		) + fadeIn(
			animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
		),
		exit = slideOutVertically(
			animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
			targetOffsetY = { it }
		) + fadeOut(
			animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
		)
	) {
		when (billingState) {
			is OfferingState.Loaded -> {
				val offering = billingState.offering
				if (offering != null) {
					PremiumPaywallDialog(
						isPlus = false,
						offering = offering,
						onEvent = navViewModel::onPaywallEvent
					) {
						paywallTriggerType = null
						navViewModel.captureEvent("paywall_dismissed", emptyMap())
					}
				} else {
					// Loaded(null) only survives visually for a frame —
					// the auto-dismiss effect closes the overlay meanwhile.
					PaywallLoaderScreen()
				}
			}
			OfferingState.Loading -> PaywallLoaderScreen()
			OfferingState.Failed -> Unit // overlay closes via the auto-dismiss effect
		}
	}
}

/** Full-screen loader shown while RevenueCat resolves the offering for the trigger. */
@Composable
private fun PaywallLoaderScreen() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		CircularWavyProgressIndicator()
	}
}

