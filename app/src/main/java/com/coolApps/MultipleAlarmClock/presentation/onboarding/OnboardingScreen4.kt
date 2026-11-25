package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager.RevenueCatEntitlements
import com.coolApps.MultipleAlarmClock.data.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.CreateFirstAlarmScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.HardPaywallScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.OnboardingPaywallScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.PermissionScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.PixelLockScreenPreviewScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.UserProfileScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.UserStrugglesScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.WakeUpTimeScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen4(onShowPaywall: (PaywallTriggerType) -> Unit = {}) {
	val viewModel: OnboardingViewModel4 = hiltViewModel()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val featureFlags by viewModel.analytics.featureFlagsData.collectAsStateWithLifecycle()
	val isHardPaywall = featureFlags?.isHardPaywallEnabled ?: false
	val onboardingOffering by viewModel.onboardingOffering.collectAsStateWithLifecycle()

	val personalization = remember(uiState.primaryStruggle) {
		uiState.primaryStruggle.toPersonalization()
	}

	LaunchedEffect(uiState.displayState) {
		viewModel.analytics.screen("Onboarding_${uiState.displayState.name.lowercase()}")
	}

	var buttonState by remember { mutableStateOf(ButtonState.Enabled) }
	var childBackOverride by remember { mutableStateOf<(() -> Unit)?>(null) }

	LaunchedEffect(uiState.displayState) {
		if (uiState.displayState != DisplayState4.CreateFirstAlarm) {
			childBackOverride = null
		}
		buttonState = when (uiState.displayState) {
			DisplayState4.CreateFirstAlarm,
			DisplayState4.PrePaywall,
			DisplayState4.OnboardingPaywall -> ButtonState.Hidden
			DisplayState4.Permission -> if (uiState.allCriticalGranted) ButtonState.Enabled else ButtonState.Disabled
			else -> ButtonState.Enabled
		}
	}

	BackHandler(
		enabled = childBackOverride != null ||
			(uiState.canNavigateBack && uiState.displayState != DisplayState4.OnboardingPaywall),
	) {
		childBackOverride?.invoke() ?: viewModel.onPreviousClicked()
	}

	val view = LocalView.current
	val motionScheme = MaterialTheme.motionScheme

	val progress = when (uiState.displayState) {
		DisplayState4.UserStruggles -> 1f / 7f
		DisplayState4.UserProfile -> 2f / 7f
		DisplayState4.WakeUpTime -> 3f / 7f
		DisplayState4.Permission -> 4f / 7f
		DisplayState4.CreateFirstAlarm -> 5f / 7f
		DisplayState4.AlarmPreview -> 6f / 7f
		DisplayState4.PrePaywall -> 6f / 7f
		DisplayState4.OnboardingPaywall -> 7f / 7f
	}

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = motionScheme.defaultSpatialSpec(),
		label = "progress4",
	)

	Scaffold(
		topBar = {
			if (uiState.displayState != DisplayState4.OnboardingPaywall) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.statusBarsPadding()
						.padding(horizontal = 8.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					IconButton(
						enabled = childBackOverride != null || uiState.canNavigateBack,
						onClick = {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							childBackOverride?.invoke() ?: viewModel.onPreviousClicked()
						},
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = stringResource(R.string.alarm_picker_previous),
						)
					}
					Spacer(modifier = Modifier.width(8.dp))
					LinearProgressIndicator(
						progress = { animatedProgress },
						modifier = Modifier
							.fillMaxWidth()
							.padding(end = 12.dp),
					)
				}
			}
		},
	) { innerPadding ->
		val stepSpatialSpec = motionScheme.defaultSpatialSpec<IntOffset>()
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.consumeWindowInsets(innerPadding),
		) {
			AnimatedContent(
				targetState = uiState.displayState,
				modifier = Modifier.fillMaxSize(),
				transitionSpec = {
					val isForward = targetState > initialState
					slideIntoContainer(
						towards = if (isForward) {
							AnimatedContentTransitionScope.SlideDirection.Left
						} else {
							AnimatedContentTransitionScope.SlideDirection.Right
						},
						animationSpec = stepSpatialSpec,
					) togetherWith slideOutOfContainer(
						towards = if (isForward) {
							AnimatedContentTransitionScope.SlideDirection.Left
						} else {
							AnimatedContentTransitionScope.SlideDirection.Right
						},
						animationSpec = stepSpatialSpec,
					)
				},
				label = "onboarding4StepTransition",
			) { state ->
				when (state) {
					DisplayState4.UserStruggles -> UserStrugglesScreen(
						initialSelectedStruggles = uiState.selectedStruggles,
						onButtonStateChange = { buttonState = it },
						onStrugglesSelected = viewModel::onStrugglesSelected,
					)

					DisplayState4.UserProfile -> UserProfileScreen(
						initialSelectedProfile = uiState.selectedProfile,
						onButtonStateChange = { buttonState = it },
						onProfileSelected = viewModel::onProfileSelected,
					)
					DisplayState4.WakeUpTime -> WakeUpTimeScreen(
						initialHour = uiState.wakeUpHour,
						initialMinute = uiState.wakeUpMinute,
						personalization = personalization,
						onTimeChanged = viewModel::onWakeUpTimeChanged,
						onButtonStateChange = { buttonState = it },
					)
					DisplayState4.Permission -> PermissionScreen(
						missingSteps = uiState.missingSteps,
						allCriticalGranted = uiState.allCriticalGranted,
						refreshPermissionUiState = viewModel::refreshPermissions,
						onButtonStateChange = { buttonState = it },
					)

					DisplayState4.CreateFirstAlarm -> {
						val defaultMessage = stringResource(personalization.defaultAlarmMessageRes)
						val presetKey = "${uiState.wakeUpHour}:${uiState.wakeUpMinute}:${uiState.primaryStruggle?.name.orEmpty()}"
						val presetAlarm = remember(presetKey, defaultMessage) {
							viewModel.buildPresetAlarm(defaultMessage)
						}
						CreateFirstAlarmScreen(
							onAlarmSetProceed = { viewModel.onNextClicked() },
							linearProgressBar = {
								LinearProgressIndicator(
									progress = { animatedProgress },
									modifier = Modifier
										.fillMaxWidth()
										.padding(end = 16.dp),
								)
							},
							onShowPaywall = onShowPaywall,
							presetAlarm = presetAlarm,
							presetKey = presetKey,
							existingAlarmId = uiState.alarmData?.id,
							onBackOverrideChanged = { childBackOverride = it },
							onCancelled = { viewModel.onPreviousClicked() },
						)
					}

					DisplayState4.AlarmPreview -> PixelLockScreenPreviewScreen(
						alarmData = uiState.alarmData,
						personalization = personalization,
						onButtonStateChange = { buttonState = it },
					)

					DisplayState4.PrePaywall -> {
						when (val billingOffering = onboardingOffering) {
							OfferingState.Loading -> OfferingsPlaceholderScreen4(
								onButtonStateChange = { buttonState = it },
							)
							is OfferingState.Loaded -> {
								val currentOffering = billingOffering.offering
								if (currentOffering != null) {
									HardPaywallScreen(
										offering = currentOffering,
										onContinue = {
											view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
											viewModel.onNextClicked()
										},
										onButtonStateChange = { buttonState = it },
									)
								} else {
									LaunchedEffect(Unit) { viewModel.skipPrePaywall() }
								}
							}
							OfferingState.Failed -> {
								LaunchedEffect(Unit) { viewModel.skipPrePaywall() }
							}
						}
					}

					DisplayState4.OnboardingPaywall -> {
						var shouldRestoreAlarmOnFinish by remember { mutableStateOf(false) }
						val hasPaywallOffering = (onboardingOffering as? OfferingState.Loaded)?.offering != null
						LaunchedEffect(isHardPaywall, hasPaywallOffering) {
							if (isHardPaywall && hasPaywallOffering) {
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							}
						}
						when (val billingOffering = onboardingOffering) {
							is OfferingState.Loaded -> OnboardingPaywallScreen(
								isHardPaywall = isHardPaywall,
								onBackPress = {
									viewModel.onPreviousClicked()
									if (hasPaywallOffering) {
										uiState.alarmData?.let { viewModel.stopAlarm(it) }
									}
								},
								onFinished = {
									viewModel.finishedOnboarding(
										restoreAlarm = if (shouldRestoreAlarmOnFinish) uiState.alarmData else null,
									)
								},
								offering = billingOffering.offering,
								onEvent = { event ->
									val unlockedPremium = when (event) {
										is PaywallEvent.PurchaseCompleted -> true
										is PaywallEvent.RestoreCompleted ->
											event.customerInfo.entitlements[RevenueCatEntitlements.PREMIUM]?.isActive == true
										else -> false
									}
									if (isHardPaywall && unlockedPremium) {
										shouldRestoreAlarmOnFinish = true
									}
									viewModel.onPaywallEvent(event)
								},
								onButtonStateChange = { buttonState = it },
							)
							OfferingState.Loading -> OfferingsPlaceholderScreen4(
								onButtonStateChange = { buttonState = it },
							)
							OfferingState.Failed -> {
								LaunchedEffect(Unit) { viewModel.finishedOnboarding() }
							}
						}
					}
				}
			}

			AnimatedVisibility(
				visible = buttonState != ButtonState.Hidden,
				modifier = Modifier.align(Alignment.BottomCenter),
				enter = fadeIn(motionScheme.defaultEffectsSpec()) +
					slideInVertically(motionScheme.fastSpatialSpec()) { it },
				exit = fadeOut(motionScheme.defaultEffectsSpec<Float>()) +
					slideOutVertically(motionScheme.fastSpatialSpec<IntOffset>()) { it },
			) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(
							brush = Brush.verticalGradient(
								0f to colorScheme.background.copy(alpha = 0f),
								0.35f to colorScheme.background,
								1f to colorScheme.background,
							),
						)
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
						AnimatedVisibility(
							visible = uiState.displayState == DisplayState4.UserProfile,
							enter = fadeIn(motionScheme.defaultEffectsSpec()) +
								expandVertically(motionScheme.defaultSpatialSpec()),
							exit = fadeOut(motionScheme.defaultEffectsSpec()) +
								shrinkVertically(motionScheme.defaultSpatialSpec()),
						) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.Center,
								verticalAlignment = Alignment.CenterVertically,
							) {
								LockBadge4()
								Spacer(modifier = Modifier.width(6.dp))
								Text(
									text = stringResource(R.string.onboarding_profile_private_hint),
									style = typography.bodySmall,
									color = colorScheme.onBackground.copy(alpha = 0.45f),
								)
							}
						}

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
								contentColor = colorScheme.onPrimaryContainer,
							),
						) {
							Text(
								text = stringResource(R.string.permission_continue),
								style = typography.titleMedium,
							)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun LockBadge4() {
	val pulse = rememberInfiniteTransition(label = "lockPulse4")
	val lockScale by pulse.animateFloat(
		initialValue = 1f,
		targetValue = 1.12f,
		animationSpec = infiniteRepeatable(
			animation = tween(900, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse,
		),
		label = "lockPulseScale4",
	)
	Icon(
		imageVector = Icons.Rounded.Lock,
		contentDescription = null,
		tint = colorScheme.onBackground.copy(alpha = 0.5f),
		modifier = Modifier
			.graphicsLayer {
				scaleX = lockScale
				scaleY = lockScale
			}
			.size(18.dp),
	)
}

@Composable
private fun OfferingsPlaceholderScreen4(onButtonStateChange: (ButtonState) -> Unit) {
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
