package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerScreen
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.AlarmPickerViewModel
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.listAlarmRingtone.ListAlarmSoundScreen
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class CreateFirstAlarmStep {
	Intro,
	Picker,
	SoundList
}

@Composable
fun CreateFirstAlarmScreen(
	alarmPickerViewModel: AlarmPickerViewModel,
	onAlarmSetProceed: () -> Unit,
	modifier: Modifier = Modifier,
	linearProgressBar: @Composable () -> Unit = {},
) {
	var currentStep by remember { mutableStateOf(CreateFirstAlarmStep.Intro) }
	val selected by alarmPickerViewModel.selectedAlarmSound.collectAsStateWithLifecycle()
	val previewing by alarmPickerViewModel.previewingSound.collectAsStateWithLifecycle()

	AnimatedContent(
		targetState = currentStep,
		modifier = modifier,
		transitionSpec = {
			val isForward = targetState.ordinal > initialState.ordinal
			if (isForward) {
				(slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(270, easing = FastOutSlowInEasing)
				) + fadeIn(tween(250))) togetherWith (slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Left,
					animationSpec = tween(220, easing = FastOutSlowInEasing)
				) + fadeOut(tween(190)))
			} else {
				(slideIntoContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(270, easing = FastOutSlowInEasing)
				) + fadeIn(tween(250))) togetherWith (slideOutOfContainer(
					towards = AnimatedContentTransitionScope.SlideDirection.Right,
					animationSpec = tween(220, easing = FastOutSlowInEasing)
				) + fadeOut(tween(190)))
			}
		},
		label = "create_first_alarm_navigation"
	) { step ->
		when (step) {
			CreateFirstAlarmStep.Intro -> {
				FirstAlarmIntroView(
					onContinue = {
						currentStep = CreateFirstAlarmStep.Picker
					}
				)
			}

			CreateFirstAlarmStep.Picker -> {
				AlarmPickerScreen(
					alarmSetProceed = onAlarmSetProceed,
					forNewAlarm = true,
					viewModel = alarmPickerViewModel,
					onNavigateToSoundList = {
						currentStep = CreateFirstAlarmStep.SoundList
					},
					settingAlarmCancelled = {
						currentStep = CreateFirstAlarmStep.Intro
					},
					onNavigateToPaywall = {},
					linearProgressBar = linearProgressBar
				)
			}

			CreateFirstAlarmStep.SoundList -> {
				ListAlarmSoundScreen(
					alarmPickerViewModel,
					previewingUri = previewing?.soundUri,
					selectedUri = selected?.soundUri,
					onBack = {
						currentStep = CreateFirstAlarmStep.Picker
					},
					onSelected = { sound ->
						alarmPickerViewModel.onAlarmSoundSelected(sound)
					}
				)
			}
		}
	}
}

@Composable
private fun FirstAlarmIntroView(
	onContinue: () -> Unit,
	modifier: Modifier = Modifier
) {
	val view = LocalView.current
	var showHeadline by remember { mutableStateOf(false) }
	var showDescription by remember { mutableStateOf(false) }
	var showButton by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
//		showHeadline = true
		showButton = true
		delay(400.milliseconds)
		showDescription = true
		delay(700.milliseconds)
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colorScheme.background)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.padding(horizontal = 32.dp),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.animateContentSize(animationSpec = tween(1000)),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				AnimatedVisibility(
					visible = true,
					enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300), initialOffsetY = { 30 })
				) {
					Text(
						text = stringResource(R.string.onboarding_create_alarm_opal_step_1),
						style = typography.headlineLarge,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						color = colorScheme.onBackground
					)
				}
				AnimatedVisibility(
					visible = showDescription,
					enter = fadeIn(animationSpec = tween(400)) + expandVertically(animationSpec = tween(400))
				) {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Spacer(modifier = Modifier.height(24.dp))
						Text(
							text = stringResource(R.string.onboarding_create_alarm_opal_step_2),
							style = typography.titleMedium,
							fontWeight = FontWeight.Medium,
							textAlign = TextAlign.Center,
							color = colorScheme.onBackground.copy(alpha = 0.6f)
						)
					}
				}
			}
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.background(colorScheme.background)
				.navigationBarsPadding()
				.padding(26.dp)
				.padding(bottom = 20.dp)
				.animateContentSize(),
			contentAlignment = Alignment.Center
		) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Button(
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onContinue()
					},
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
					shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
					colors = ButtonDefaults.buttonColors(
						containerColor = colorScheme.primaryContainer,
						contentColor = colorScheme.onPrimaryContainer
					)
				) {
					Text(
						text = stringResource(R.string.onboarding_create_alarm_btn_continue),
						style = typography.titleMedium
					)
				}

//				AnimatedVisibility(
//					visible = showButton,
//					enter = fadeIn(animationSpec = tween(1000)) + expandVertically(animationSpec = tween(1000))
//				) {
//				}
			}
		}
	}
}
