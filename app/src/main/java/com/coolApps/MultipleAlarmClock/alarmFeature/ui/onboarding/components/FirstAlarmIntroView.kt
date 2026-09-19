package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.graphics.graphicsLayer
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
	var showTitle by remember { mutableStateOf(false) }
	var moveTitleToTop by remember { mutableStateOf(false) }
	var showContent by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Disabled)

		showTitle = true
		delay(700.milliseconds)

		moveTitleToTop = true
		delay(800.milliseconds)

		showContent = true
		delay(600.milliseconds)
		onButtonStateChange(ButtonState.Enabled)
	}

	val enterReveal = fadeIn(
		animationSpec = tween(700, easing = FastOutSlowInEasing)
	) + slideInVertically(
		animationSpec = spring(
			dampingRatio = 0.8f,
			stiffness = Spring.StiffnessLow
		),
		initialOffsetY = { 90 }
	) + scaleIn(
		initialScale = 0.95f,
		animationSpec = spring(
			dampingRatio = 0.8f,
			stiffness = Spring.StiffnessLow
		)
	)

	val contentAlpha by animateFloatAsState(
		targetValue = if (showContent) 1f else 0f,
		animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
		label = "contentAlpha"
	)

	Column(
		modifier = modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				AnimatedVisibility(
					visible = showTitle,
					enter = enterReveal
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
					visible = moveTitleToTop,
					enter = expandVertically(
						animationSpec = tween(
							durationMillis = 800,
							easing = FastOutSlowInEasing
						),
						expandFrom = Alignment.Top
					)
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 24.dp)
							.graphicsLayer { alpha = contentAlpha },
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						StepsContent()
						EditNoteContent()

						Spacer(modifier = Modifier.height(120.dp))
					}
				}
			}
		}
	}
}

@Composable
private fun StepsContent() {
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

@Composable
private fun EditNoteContent() {
	Text(
		text = stringResource(R.string.onboarding_create_alarm_edit_note),
		modifier = Modifier.padding(top = 22.dp),
		style = typography.bodyMedium,
		textAlign = TextAlign.Center,
		color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
	)
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
