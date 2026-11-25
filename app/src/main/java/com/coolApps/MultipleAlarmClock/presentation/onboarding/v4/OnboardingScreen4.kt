package com.coolApps.MultipleAlarmClock.presentation.onboarding.v4

import android.view.HapticFeedbackConstants
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.data.billing.EntitlementManager.RevenueCatEntitlements
import com.coolApps.MultipleAlarmClock.data.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.CreateFirstAlarmScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.HardPaywallScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.OnboardingPaywallScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.PermissionScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.UserProfileScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.components.UserStrugglesScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.v4.components.PixelLockScreenPreviewScreen
import com.coolApps.MultipleAlarmClock.presentation.onboarding.v4.components.WakeUpTimeScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen4(onShowPaywall: (PaywallTriggerType) -> Unit = {}) {
	val viewModel: OnboardingViewModel4 = hiltViewModel()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val featureFlags by viewModel.analytics.featureFlagsData.collectAsStateWithLifecycle()
	val isHardPaywall = featureFlags?.isHardPaywallEnabled ?: false
	val onboardingOffering by viewModel.onboardingOffering.collectAsStateWithLifecycle()

	val backStack = rememberNavBackStack(Onboarding4Route.UserStruggles)
	val currentRoute = (backStack.lastOrNull() as? Onboarding4Route) ?: Onboarding4Route.UserStruggles

	val personalization = remember(uiState.primaryStruggle) {
		uiState.primaryStruggle.toPersonalization()
	}

	LaunchedEffect(currentRoute) {
		viewModel.analytics.screen("Onboarding_${currentRoute.stepName.lowercase()}")
	}

	var buttonState by remember { mutableStateOf(ButtonState.Enabled) }
	var childBackOverride by remember { mutableStateOf<(() -> Unit)?>(null) }

	LaunchedEffect(currentRoute) {
		if (currentRoute != Onboarding4Route.CreateFirstAlarm) {
			childBackOverride = null
		}
		buttonState = when (currentRoute) {
			Onboarding4Route.CreateFirstAlarm,
			Onboarding4Route.PrePaywall,
			Onboarding4Route.OnboardingPaywall -> ButtonState.Hidden
			Onboarding4Route.Permission -> if (uiState.allCriticalGranted) ButtonState.Enabled else ButtonState.Disabled
			else -> ButtonState.Enabled
		}
	}

	val navigateNext: () -> Unit = {
		viewModel.onNextClicked(currentRoute)?.let(backStack::add)
	}

	val navigateBack: () -> Unit = {
		if (backStack.size > 1) {
			viewModel.onPreviousClicked(currentRoute)
			backStack.removeLastOrNull()
		}
	}

	val skipPrePaywall: () -> Unit = {
		if (backStack.lastOrNull() == Onboarding4Route.PrePaywall) {
			backStack[backStack.lastIndex] = Onboarding4Route.OnboardingPaywall
		}
	}

	val view = LocalView.current
	val motionScheme = MaterialTheme.motionScheme

	val animatedProgress by animateFloatAsState(
		targetValue = currentRoute.progress,
		animationSpec = motionScheme.defaultSpatialSpec(),
		label = "progress4",
	)

	Scaffold(
		topBar = {
			if (currentRoute != Onboarding4Route.OnboardingPaywall) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.statusBarsPadding()
						.padding(horizontal = 8.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					IconButton(
						enabled = childBackOverride != null || backStack.size > 1,
						onClick = dropUnlessResumed {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							childBackOverride?.invoke() ?: navigateBack()
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
			NavDisplay(
				backStack = backStack,
				onBack = navigateBack,
				modifier = Modifier.fillMaxSize(),
				entryDecorators = listOf(
					rememberSaveableStateHolderNavEntryDecorator(),
					rememberViewModelStoreNavEntryDecorator(),
				),
				transitionSpec = {
					slideInHorizontally(stepSpatialSpec) { it } togetherWith
						slideOutHorizontally(stepSpatialSpec) { -it }
				},
				popTransitionSpec = {
					slideInHorizontally(stepSpatialSpec) { -it } togetherWith
						slideOutHorizontally(stepSpatialSpec) { it }
				},
				predictivePopTransitionSpec = {
					slideInHorizontally(stepSpatialSpec) { -it } togetherWith
						slideOutHorizontally(stepSpatialSpec) { it }
				},
				entryProvider = entryProvider {
					entry<Onboarding4Route.UserStruggles> {
						UserStrugglesScreen(
							initialSelectedStruggles = uiState.selectedStruggles,
							onButtonStateChange = { buttonState = it },
							onStrugglesSelected = viewModel::onStrugglesSelected,
						)
					}

					entry<Onboarding4Route.UserProfile> {
						UserProfileScreen(
							initialSelectedProfile = uiState.selectedProfile,
							onButtonStateChange = { buttonState = it },
							onProfileSelected = viewModel::onProfileSelected,
						)
					}

					entry<Onboarding4Route.WakeUpTime> {
						WakeUpTimeScreen(
							initialHour = uiState.wakeUpHour,
							initialMinute = uiState.wakeUpMinute,
							personalization = personalization,
							onTimeChanged = viewModel::onWakeUpTimeChanged,
							onButtonStateChange = { buttonState = it },
						)
					}

					entry<Onboarding4Route.Permission> {
						PermissionScreen(
							missingSteps = uiState.missingSteps,
							allCriticalGranted = uiState.allCriticalGranted,
							refreshPermissionUiState = viewModel::refreshPermissions,
							onButtonStateChange = { buttonState = it },
						)
					}

					entry<Onboarding4Route.CreateFirstAlarm> {
						val initialAlarm = remember { viewModel.resolveInitialAlarm() }
						val onNext = dropUnlessResumed { navigateNext() }
						val onCancelled = dropUnlessResumed { navigateBack() }
						CreateFirstAlarmScreen(
							onAlarmSetProceed = { savedAlarm ->
								viewModel.onAlarmSaved(savedAlarm)
								onNext()
							},
							linearProgressBar = {
								LinearProgressIndicator(
									progress = { animatedProgress },
									modifier = Modifier
										.fillMaxWidth()
										.padding(end = 16.dp),
								)
							},
							onShowPaywall = onShowPaywall,
							initialAlarm = initialAlarm,
							onBackOverrideChanged = { childBackOverride = it },
							onCancelled = onCancelled,
						)
					}

					entry<Onboarding4Route.AlarmPreview> {
						PixelLockScreenPreviewScreen(
							alarmData = uiState.alarmData,
							personalization = personalization,
							onButtonStateChange = { buttonState = it },
						)
					}

					entry<Onboarding4Route.PrePaywall> {
						val onContinue = dropUnlessResumed {
							view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
							navigateNext()
						}
						when (val billingOffering = onboardingOffering) {
							OfferingState.Loading -> OfferingsPlaceholderScreen4(
								onButtonStateChange = { buttonState = it },
							)
							is OfferingState.Loaded -> {
								val currentOffering = billingOffering.offering
								if (currentOffering != null) {
									HardPaywallScreen(
										offering = currentOffering,
										onContinue = onContinue,
										onButtonStateChange = { buttonState = it },
									)
								} else {
									LaunchedEffect(Unit) { skipPrePaywall() }
								}
							}
							OfferingState.Failed -> {
								LaunchedEffect(Unit) { skipPrePaywall() }
							}
						}
					}

					entry<Onboarding4Route.OnboardingPaywall> {
						var shouldRestoreAlarmOnFinish by remember { mutableStateOf(false) }
						val hasPaywallOffering = (onboardingOffering as? OfferingState.Loaded)?.offering != null
						val onBackPress = dropUnlessResumed {
							navigateBack()
							if (hasPaywallOffering) {
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							}
						}
						LaunchedEffect(isHardPaywall, hasPaywallOffering) {
							if (isHardPaywall && hasPaywallOffering) {
								uiState.alarmData?.let { viewModel.stopAlarm(it) }
							}
						}
						when (val billingOffering = onboardingOffering) {
							is OfferingState.Loaded -> OnboardingPaywallScreen(
								isHardPaywall = isHardPaywall,
								onBackPress = onBackPress,
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
				},
			)

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
							visible = currentRoute == Onboarding4Route.UserProfile,
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
							onClick = dropUnlessResumed {
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								navigateNext()
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
