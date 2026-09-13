package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.AlarmResultClaude
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.CreateFirstAlarmScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.GreetingScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.OnboardingPaywallScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.PermissionScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components.ProblemScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen() {
	val viewModel : OnboardingViewModel = hiltViewModel()
	val alarmPickerViewModel : AlarmPickerViewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory -> factory.create(null) }
	val alarmPickerUiState by alarmPickerViewModel.uiState.collectAsStateWithLifecycle()

	val uiState by viewModel.displayState.collectAsStateWithLifecycle()

	LaunchedEffect(uiState.displaySate) {
		viewModel.analytics.screen("Onboarding_${uiState.displaySate.name.lowercase()}")
	}
	// pre-fetching for caching
	var offering by remember { mutableStateOf<Offering?>(null) }
	var loadFailed by remember { mutableStateOf(false) }

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
		DisplaySate.Greeting -> 1f / 6f
		DisplaySate.Problem -> 2f / 6f
		DisplaySate.Permission -> 3f / 6f
		DisplaySate.CreateFirstAlarm -> {
			val subProgress = when (alarmPickerUiState.progress) {
				com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.Progress.StartTime -> 1f / 3f
				com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.Progress.EndTime -> 2f / 3f
				com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.Progress.FullEditor -> 3f / 3f
			}
			(3f / 6f) + (subProgress / 6f)
		}
		DisplaySate.AlarmResult -> 5f / 6f
		DisplaySate.OnboardingPaywall -> 6f / 6f
	}

	val animatedProgress by animateFloatAsState(
		targetValue = progress,
		animationSpec = tween(durationMillis = 300),
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
					IconButton(onClick = { viewModel.onPreviousClicked() }) {
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
		AnimatedContent(
			targetState = uiState.displaySate,
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
			transitionSpec = {
				slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(270, easing = FastOutSlowInEasing)
				) togetherWith slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(220, easing = FastOutSlowInEasing)
				)
			},
		) { state ->
			when (state) {
				DisplaySate.Greeting -> GreetingScreen(onClickNext = { viewModel.onNextClicked() })
				DisplaySate.Problem -> ProblemScreen { viewModel.onNextClicked() }
				DisplaySate.Permission -> {
					PermissionScreen(
						missingSteps = uiState.missingSteps,
						allCriticalGranted = uiState.allCriticalGranted,
						onNext = { viewModel.onNextClicked() },
						refreshPermissionUiState = { viewModel.refreshPermissions() },
					)
				}
				DisplaySate.CreateFirstAlarm -> {
					CreateFirstAlarmScreen(
						alarmPickerViewModel = alarmPickerViewModel,
						onAlarmSetProceed = { viewModel.onNextClicked() },
//						onSettingAlarmCancelled = { viewModel.onPreviousClicked() },
						linearProgressBar = {
							LinearProgressIndicator(
								progress = { animatedProgress },
								modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
							)
						}
					)
				}
				DisplaySate.AlarmResult -> AlarmResultClaude(uiState.alarmData, onNextClick = { viewModel.onNextClicked() })
				DisplaySate.OnboardingPaywall -> {
					OnboardingPaywallScreen(onFinished = { viewModel.finishedOnboarding() }, loadFailed = loadFailed, offering = offering)
				}
			}
		}
	}
}