package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.coolApps.MultipleAlarmClock.presentation.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.presentation.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.presentation.paywall.PaywallTriggerType
import com.google.accompanist.permissions.ExperimentalPermissionsApi


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen(onShowPaywall: (PaywallTriggerType) -> Unit = {}) {
	val viewModel : OnboardingViewModel = hiltViewModel()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val featureFlags by viewModel.analytics.featureFlagsData.collectAsStateWithLifecycle()
	val isHardPaywall = featureFlags?.isHardPaywallEnabled ?: false

	val onboardingOffering by viewModel.onboardingOffering.collectAsStateWithLifecycle()

	LaunchedEffect(uiState.displaySate) {
		viewModel.analytics.screen("Onboarding_${uiState.displaySate.name.lowercase()}")
	}
	// Pre-compute the initial button state from the display state so it's set
	// *before* AnimatedContent composes the new screen – prevents layout jitter
	// from child screens changing it during their first composition frame.
	var buttonState by remember { mutableStateOf(ButtonState.Enabled) }
	LaunchedEffect(uiState.displaySate) {
		buttonState = when (uiState.displaySate) {
			DisplaySate.CreateFirstAlarm,
			DisplaySate.AlarmResult,
			DisplaySate.PrePaywall,
			DisplaySate.OnboardingPaywall -> ButtonState.Hidden
			else -> buttonState  // keep current; child screens will refine via callback
		}
	}
	val view = LocalView.current

	val progress = when (uiState.displaySate) {
		DisplaySate.Problem -> 1f / 7f
		DisplaySate.UserStruggles -> 2f / 7f
		DisplaySate.Permission -> 3f / 7f
		DisplaySate.FirstAlarmIntro -> 4f / 7f
		DisplaySate.CreateFirstAlarm -> {
			5f / 7f
		}
		DisplaySate.AlarmResult -> 6f / 7f
		DisplaySate.PrePaywall -> 6f / 7f
		DisplaySate.OnboardingPaywall -> 7f / 7f
	}

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = tween(durationMillis = 400),
		label = "progress"
	)

	Scaffold(
		topBar = {
			if (uiState.displaySate != DisplaySate.OnboardingPaywall &&
				uiState.displaySate != DisplaySate.PrePaywall
			) {
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
							onShowPaywall = onShowPaywall
						)
					}
					DisplaySate.AlarmResult -> AlarmResultScreen(
						alarmData = uiState.alarmData,
						onButtonStateChange = { buttonState = it }
					)
					DisplaySate.PrePaywall -> {
						when (val billingOffering = onboardingOffering) {
							OfferingState.Loading -> OfferingsPlaceholderScreen(
								onButtonStateChange = { buttonState = it }
							)
							is OfferingState.Loaded -> {
								val currentOffering = billingOffering.offering
								if (currentOffering != null) {
									HardPaywallEnabledScreen(
										offering = currentOffering,
										onContinue = {
											view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
											viewModel.onNextClicked()
										},
										onButtonStateChange = { buttonState = it },
									)
								} else {
									// RevenueCat serves no offering for the placement — skip
									// ahead to the paywall step, which has its own fallback UI.
									LaunchedEffect(Unit) { viewModel.onNextClicked() }
								}
							}
							OfferingState.Failed -> {
								// offerings failed to load (e.g. offline) — skip this step
								LaunchedEffect(Unit) { viewModel.onNextClicked() }
							}
						}
					}
					DisplaySate.OnboardingPaywall -> {
						LaunchedEffect(isHardPaywall) {
							if (isHardPaywall) {
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							}
						}
						when (val billingOffering = onboardingOffering) {
							is OfferingState.Loaded -> OnboardingPaywallScreen(
								isHardPaywall = isHardPaywall,
								onBackPress = {
									viewModel.onPreviousClicked()
									uiState.alarmData?.let { viewModel.stopAlarm(it) }
								},
								onFinished = { viewModel.finishedOnboarding() },
								offering = billingOffering.offering,
								onEvent = { event ->
									if (isHardPaywall && (event is PaywallEvent.PurchaseCompleted || event is PaywallEvent.RestoreCompleted)) {
										uiState.alarmData?.let { alarm -> viewModel.resetAlarm(alarm) }
									}
									viewModel.onPaywallEvent(event)
								},
								onButtonStateChange = { buttonState = it }
							)
							OfferingState.Loading -> {
								// offerings still loading — hold on the placeholder, the
								// paywall step will render (or fall back) once resolved.
								OfferingsPlaceholderScreen(
									onButtonStateChange = { buttonState = it }
								)
							}
							OfferingState.Failed -> {
								// same fallback the paywall screen used for a null offering
								LaunchedEffect(Unit) { viewModel.finishedOnboarding() }
							}
						}
					}
				}
			}

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
					Column(
						modifier = Modifier
							.widthIn(max = 560.dp)
							.fillMaxWidth(),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(8.dp),
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

						if (uiState.displaySate == DisplaySate.FirstAlarmIntro) {
							TextButton(
								onClick = {
									view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
									viewModel.onSkipAlarmCreation()
								},
								enabled = buttonState == ButtonState.Enabled,
								modifier = Modifier
									.fillMaxWidth()
									.height(48.dp),
							) {
								Text(
									text = stringResource(R.string.onboarding_skip_alarm_creation),
									style = typography.bodyLarge.copy(color = colorScheme.onBackground.copy(alpha = 0.5f)),
								)
							}
						}
					}
				}
			}
		}
	}
}

/**
 * Minimal placeholder shown while the RevenueCat offerings are still loading.
 * Design pass pending (same as the rest of the flow).
 */
@Composable
private fun OfferingsPlaceholderScreen(onButtonStateChange: (ButtonState) -> Unit) {
	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Hidden)
	}
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		CircularProgressIndicator()
	}
}
