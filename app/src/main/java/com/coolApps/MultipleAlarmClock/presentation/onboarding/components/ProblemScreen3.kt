package com.coolApps.MultipleAlarmClock.presentation.onboarding.components

import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProblemScreen3(
	onButtonStateChange: (ButtonState) -> Unit = {},
) {
	val view = LocalView.current
	var showTitle by remember { mutableStateOf(false) }
	var showSubtitle by remember { mutableStateOf(false) }
	var showHero by remember { mutableStateOf(false) }
	var alternate by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Enabled)
		showTitle = true
		delay(350.milliseconds)
		showSubtitle = true
		delay(300.milliseconds)
		showHero = true
		while (true) {
			delay(3.seconds)
			alternate = !alternate
		}
	}

	val enterReveal = fadeIn(motionScheme.slowEffectsSpec()) + slideInVertically(
		animationSpec = motionScheme.slowSpatialSpec(),
		initialOffsetY = { 40 }
	) + scaleIn(
		initialScale = 0.95f,
		animationSpec = motionScheme.slowSpatialSpec()
	)

	val slowEffects = motionScheme.slowEffectsSpec<Float>()
	val slideSpec = motionScheme.defaultEffectsSpec<IntOffset>()

	val titleChangeTransition: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = {
		(fadeIn(slowEffects) + slideInVertically(
			animationSpec = slideSpec,
			initialOffsetY = { (it * 0.6).toInt() }
		)).togetherWith(
			fadeOut(slowEffects) + slideOutVertically(
				animationSpec = slideSpec,
				targetOffsetY = { (-it * 0.6).toInt() }
			)
		)
	}

	val subtitleChangeTransition: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = {
		(fadeIn(slowEffects) + scaleIn(
			initialScale = 0.98f,
			animationSpec = slowEffects
		)).togetherWith(fadeOut(slowEffects))
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Spacer(Modifier.height(8.dp))

		AnimatedVisibility(visible = showTitle, enter = enterReveal) {
			AnimatedContent(
				targetState = alternate,
				transitionSpec = titleChangeTransition,
				label = "title_transition"
			) { isAlternate ->
				Text(
					text = if (isAlternate) {
						stringResource(R.string.onboarding_problem3_title_alternate)
					} else {
						stringResource(R.string.onboarding_problem3_title)
					},
					textAlign = TextAlign.Center,
					style = typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onBackground,
				)
			}
		}

		Spacer(Modifier.height(10.dp))

		AnimatedVisibility(visible = showSubtitle, enter = enterReveal) {
			AnimatedContent(
				targetState = alternate,
				transitionSpec = subtitleChangeTransition,
				label = "subtitle_transition"
			) { isAlternate ->
				Text(
					text = if (isAlternate) {
						stringResource(R.string.onboarding_problem3_subtitle_alternate)
					} else {
						stringResource(R.string.onboarding_problem3_subtitle)
					},
					textAlign = TextAlign.Center,
					style = typography.titleMedium,
					color = colorScheme.onBackground.copy(alpha = 0.6f),
					modifier = Modifier.widthIn(max = 360.dp)
				)
			}
		}

		Spacer(Modifier.weight(1f))

		AnimatedVisibility(
			visible = showHero,
			enter = enterReveal,
			modifier = Modifier.widthIn(max = 480.dp),
		) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(14.dp),
			) {
				RepeatAlarmCard()
				RingingAlarmStack(
					onAlarmDismissed = {
						view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
					}
				)
			}
		}

		Spacer(Modifier.weight(1f))
	}
}
