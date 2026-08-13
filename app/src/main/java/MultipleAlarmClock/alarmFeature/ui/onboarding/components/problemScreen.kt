package MultipleAlarmClock.alarmFeature.ui.onboarding.components


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class ProblemPhase {
	Building,
	ShowingProblem,
	Crushing
}

@Composable
fun ProblemScreen(
	onComplete: () -> Unit
) {
	var phase by remember { mutableStateOf(ProblemPhase.Building) }
	var visibleAlarms by remember { mutableIntStateOf(0) }

	val alarms = remember {
		listOf("7:00", "7:10", "7:20", "7:30", "7:40")
	}

	// Single source of truth for card sizing — everything else derives from these.
	val cardHeight = 64.dp
	val cardGap = 12.dp
	val cardSpacing = cardHeight + cardGap
	// Exact vertical room the stack needs, so the container never clips
	// or forces overlap into the text below, regardless of alarm count.
	val animationHeight = cardHeight + cardSpacing * (alarms.size - 1)

	LaunchedEffect(Unit) {
		alarms.indices.forEach { index ->
			delay(160.milliseconds)
			visibleAlarms = index + 1
		}
		delay(400.milliseconds)
		phase = ProblemPhase.ShowingProblem
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.weight(0.8f))

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

		Spacer(modifier = Modifier.height(34.dp)) // was 52dp — animationHeight already sized correctly now

		Text(
			text = if (phase == ProblemPhase.Crushing) {
				"Better way"
			} else {
				"One task. So many alarms."
			},
			style = typography.headlineLarge,
			textAlign = TextAlign.Center,
			color = colorScheme.onBackground
		)

		Spacer(modifier = Modifier.height(10.dp))

		Text(
			text = if (phase == ProblemPhase.Crushing) {
				"Select the time interval and how frequent you want the alarm and app handles the rest"
			} else {
				"Managing multiple alarms for a task takes time and effort."
			},
			style = typography.bodyMedium,
			textAlign = TextAlign.Center,
			color = colorScheme.onBackground.copy(alpha = 0.72f),
			modifier = Modifier.fillMaxWidth(0.88f)
		)

		Spacer(modifier = Modifier.weight(1f))

		Button(
			onClick = {
				when (phase) {
					ProblemPhase.Crushing -> onComplete()
					else -> phase = ProblemPhase.Crushing
				}
			},
			enabled = phase != ProblemPhase.Building,
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
				text = if (phase == ProblemPhase.Crushing) "Set my own" else "Fix this",
				style = typography.titleMedium
			)
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
				dampingRatio = Spring.DampingRatioNoBouncy,
				stiffness = Spring.StiffnessMediumLow
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
				dampingRatio = Spring.DampingRatioNoBouncy,
				stiffness = Spring.StiffnessMediumLow
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
				dampingRatio = Spring.DampingRatioNoBouncy,
				stiffness = Spring.StiffnessMediumLow
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
			tween(
				durationMillis = 450,
				easing = FastOutSlowInEasing
			)
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
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessLow
		),
		label = "entrance"
	)

	val restingY = normalDistance * (1f - crushProgress)
	val enterOffsetY = (1f - entrance) * 90f
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
		shape = shapes.large,
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
					style = typography.titleLarge,
					color = colorScheme.onSurface
				)
				Text(
					text = "Wake up",
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
		shape = shapes.extraLarge,
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
				shape = shapes.large,
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
					text = "7:00 → 7:40",
					style = typography.titleLarge,
					color = colorScheme.onPrimaryContainer
				)
				Spacer(modifier = Modifier.height(3.dp))
				Text(
					text = "every 10 minutes",
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
