package MultipleAlarmClock.alarmFeature.ui.onboarding


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
	onContinue: () -> Unit = {}
) {
	var phase by remember {
		mutableStateOf(ProblemPhase.Building)
	}

	LaunchedEffect(Unit) {
		while (true) {
			phase = ProblemPhase.Building
			delay(2_400.milliseconds)

			phase = ProblemPhase.ShowingProblem
			delay(900.milliseconds)

			// The entire transformation happens from here.
			phase = ProblemPhase.Crushing

			// Hold the completed result briefly.
			delay(2_500.milliseconds)
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.navigationBarsPadding()
			.padding(horizontal = 24.dp, vertical = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {

		Spacer(
			modifier = Modifier.weight(0.8f)
		)

		ProblemAnimation(
			phase = phase,
			modifier = Modifier
				.fillMaxWidth()
				.height(340.dp)
		)

		Spacer(modifier = Modifier.height(52.dp))

		Text(
			text = if (phase == ProblemPhase.Crushing) {
				"We can simplify that."
			} else {
				"One task. So many alarms."
			},
			style = typography.headlineMedium,
			textAlign = TextAlign.Center,
			color = colorScheme.onBackground
		)

		Spacer(modifier = Modifier.height(12.dp))

		Text(
			text = if (phase == ProblemPhase.Crushing) {
				"Set the range and interval once."
			} else {
				"Setting each alarm separately takes time."
			},
			style = typography.bodyLarge,
			textAlign = TextAlign.Center,
			color = colorScheme.onBackground.copy(alpha = 0.72f),
			modifier = Modifier.fillMaxWidth(0.88f)
		)

		Spacer(
			modifier = Modifier.weight(1f)
		)

		Button(
			onClick = onContinue,
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp),
			shape = shapes.extraLarge,
			colors = ButtonDefaults.buttonColors(
				containerColor = colorScheme.primaryContainer,
				contentColor = colorScheme.onPrimaryContainer
			)
		) {
			Text("Fix this")
		}
	}
}
@Composable
private fun ProblemAnimation(
	phase: ProblemPhase,
	modifier: Modifier = Modifier
) {
	val alarms = remember {
		listOf(
			"7:00",
			"7:10",
			"7:20",
			"7:30",
			"7:40",
			"7:50",
			"8:00"
		)
	}

	val transition = updateTransition(
		targetState = phase,
		label = "alarm transformation"
	)

	/*
	 * How far the stack has collapsed.
	 *
	 * 0f = normal alarms
	 * 1f = completely transformed into one alarm
	 */
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

	/*
	 * The final card grows DIRECTLY out of the crushing
	 * stack.
	 */
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

		/*
		 * ─────────────────────────────────────
		 * THE ORIGINAL ALARMS
		 * ─────────────────────────────────────
		 */

		alarms.forEachIndexed { index, time ->

			CrushingAlarm(
				time = time,
				index = index,
				total = alarms.size,
				crushProgress = crushProgress
			)
		}

		/*
		 * ─────────────────────────────────────
		 * THE DESTINATION
		 * ─────────────────────────────────────
		 *
		 * This is NOT shown after the animation.
		 *
		 * It exists underneath the alarms and gradually
		 * becomes the thing the alarms are collapsing into.
		 */
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
	crushProgress: Float
) {
	val center = (total - 1) / 2f

	/*
	 * Normal distance from the center.
	 */
	val normalDistance = (index - center) * 62f

	/*
	 * Every card moves toward the exact center.
	 */
	val y = normalDistance * (1f - crushProgress)

	/*
	 * The cards become progressively shorter.
	 */
	val height = 58f * (1f - crushProgress)

	/*
	 * The cards compress horizontally toward the destination.
	 */
	val horizontalScale =
		1f - (crushProgress * 0.08f)

	/*
	 * Outer cards rotate slightly toward the center.
	 *
	 * This makes the stack feel physical rather than
	 * simply scaling down.
	 */
	val rotation =
		(index - center) * -1.2f * crushProgress

	/*
	 * Don't make them disappear immediately.
	 *
	 * They remain visible almost until they have merged
	 * with the destination.
	 */
	val alpha = 1f - (crushProgress * 0.55f)

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
		color = colorScheme.surfaceContainerHigh
	) {

		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 20.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {

			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {

				Icon(
					imageVector = Icons.Outlined.Alarm,
					contentDescription = null,
					modifier = Modifier.size(21.dp),
					tint = colorScheme.primary
				)

				Text(
					text = time,
					style = typography.titleMedium,
					color = colorScheme.onSurface
				)
			}

			Text(
				text = "Wake up",
				style = typography.bodyMedium,
				color = colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun ModifiedAlarm(
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
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {

			Surface(
				modifier = Modifier.size(48.dp),
				shape = shapes.large,
				color = colorScheme.primary.copy(
					alpha = 0.12f
				)
			) {
				Box(
					contentAlignment = Alignment.Center
				) {
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
					text = "7:00 → 8:00",
					style = typography.titleLarge,
					color = colorScheme.onPrimaryContainer
				)

				Spacer(modifier = Modifier.height(3.dp))

				Text(
					text = "every 10 minutes",
					style = typography.bodyMedium,
					color = colorScheme.onPrimaryContainer.copy(
						alpha = 0.72f
					)
				)
			}
		}
	}
}