package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
	val scrollState = rememberScrollState()

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colorScheme.background)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.verticalScroll(scrollState)
				.padding(horizontal = 24.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Spacer(modifier = Modifier.weight(0.5f))

			Surface(
				modifier = Modifier.size(68.dp),
				shape = CircleShape,
				color = colorScheme.primaryContainer
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = Icons.Outlined.Alarm,
						contentDescription = null,
						tint = colorScheme.onPrimaryContainer,
						modifier = Modifier.size(36.dp)
					)
				}
			}

			Spacer(modifier = Modifier.height(20.dp))

			Text(
				text = stringResource(R.string.onboarding_create_alarm_title),
				style = typography.headlineMedium,
				fontWeight = FontWeight.SemiBold,
				textAlign = TextAlign.Center,
				color = colorScheme.onBackground
			)

			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = stringResource(R.string.onboarding_create_alarm_subtitle),
				style = typography.bodyMedium,
				textAlign = TextAlign.Center,
				color = colorScheme.onBackground.copy(alpha = 0.72f),
				modifier = Modifier.fillMaxWidth(0.9f)
			)

			Spacer(modifier = Modifier.height(28.dp))

			Column(
				modifier = Modifier
					.widthIn(max = 520.dp)
					.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				FirstAlarmInstructionCard(
					badgeText = "1",
					title = stringResource(R.string.onboarding_create_alarm_step_start_title),
					description = stringResource(R.string.onboarding_create_alarm_step_start_desc)
				)

				FirstAlarmInstructionCard(
					badgeText = "2",
					title = stringResource(R.string.onboarding_create_alarm_step_end_title),
					description = stringResource(R.string.onboarding_create_alarm_step_end_desc)
				)

				FirstAlarmInstructionCard(
					icon = Icons.Filled.Check,
					title = stringResource(R.string.onboarding_create_alarm_step_flexible_title),
					description = stringResource(R.string.onboarding_create_alarm_step_flexible_desc)
				)
			}

			Spacer(modifier = Modifier.weight(0.8f))
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
			Box(
				modifier = Modifier
					.widthIn(max = 520.dp)
					.fillMaxWidth()
			) {
				Button(
					onClick = {
						view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
						onContinue()
					},
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
						text = stringResource(R.string.onboarding_create_alarm_btn_continue),
						style = typography.titleMedium
					)
				}
			}
		}
	}
}

@Composable
private fun FirstAlarmInstructionCard(
	title: String,
	description: String,
	modifier: Modifier = Modifier,
	badgeText: String? = null,
	icon: ImageVector? = null
) {
	Surface(
		modifier = modifier.fillMaxWidth(),
		shape = shapes.large,
		color = colorScheme.surfaceContainerHigh,
		tonalElevation = 1.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				modifier = Modifier.size(38.dp),
				shape = CircleShape,
				color = colorScheme.primary.copy(alpha = 0.12f)
			) {
				Box(contentAlignment = Alignment.Center) {
					if (icon != null) {
						Icon(
							imageVector = icon,
							contentDescription = null,
							tint = colorScheme.primary,
							modifier = Modifier.size(20.dp)
						)
					} else if (badgeText != null) {
						Text(
							text = badgeText,
							style = typography.titleMedium,
							fontWeight = FontWeight.Bold,
							color = colorScheme.primary
						)
					}
				}
			}

			Spacer(modifier = Modifier.width(14.dp))

			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				Text(
					text = title,
					style = typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					color = colorScheme.onSurface
				)
				Text(
					text = description,
					style = typography.bodyMedium,
					color = colorScheme.onSurfaceVariant
				)
			}
		}
	}
}
