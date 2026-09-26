package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen(onNavigateToPaywall: (Boolean) -> Unit = {}) {
	val viewModel : OnboardingViewModel = hiltViewModel()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val featureFlags by viewModel.analytics.featureFlagsData.collectAsStateWithLifecycle()
	val isHardPaywall = featureFlags?.isHardPaywallEnabled ?: false

	LaunchedEffect(uiState.displaySate) {
		viewModel.analytics.screen("Onboarding_${uiState.displaySate.name.lowercase()}")
	}
	var offering by remember { mutableStateOf<Offering?>(null) }
	var loadFailed by remember { mutableStateOf(false) }
	// Pre-compute the initial button state from the display state so it's set
	// *before* AnimatedContent composes the new screen – prevents layout jitter
	// from child screens changing it during their first composition frame.
	var buttonState by remember { mutableStateOf(ButtonState.Enabled) }
	LaunchedEffect(uiState.displaySate) {
		buttonState = when (uiState.displaySate) {
			DisplaySate.CreateFirstAlarm,
			DisplaySate.AlarmResult,
			DisplaySate.OnboardingPaywall -> ButtonState.Hidden
			else -> buttonState  // keep current; child screens will refine via callback
		}
	}
	val view = LocalView.current

	LaunchedEffect(Unit) {
		try {
			val offerings = Purchases.sharedInstance.awaitOfferings()
			offering = offerings.getCurrentOfferingForPlacement("onboarding_end")
				?: offerings.current
		} catch (e: PurchasesException) {
			loadFailed = true
		}
	}

	val progress = when (uiState.displaySate) {
		DisplaySate.Greeting -> 1f / 8f
		DisplaySate.Problem -> 2f / 8f
		DisplaySate.UserStruggles -> 3f / 8f
		DisplaySate.Permission -> 4f / 8f
		DisplaySate.FirstAlarmIntro -> 5f / 8f
		DisplaySate.CreateFirstAlarm -> {
			6f / 8f
		}
		DisplaySate.AlarmResult -> 7f / 8f
		DisplaySate.OnboardingPaywall -> 8f / 8f
	}

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = tween(durationMillis = 400),
		label = "progress"
	)

	Scaffold(
		topBar = {
			if (uiState.displaySate != DisplaySate.OnboardingPaywall) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.statusBarsPadding()
						.padding(horizontal = 8.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					IconButton(onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						viewModel.onPreviousClicked()
						buttonState = ButtonState.Enabled
					}) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = "Back"
						)
					}
					Spacer(modifier = Modifier.width(8.dp))
					LinearProgressIndicator(
						progress = { animatedProgress },
						modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
					)
				}
			}
		}
	) { innerPadding ->
		// Box overlay: content always gets the same innerPadding (no bottomBar),
		// button floats on top without affecting layout measurements.
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) {
			AnimatedContent(
				targetState = uiState.displaySate,
				modifier = Modifier.fillMaxSize(),
				transitionSpec = {
					// Use natural enum ordering to determine navigation direction
					val isForward = targetState > initialState
					slideIntoContainer(
						towards = if (isForward) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right,
						animationSpec = tween(370, easing = FastOutSlowInEasing)
					) togetherWith slideOutOfContainer(
						towards = if (isForward) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right,
						animationSpec = tween(370, easing = FastOutSlowInEasing)
					)
				},
			) { state ->
				when (state) {
					DisplaySate.Greeting -> GreetingScreen()
					// here make this into one uniform animation and no click etc. and then loop
					DisplaySate.Problem -> ProblemScreen(
						onButtonStateChange = { buttonState = it }
					)
					DisplaySate.UserStruggles -> UserStrugglesScreen(
						onButtonStateChange = { buttonState = it },
						onStrugglesSelected = { viewModel.onStrugglesSelected(it) }
					)
					DisplaySate.Permission -> {
						PermissionScreen(
							missingSteps = uiState.missingSteps,
							allCriticalGranted = uiState.allCriticalGranted,
							refreshPermissionUiState = { viewModel.refreshPermissions() },
							onButtonStateChange = { buttonState = it }
						)
					}
					DisplaySate.FirstAlarmIntro -> {
						FirstAlarmIntroView(
							onButtonStateChange = { buttonState = it }
						)
					}
					DisplaySate.CreateFirstAlarm -> {
						CreateFirstAlarmScreen(
							onAlarmSetProceed = { viewModel.onNextClicked() },
							linearProgressBar = {
								LinearProgressIndicator(
									progress = { animatedProgress },
									modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
								)
							},
							onButtonStateChange = { buttonState = it },
							onShowPaywall = onNavigateToPaywall
						)
					}
					DisplaySate.AlarmResult -> AlarmResultClaude(
						alarmData = uiState.alarmData,
						onButtonStateChange = { buttonState = it }
					)
					DisplaySate.OnboardingPaywall -> {
						LaunchedEffect(isHardPaywall) {
							if (isHardPaywall) {
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							}
						}
						OnboardingPaywallScreen(
							isHardPaywall = isHardPaywall,
							onBackPress = {
								viewModel.onPreviousClicked()
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							},
							onFinished = { viewModel.finishedOnboarding() }, loadFailed = loadFailed, offering = offering,
							onPurchaseCompletedEvent = {customerInfo, storeTransaction -> 
								if (isHardPaywall) {
									uiState.alarmData?.let { viewModel.resetAlarm(it) }
								}
								viewModel.onPurchaseCompletedEvent(customerInfo,storeTransaction) 
							},
							onRestoreCompletedEvent = { 
								if (isHardPaywall) {
									uiState.alarmData?.let { viewModel.resetAlarm(it) }
								}
								viewModel.onRestoreCompletedEvent(it) 
							},
							onButtonStateChange = { buttonState = it }
						)
					}
				}
			}

			// Continue button — floating overlay, doesn't affect layout sizing.
			// Instant show/hide: no animation, so zero layout shift during
			// screen transitions.
			if (buttonState != ButtonState.Hidden) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.background(colorScheme.background)
						.navigationBarsPadding()
						.padding(26.dp)
						.padding(bottom = 20.dp),
					contentAlignment = Alignment.Center,
				) {
					Button(
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							viewModel.onNextClicked()
						},
						enabled = buttonState == ButtonState.Enabled,
						modifier = Modifier
							.fillMaxWidth()
							.height(56.dp),
						shape = shapes.extraLarge,
						colors = ButtonDefaults.buttonColors(
							containerColor = colorScheme.primaryContainer,
							contentColor = colorScheme.onPrimaryContainer
						)
					) {
						Text(
							text = stringResource(R.string.permission_continue),
							style = typography.titleMedium
						)
					}
				}
			}
		}
	}
}