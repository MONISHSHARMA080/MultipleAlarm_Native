package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.ButtonState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FirstAlarmIntroView(
		modifier: Modifier = Modifier,
		onButtonStateChange: (ButtonState) -> Unit = {}
) {
	var showHeadline by remember { mutableStateOf(false) }
	var showConversation by remember { mutableStateOf(false) }
	var showSteps by remember { mutableStateOf(false) }
	var showEditNote by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Hidden)
		// 1. "Let's create your first alarm"
		showHeadline = true

		// Give the headline its own moment.
		delay(900.milliseconds)

		// 2. Conversation appears.
		showConversation = true

		// Let the user read it before introducing structure.
		showSteps = true
		delay(620.milliseconds)
		showEditNote = true
		onButtonStateChange(ButtonState.Enabled)
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
						),
						alignment = Alignment.TopCenter
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
					visible = showSteps,
					enter =
						fadeIn(animationSpec = tween(500)) +
								slideInVertically(
									animationSpec = tween(
										durationMillis = 550,
										easing = FastOutSlowInEasing
									),
									initialOffsetY = { 32 }
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
				.background(colorScheme.primaryContainer),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = number,
				style = typography.labelLarge,
				fontWeight = FontWeight.Bold,
				color = colorScheme.onPrimaryContainer
			)
		}

		Spacer(modifier = Modifier.width(16.dp))

		Column(
			modifier = Modifier.weight(1f)
		) {
			Text(
				text = title,
				style = typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = colorScheme.onBackground
			)

			Spacer(modifier = Modifier.height(3.dp))

			Text(
				text = description,
				style = typography.bodySmall,
				color = colorScheme.onSurfaceVariant
			)
		}
	}
}
