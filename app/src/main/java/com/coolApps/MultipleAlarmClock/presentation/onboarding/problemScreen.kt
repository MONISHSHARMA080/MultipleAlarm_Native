@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.coolApps.MultipleAlarmClock.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private enum class ProblemPhase {
	Building,
	ShowingProblem,
	Crushing
}

@Composable
fun ProblemScreen(
	onButtonStateChange: (ButtonState) -> Unit = {}
) {
	val view = LocalView.current
	var phase by remember { mutableStateOf(ProblemPhase.Building) }
	var visibleAlarms by remember { mutableIntStateOf(0) }
	
	var showAlarmSpace by remember { mutableStateOf(false) }
	var showTitle by remember { mutableStateOf(false) }
	var showSubtitle by remember { mutableStateOf(false) }
	var showAnimation by remember { mutableStateOf(false) }

	val alarms = remember {
		listOf("7:00", "7:05", "7:10",  "7:15", "7:20",)
	}

	val cardHeight = 64.dp
	val cardGap = 12.dp
	val cardSpacing = cardHeight + cardGap
	val animationHeight = cardHeight + cardSpacing * (alarms.size - 1)

	LaunchedEffect(Unit) {
		var isFirstRun = true
		while (true) {
			if (isFirstRun) {
				onButtonStateChange(ButtonState.Disabled)
			}

			phase = ProblemPhase.Building
			visibleAlarms = 0

			showTitle = true
			delay(700.milliseconds)

			showSubtitle = true
			delay(780.milliseconds)

			showAlarmSpace = true
			delay(950.milliseconds)

			showAnimation = true

			alarms.indices.forEach { index ->
				delay(250.milliseconds)
				visibleAlarms = index + 1
				view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
			}
			delay(450.milliseconds)
			phase = ProblemPhase.ShowingProblem
			
			delay(800.milliseconds)

			phase = ProblemPhase.Crushing
			view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

			delay(500.milliseconds)

			if (isFirstRun) {
				onButtonStateChange(ButtonState.Enabled)
				isFirstRun = false
			}

			// repat it
			delay(2.5.seconds)

			showTitle = false
			showSubtitle = false
			showAlarmSpace = false
			showAnimation = false

			delay(600.milliseconds)
		}
	}

	val enterReveal = fadeIn(
		animationSpec = tween(700, easing = FastOutSlowInEasing)
	) + slideInVertically(
		animationSpec = spring(
			dampingRatio = 0.8f,
			stiffness = Spring.StiffnessLow
		),
		initialOffsetY = { 40 }
	) + scaleIn(
		initialScale = 0.95f,
		animationSpec = spring(
			dampingRatio = 0.8f,
			stiffness = Spring.StiffnessLow
		)
	)

	val textChangeTransition: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform = {
		(fadeIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)) + slideInVertically(
			animationSpec = spring(
				dampingRatio = 0.85f,
				stiffness = Spring.StiffnessLow
			),
			initialOffsetY = { -30 }
		)).togetherWith(
			fadeOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)) + slideOutVertically(
				animationSpec = spring(
					dampingRatio = 0.85f,
					stiffness = Spring.StiffnessLow
				),
				targetOffsetY = { 30 }
			)
		)
	}
	
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.animateContentSize(
						animationSpec = tween(
							durationMillis = 450,
							easing = FastOutSlowInEasing
						),
						alignment = Alignment.BottomCenter
					),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				AnimatedVisibility(
					visible = showAlarmSpace,
					enter = expandVertically(
						animationSpec = tween(
							durationMillis = 450,
							easing = FastOutSlowInEasing
						),
						expandFrom = Alignment.Top
					)
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.height(animationHeight + 34.dp),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center
					) {
						AnimatedVisibility(
							visible = showAnimation,
							enter = enterReveal
						) {
							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
							) {
								ProblemAnimation(
									phase = phase,
									alarms = alarms,
									visibleAlarms = visibleAlarms,
									cardHeight = cardHeight,
									cardSpacing = cardSpacing,
									modifier = Modifier
										.fillMaxWidth()
										.height(animationHeight)
								)
								Spacer(modifier = Modifier.height(34.dp))
							}
						}
					}
				}

				AnimatedVisibility(
					visible = showTitle,
					enter = enterReveal
				) {
					AnimatedContent(
						targetState = phase == ProblemPhase.Crushing,
						transitionSpec = textChangeTransition,
						label = "title_transition"
					) { isCrushing ->
						Text(
							text = if (isCrushing) {
								stringResource(R.string.onboarding_problem_better_way_title)
							} else {
								stringResource(R.string.onboarding_problem_title)
							},
							style = typography.headlineLargeEmphasized,
							textAlign = TextAlign.Center,
							color = colorScheme.onBackground
						)
					}
				}

				SubtitleBlock(
					showSubtitle = showSubtitle,
					phase = phase,
					enterReveal = enterReveal,
					textChangeTransition = textChangeTransition
				)
			}
		}
	}
}

@Composable
private fun ProblemAnimation(
	phase: ProblemPhase,
	alarms: List<String>,
	visibleAlarms: Int,
	cardHeight: Dp,
	cardSpacing: Dp,
	modifier: Modifier = Modifier
) {
	val transition = updateTransition(
		targetState = phase,
		label = "alarm transformation"
	)

	val crushProgress by transition.animateFloat(
		transitionSpec = {
			spring(
				dampingRatio = 0.85f,
				stiffness = Spring.StiffnessLow
			)
		},
		label = "crush progress"
	) { state ->
		when (state) {
			ProblemPhase.Building,
			ProblemPhase.ShowingProblem -> 0f
			ProblemPhase.Crushing -> 1f
		}
	}

	val finalCardWidth by transition.animateDp(
		transitionSpec = {
			spring(
				dampingRatio = 0.85f,
				stiffness = Spring.StiffnessLow
			)
		},
		label = "final card width"
	) { state ->
		when (state) {
			ProblemPhase.Building,
			ProblemPhase.ShowingProblem -> 0.dp
			ProblemPhase.Crushing -> 330.dp
		}
	}

	val finalCardHeight by transition.animateDp(
		transitionSpec = {
			spring(
				dampingRatio = 0.85f,
				stiffness = Spring.StiffnessLow
			)
		},
		label = "final card height"
	) { state ->
		when (state) {
			ProblemPhase.Building,
			ProblemPhase.ShowingProblem -> 0.dp
			ProblemPhase.Crushing -> 92.dp
		}
	}

	val finalCardAlpha by transition.animateFloat(
		transitionSpec = {
			spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
		},
		label = "final card alpha"
	) { state ->
		when (state) {
			ProblemPhase.Building,
			ProblemPhase.ShowingProblem -> 0f
			ProblemPhase.Crushing -> 1f
		}
	}

	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		alarms.forEachIndexed { index, time ->
			CrushingAlarm(
				time = time,
				index = index,
				total = alarms.size,
				visible = index < visibleAlarms,
				crushProgress = crushProgress,
				cardHeight = cardHeight,
				cardSpacing = cardSpacing
			)
		}

		ModifiedAlarm(
			width = finalCardWidth,
			height = finalCardHeight,
			alpha = finalCardAlpha
		)
	}
}

@Composable
private fun CrushingAlarm(
	time: String,
	index: Int,
	total: Int,
	visible: Boolean,
	crushProgress: Float,
	cardHeight: Dp,
	cardSpacing: Dp
) {
	val center = (total - 1) / 2f
	val normalDistance = (index - center) * cardSpacing.value

	val entrance by animateFloatAsState(
		targetValue = if (visible) 1f else 0f,
		animationSpec = spring(
			dampingRatio = 0.85f,
			stiffness = Spring.StiffnessLow
		),
		label = "entrance"
	)

	val restingY = normalDistance * (1f - crushProgress)
	val enterOffsetY = (1f - entrance) * -90f
	val y = restingY + enterOffsetY

	val height = cardHeight.value * (1f - crushProgress)
	val horizontalScale = 1f - (crushProgress * 0.08f)
	val rotation = (index - center) * -1.2f * crushProgress
	val alpha = entrance * (1f - crushProgress * 0.55f)

	Surface(
		modifier = Modifier
			.fillMaxWidth(0.86f)
			.height(height.dp)
			.offset(y = y.dp)
			.graphicsLayer {
				rotationZ = rotation
				this.alpha = alpha
				scaleX = horizontalScale
			},
		shape = shapes.extraLarge,
		color = colorScheme.surfaceContainerHigh,
		tonalElevation = 1.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 20.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column(
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				Text(
					text = time,
					style = typography.titleLargeEmphasized,
					color = colorScheme.onSurface
				)
				Text(
					text = stringResource(R.string.onboarding_problem_wake_up),
					style = typography.bodyMedium,
					color = colorScheme.onSurfaceVariant
				)
			}

			Switch(
				checked = true,
				onCheckedChange = null,
				colors = SwitchDefaults.colors(
					checkedThumbColor = colorScheme.onPrimary,
					checkedTrackColor = colorScheme.primary
				)
			)
		}
	}
}

@Composable private fun ModifiedAlarm(
	width: Dp,
	height: Dp,
	alpha: Float
) {
	Surface(
		modifier = Modifier
			.width(width)
			.height(height)
			.graphicsLayer {
				this.alpha = alpha
			},
		shape = shapes.extraExtraLarge,
		color = colorScheme.primaryContainer
	) {
		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 22.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Surface(
				modifier = Modifier.size(48.dp),
				shape = shapes.extraLarge,
				color = colorScheme.primary.copy(alpha = 0.12f)
			) {
				Box(contentAlignment = Alignment.Center) {
					Icon(
						imageVector = Icons.Outlined.Alarm,
						contentDescription = null,
						tint = colorScheme.onPrimaryContainer,
						modifier = Modifier.size(25.dp)
					)
				}
			}
			Column {
				Text(
					text = stringResource(R.string.onboarding_problem_preview_interval),
					style = typography.titleLargeEmphasized,
					color = colorScheme.onPrimaryContainer
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = stringResource(R.string.onboarding_problem_preview_frequency),
					style = typography.bodyMedium,
					color = colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
				)
			}

			Spacer(modifier = Modifier.width(13.dp))
			Switch(
				checked = true,
				onCheckedChange = null,
				colors = SwitchDefaults.colors(
					checkedThumbColor = colorScheme.onPrimary,
					checkedTrackColor = colorScheme.primary
				)
			)

		}
	}
}

@Composable
private fun SubtitleBlock(
		showSubtitle: Boolean,
		phase: ProblemPhase,
		enterReveal: EnterTransition,
		textChangeTransition: AnimatedContentTransitionScope<Boolean>.() -> ContentTransform
) {
	Box(contentAlignment = Alignment.TopCenter) {
		Box(modifier = Modifier.alpha(0f)) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Spacer(modifier = Modifier.height(10.dp))
				Text(
					text = stringResource(R.string.onboarding_problem_subtitle),
					style = typography.bodyMedium,
					modifier = Modifier.fillMaxWidth(0.88f)
				)
			}
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Spacer(modifier = Modifier.height(10.dp))
				Text(
					text = stringResource(R.string.onboarding_problem_better_way_subtitle),
					style = typography.bodyMedium,
					modifier = Modifier.fillMaxWidth(0.88f)
				)
			}
		}
		AnimatedVisibility(
			visible = showSubtitle,
			enter = enterReveal
		) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				AnimatedVisibility(
					visible = phase != ProblemPhase.Crushing
				) {
					Spacer(modifier = Modifier.height(10.dp))
				}
				AnimatedContent(
					targetState = phase == ProblemPhase.Crushing,
					transitionSpec = textChangeTransition,
					label = "subtitle_transition"
				) { isCrushing ->
					Text(
						text = if (isCrushing) {
							stringResource(R.string.onboarding_problem_better_way_subtitle)
						} else {
							stringResource(R.string.onboarding_problem_subtitle)
						},
						style = typography.bodyMedium,
						textAlign = TextAlign.Center,
						color = colorScheme.onBackground.copy(alpha = 0.72f),
						modifier = Modifier.fillMaxWidth(0.88f)
					)
				}
			}
		}
	}
}
