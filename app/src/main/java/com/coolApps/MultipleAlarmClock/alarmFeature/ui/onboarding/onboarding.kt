package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.AlarmResultClaude
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.CreateFirstAlarmScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.FirstAlarmIntroView
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.GreetingScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.OnboardingPaywallScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.PermissionScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.ProblemScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.ButtonState
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen() {
	val viewModel : OnboardingViewModel = hiltViewModel()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()

	LaunchedEffect(uiState.displaySate) {
		viewModel.analytics.screen("Onboarding_${uiState.displaySate.name.lowercase()}")
	}
	var offering by remember { mutableStateOf<Offering?>(null) }
	var loadFailed by remember { mutableStateOf(false) }
	var buttonState by remember { mutableStateOf(ButtonState.Enabled) }
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
		DisplaySate.Greeting -> 1f / 7f
		DisplaySate.Problem -> 2f / 7f
		DisplaySate.Permission -> 3f / 7f
		DisplaySate.FirstAlarmIntro -> 4f / 7f
		DisplaySate.CreateFirstAlarm -> {
			5/7f
		}
		DisplaySate.AlarmResult -> 6f / 7f
		DisplaySate.OnboardingPaywall -> 7f / 7f
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
		},
		bottomBar = {
			AnimatedVisibility(
				visible = buttonState != ButtonState.Hidden,
				enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
				exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
			) {
				Box(
					modifier =
						Modifier.fillMaxWidth()
							.background(colorScheme.background)
							.navigationBarsPadding()
							.padding(26.dp)
							.padding(bottom = 20.dp)
							.animateContentSize(),
					contentAlignment = Alignment.Center,
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.End,
						verticalAlignment = Alignment.CenterVertically
					) {
						Button(
							onClick = {
								view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
								viewModel.onNextClicked()
							},
							enabled = buttonState == ButtonState.Enabled ,
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
	) { innerPadding ->
		AnimatedContent(
			targetState = uiState.displaySate,
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
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
						onButtonStateChange = { buttonState = it }
					)
				}
				DisplaySate.AlarmResult -> AlarmResultClaude(
					alarmData = uiState.alarmData,
					onButtonStateChange = { buttonState = it }
				)
				DisplaySate.OnboardingPaywall -> {
					OnboardingPaywallScreen(
						onFinished = { viewModel.finishedOnboarding() }, loadFailed = loadFailed, offering = offering,
						onPurchaseCompletedEvent = {customerInfo, storeTransaction -> viewModel.onPurchaseCompletedEvent(customerInfo,storeTransaction) },
						onRestoreCompletedEvent = { viewModel.onRestoreCompletedEvent(it) },
						onButtonStateChange = { buttonState = it }
					)
				}
			}
		}
	}
}