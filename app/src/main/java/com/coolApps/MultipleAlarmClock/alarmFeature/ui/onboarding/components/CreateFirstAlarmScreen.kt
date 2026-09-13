package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

//import androidx.compose.animation.slideIntoContainer
//import androidx.compose.animation.slideOutOfContainer
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
	var showConversation by remember { mutableStateOf(false) }
	var showSteps by remember { mutableStateOf(false) }
	var showEditNote by remember { mutableStateOf(false) }
	var showButton by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		// 1. "Let's create your first alarm"
		showHeadline = true

		// Give the headline its own moment.
		delay(600.milliseconds)

		// 2. Conversation appears.
		showConversation = true

		// Let the user read it before introducing structure.
		delay(700.milliseconds)
		showSteps = true
		delay(500.milliseconds)
		showButton = true
		showEditNote = true
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colorScheme.background)
	) {

		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 28.dp)
					.align(Alignment.Center)
					.animateContentSize(
						animationSpec = tween(
							durationMillis = 450,
							easing = FastOutSlowInEasing
						)
					),
				horizontalAlignment = Alignment.CenterHorizontally
			) {

				AnimatedVisibility(
					visible = showHeadline,
					enter =
						fadeIn(
							animationSpec = tween(500)
						) +
						slideInVertically(
							animationSpec = tween(500),
							initialOffsetY = { 32 }
						)
				) {
					Text(
						text = stringResource(R.string.onboarding_create_alarm_title),
						style = typography.headlineLarge,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						color = colorScheme.onBackground
					)
				}

				AnimatedVisibility(
					visible = showConversation,
					enter =
						fadeIn(
							animationSpec = tween(
								durationMillis = 500
							)
						) +
								slideInVertically(
									animationSpec = tween(
										durationMillis = 550,
										easing = FastOutSlowInEasing
									),
									initialOffsetY = { 18 }
								)
				) {
					Text(
						text = stringResource(
							R.string.onboarding_create_alarm_subtitle
						),
						modifier = Modifier
							.padding(top = 8.dp)
							.fillMaxWidth(),
						style = typography.titleMedium,
						fontWeight = FontWeight.Normal,
						lineHeight = 25.sp,
						textAlign = TextAlign.Center,
						color = colorScheme.onSurfaceVariant
					)
				}

				AnimatedVisibility(
					visible = showSteps,
					enter =
						fadeIn(animationSpec = tween(500)) +
								expandVertically(
									animationSpec = tween(
										durationMillis = 550,
										easing = FastOutSlowInEasing
									),
									expandFrom = Alignment.Top
								)
				) {
					Column(
						modifier = Modifier
							.padding(top = 32.dp)
							.fillMaxWidth()
					) {
						AlarmIntroStep(
							number = "1",
							title = stringResource(
								R.string.onboarding_create_alarm_step_start_title
							),
							description = stringResource(
								R.string.onboarding_create_alarm_step_start_desc
							)
						)

						Spacer(modifier = Modifier.height(14.dp))

						AlarmIntroStep(
							number = "2",
							title = stringResource(
								R.string.onboarding_create_alarm_step_end_title
							),
							description = stringResource(
								R.string.onboarding_create_alarm_step_end_desc
							)
						)
					}
				}

				AnimatedVisibility(
					visible = showEditNote,
					enter = fadeIn(
						animationSpec = tween(500)
					)
				) {
					Text(
						text = stringResource(R.string.onboarding_create_alarm_edit_note),
						modifier = Modifier.padding(top = 22.dp),
						style = typography.bodyMedium,
						textAlign = TextAlign.Center,
						color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
					)
				}
			}
		}

		// ─────────────────────────────
		// BUTTON
		// ─────────────────────────────

		AnimatedVisibility(
			visible = showButton,
			enter =
				fadeIn(
					animationSpec = tween(350)
				) +
						slideInVertically(
							animationSpec = tween(400),
							initialOffsetY = { 20 }
						)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(
						horizontal = 24.dp,
						vertical = 24.dp
					)
			) {
				Button(
					onClick = {
						view.performHapticFeedback(
							HapticFeedbackConstants.VIRTUAL_KEY
						)
						onContinue()
					},
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
					shape = MaterialTheme.shapes.extraLarge,
					colors = ButtonDefaults.buttonColors(
						containerColor =
							MaterialTheme.colorScheme.primaryContainer,
						contentColor =
							MaterialTheme.colorScheme.onPrimaryContainer
					)
				) {
					Text(
						text = stringResource(
							R.string.onboarding_create_alarm_btn_continue
						),
						style = MaterialTheme.typography.titleMedium
					)
				}
			}
		}
	}
}


@Composable
private fun AlarmIntroStep(
		number: String,
		title: String,
		description: String,
		modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier
			.fillMaxWidth(),
		verticalAlignment = Alignment.Top
	) {
		Box(
			modifier = Modifier
				.padding(top = 2.dp)
				.size(32.dp)
				.clip(CircleShape)
				.background(
					MaterialTheme.colorScheme.primaryContainer
				),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = number,
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimaryContainer
			)
		}

		Spacer(modifier = Modifier.width(16.dp))

		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onBackground
			)

			Spacer(modifier = Modifier.height(3.dp))

			Text(
				text = description,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

/**
 * A single "fine print" line for the secondary detail block —
 * deliberately quiet: no icons, no card chrome, no numbered badges.
 * It should read like a caption you can skip, not a step you must follow.
 */
@Composable
private fun IntroDetailRow(
	title: String,
	description: String,
	modifier: Modifier = Modifier
) {
	Text(
		text = buildString {
			append(title)
			append(" — ")
			append(description)
		},
		style = typography.bodySmall,
		textAlign = TextAlign.Center,
		color = colorScheme.onSurfaceVariant,
		modifier = modifier.fillMaxWidth(0.85f)
	)
}
