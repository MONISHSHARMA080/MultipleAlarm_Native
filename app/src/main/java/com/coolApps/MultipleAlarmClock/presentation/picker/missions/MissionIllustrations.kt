package com.coolApps.MultipleAlarmClock.presentation.picker.missions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmMission.expressiveBackdropShape(): Shape = when (this) {
	AlarmMission.SHAKE -> MaterialShapes.SoftBurst.toShape()
	AlarmMission.TYPING -> MaterialShapes.Clover4Leaf.toShape()
	AlarmMission.MATH -> MaterialShapes.Cookie6Sided.toShape()
	AlarmMission.ITEM_HUNT -> MaterialShapes.Flower.toShape()
	AlarmMission.STEPS -> MaterialShapes.Sunny.toShape()
	AlarmMission.PUSH_UPS -> MaterialShapes.Cookie4Sided.toShape()
}

@Composable
fun AlarmMission.expressiveAccentContainerColor(): Color = when (this) {
	AlarmMission.SHAKE, AlarmMission.STEPS -> MaterialTheme.colorScheme.primaryContainer
	AlarmMission.TYPING, AlarmMission.ITEM_HUNT -> MaterialTheme.colorScheme.secondaryContainer
	AlarmMission.MATH, AlarmMission.PUSH_UPS -> MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
fun MissionIllustration(
	mission: AlarmMission,
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	when (mission) {
		AlarmMission.SHAKE -> ShakeMissionIllustration(isSelected = isSelected, modifier = modifier)
		AlarmMission.TYPING -> TypingMissionIllustration(isSelected = isSelected, modifier = modifier)
		AlarmMission.MATH -> MathMissionIllustration(isSelected = isSelected, modifier = modifier)
		AlarmMission.ITEM_HUNT -> ItemHuntMissionIllustration(isSelected = isSelected, modifier = modifier)
		AlarmMission.STEPS -> StepsMissionIllustration(isSelected = isSelected, modifier = modifier)
		AlarmMission.PUSH_UPS -> PushUpsMissionIllustration(isSelected = isSelected, modifier = modifier)
	}
}

@Composable
private fun ShakeMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "shake_mission")
	val phase by transition.animateFloat(
		initialValue = 0f,
		targetValue = (2f * PI).toFloat(),
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1500, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "shake_phase"
	)
	val fillProgress by transition.animateFloat(
		initialValue = 0.25f,
		targetValue = 0.95f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "shake_fill"
	)

	val primary = MaterialTheme.colorScheme.primary
	val secondary = MaterialTheme.colorScheme.secondary
	val surface = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerHigh
	val trackColor = MaterialTheme.colorScheme.surfaceVariant
	val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant

	val tilt = sin(phase.toDouble()).toFloat() * 14f
	val shiftX = sin(phase.toDouble()).toFloat() * 4f

	Canvas(modifier = modifier.fillMaxSize()) {
		val cx = size.width / 2f + shiftX.dp.toPx()
		val cy = size.height / 2f
		val phoneW = size.minDimension * 0.42f
		val phoneH = size.minDimension * 0.72f
		val phoneRadius = phoneW * 0.22f

		val waveAlpha = (0.35f + 0.45f * abs(sin(phase.toDouble())).toFloat())
		val arcStroke = 2.6.dp.toPx()

		listOf(-1f, 1f).forEach { dir ->
			val baseX = cx + dir * (phoneW * 0.78f)
			val outerX = baseX + dir * 7.dp.toPx()
			drawArc(
				color = primary.copy(alpha = waveAlpha),
				startAngle = if (dir < 0) 140f else -40f,
				sweepAngle = 80f,
				useCenter = false,
				topLeft = Offset(baseX - 10.dp.toPx(), cy - phoneH * 0.28f),
				size = Size(20.dp.toPx(), phoneH * 0.56f),
				style = Stroke(width = arcStroke, cap = StrokeCap.Round)
			)
			drawArc(
				color = secondary.copy(alpha = waveAlpha * 0.65f),
				startAngle = if (dir < 0) 145f else -35f,
				sweepAngle = 70f,
				useCenter = false,
				topLeft = Offset(outerX - 13.dp.toPx(), cy - phoneH * 0.36f),
				size = Size(26.dp.toPx(), phoneH * 0.72f),
				style = Stroke(width = arcStroke * 0.85f, cap = StrokeCap.Round)
			)
		}

		rotate(degrees = tilt, pivot = Offset(cx, cy)) {
			val left = cx - phoneW / 2f
			val top = cy - phoneH / 2f

			drawRoundRect(
				color = outlineColor.copy(alpha = 0.24f),
				topLeft = Offset(left - 1.5.dp.toPx(), top - 1.5.dp.toPx()),
				size = Size(phoneW + 3.dp.toPx(), phoneH + 3.dp.toPx()),
				cornerRadius = CornerRadius(phoneRadius + 1.5.dp.toPx())
			)
			drawRoundRect(
				color = surface,
				topLeft = Offset(left, top),
				size = Size(phoneW, phoneH),
				cornerRadius = CornerRadius(phoneRadius)
			)
			drawRoundRect(
				color = primary,
				topLeft = Offset(left, top),
				size = Size(phoneW, phoneH),
				cornerRadius = CornerRadius(phoneRadius),
				style = Stroke(width = 2.dp.toPx())
			)

			drawLine(
				color = outlineColor.copy(alpha = 0.45f),
				start = Offset(cx - phoneW * 0.16f, top + phoneH * 0.1f),
				end = Offset(cx + phoneW * 0.16f, top + phoneH * 0.1f),
				strokeWidth = 2.dp.toPx(),
				cap = StrokeCap.Round
			)

			val barW = phoneW * 0.68f
			val barH = phoneH * 0.14f
			val barLeft = cx - barW / 2f
			val barTop = cy - barH / 2f
			drawRoundRect(
				color = trackColor,
				topLeft = Offset(barLeft, barTop),
				size = Size(barW, barH),
				cornerRadius = CornerRadius(barH / 2f)
			)
			drawRoundRect(
				color = primary,
				topLeft = Offset(barLeft, barTop),
				size = Size(barW * fillProgress, barH),
				cornerRadius = CornerRadius(barH / 2f)
			)

			drawCircle(
				color = primary.copy(alpha = 0.4f),
				radius = 2.2.dp.toPx(),
				center = Offset(cx, top + phoneH * 0.84f)
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TypingMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val phrase = "WAKE UP"
	val transition = rememberInfiniteTransition(label = "typing_mission")
	val step by transition.animateFloat(
		initialValue = 1f,
		targetValue = (phrase.length + 2).toFloat(),
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 2200, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "typing_step"
	)
	val cursorAlpha by transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 450),
			repeatMode = RepeatMode.Reverse
		),
		label = "cursor_blink"
	)

	val visibleCount = step.toInt().coerceIn(1, phrase.length)
	val activeKeyIndex = (step.toInt() - 1) % 9

	val pillColor = if (isSelected) {
		MaterialTheme.colorScheme.surfaceContainerLowest
	} else {
		MaterialTheme.colorScheme.surfaceContainerHigh
	}
	val keyColor = MaterialTheme.colorScheme.surfaceContainerHighest
	val activeKeyColor = MaterialTheme.colorScheme.primary
	val textColor = MaterialTheme.colorScheme.onSurface

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 12.dp, vertical = 10.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Surface(
			shape = RoundedCornerShape(12.dp),
			color = pillColor,
			modifier = Modifier.fillMaxWidth(0.88f)
		) {
			Row(
				modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center
			) {
				Text(
					text = phrase.take(visibleCount),
					style = MaterialTheme.typography.labelLargeEmphasized.copy(
						fontFeatureSettings = "tnum"
					),
					color = textColor,
					maxLines = 1
				)
				Spacer(modifier = Modifier.width(2.dp))
				Box(
					modifier = Modifier
						.width(2.dp)
						.height(14.dp)
						.graphicsLayer { alpha = cursorAlpha }
						.background(activeKeyColor, RoundedCornerShape(1.dp))
				)
			}
		}

		Spacer(modifier = Modifier.height(10.dp))

		Canvas(
			modifier = Modifier
				.fillMaxWidth(0.84f)
				.height(34.dp)
		) {
			val rows = 3
			val rowGap = 4.dp.toPx()
			val keyGap = 4.dp.toPx()
			val keyH = (size.height - rowGap * (rows - 1)) / rows
			val rowCounts = listOf(5, 4, 3)

			var keyIdx = 0
			rowCounts.forEachIndexed { r, count ->
				val y = r * (keyH + rowGap)
				val totalW = if (r == 2) size.width * 0.72f else size.width * (1f - r * 0.08f)
				val startX = (size.width - totalW) / 2f
				val keyW = (totalW - keyGap * (count - 1)) / count

				for (c in 0 until count) {
					val x = startX + c * (keyW + keyGap)
					val isHighlighted = (keyIdx % 9) == activeKeyIndex
					drawRoundRect(
						color = if (isHighlighted) activeKeyColor else keyColor,
						topLeft = Offset(x, y),
						size = Size(keyW, keyH),
						cornerRadius = CornerRadius(3.dp.toPx())
					)
					keyIdx++
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MathMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "math_mission")
	val progress by transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "math_progress"
	)
	val showAnswer = progress > 0.55f

	val cardColor = if (isSelected) {
		MaterialTheme.colorScheme.surfaceContainerLowest
	} else {
		MaterialTheme.colorScheme.surfaceContainerHigh
	}
	val primary = MaterialTheme.colorScheme.primary
	val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
	val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
	val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
	val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
	val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Surface(
			shape = CircleShape,
			color = tertiaryContainer,
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(start = 14.dp, top = 10.dp)
				.offset(y = (-progress * 3f).dp)
				.size(22.dp)
		) {
			Box(contentAlignment = Alignment.Center) {
				Text(
					text = "×",
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.ExtraBold,
					color = onTertiaryContainer
				)
			}
		}

		Surface(
			shape = CircleShape,
			color = secondaryContainer,
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.padding(end = 14.dp, bottom = 10.dp)
				.offset(y = (progress * 3f).dp)
				.size(22.dp)
		) {
			Box(contentAlignment = Alignment.Center) {
				Text(
					text = "+",
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.ExtraBold,
					color = onSecondaryContainer
				)
			}
		}

		Surface(
			shape = RoundedCornerShape(16.dp),
			color = cardColor,
			tonalElevation = 2.dp
		) {
			Column(
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = "14 + 27",
					style = MaterialTheme.typography.titleSmall.copy(
						fontWeight = FontWeight.Bold,
						fontFeatureSettings = "tnum"
					),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.height(2.dp))
				AnimatedContent(
					targetState = showAnswer,
					transitionSpec = {
						fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
					},
					label = "math_answer"
				) { answered ->
					Text(
						text = if (answered) "= 41" else "= ?",
						style = MaterialTheme.typography.titleMediumEmphasized.copy(
							fontFeatureSettings = "tnum"
						),
						color = primary
					)
				}
			}
		}
	}
}

@Composable
private fun ItemHuntMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "item_hunt_mission")
	val scanFraction by transition.animateFloat(
		initialValue = 0.18f,
		targetValue = 0.82f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "scan_line"
	)

	val frameColor = MaterialTheme.colorScheme.primary
	val potColor = MaterialTheme.colorScheme.tertiary
	val potRimColor = MaterialTheme.colorScheme.tertiaryContainer
	val leafLight = MaterialTheme.colorScheme.primary
	val leafDark = MaterialTheme.colorScheme.secondary
	val soilColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
	val backdropColor = if (isSelected) {
		MaterialTheme.colorScheme.surfaceContainerLowest
	} else {
		MaterialTheme.colorScheme.surfaceContainerHigh
	}

	Canvas(modifier = modifier.fillMaxSize()) {
		val cx = size.width / 2f
		val cy = size.height / 2f
		val boxSize = size.minDimension * 0.78f
		val left = cx - boxSize / 2f
		val top = cy - boxSize / 2f

		drawRoundRect(
			color = backdropColor,
			topLeft = Offset(left, top),
			size = Size(boxSize, boxSize),
			cornerRadius = CornerRadius(14.dp.toPx())
		)

		val potTopW = boxSize * 0.34f
		val potBottomW = boxSize * 0.24f
		val potH = boxSize * 0.26f
		val potTopY = cy + boxSize * 0.08f
		val potBottomY = potTopY + potH

		val potPath = Path().apply {
			moveTo(cx - potTopW / 2f, potTopY)
			lineTo(cx + potTopW / 2f, potTopY)
			lineTo(cx + potBottomW / 2f, potBottomY)
			lineTo(cx - potBottomW / 2f, potBottomY)
			close()
		}
		drawPath(path = potPath, color = potColor)

		drawRoundRect(
			color = potRimColor,
			topLeft = Offset(cx - potTopW * 0.56f, potTopY - 4.dp.toPx()),
			size = Size(potTopW * 1.12f, 6.dp.toPx()),
			cornerRadius = CornerRadius(3.dp.toPx())
		)

		drawLine(
			color = soilColor,
			start = Offset(cx - potTopW * 0.42f, potTopY - 4.5.dp.toPx()),
			end = Offset(cx + potTopW * 0.42f, potTopY - 4.5.dp.toPx()),
			strokeWidth = 2.dp.toPx(),
			cap = StrokeCap.Round
		)

		val stemBaseY = potTopY - 5.dp.toPx()
		drawLine(
			color = leafDark,
			start = Offset(cx, stemBaseY),
			end = Offset(cx, stemBaseY - boxSize * 0.30f),
			strokeWidth = 2.6.dp.toPx(),
			cap = StrokeCap.Round
		)

		val leftLeaf = Path().apply {
			moveTo(cx - 1.dp.toPx(), stemBaseY - boxSize * 0.08f)
			quadraticTo(
				cx - boxSize * 0.26f,
				stemBaseY - boxSize * 0.14f,
				cx - boxSize * 0.22f,
				stemBaseY - boxSize * 0.28f
			)
			quadraticTo(
				cx - boxSize * 0.06f,
				stemBaseY - boxSize * 0.26f,
				cx - 1.dp.toPx(),
				stemBaseY - boxSize * 0.10f
			)
			close()
		}
		drawPath(path = leftLeaf, color = leafLight)

		val rightLeaf = Path().apply {
			moveTo(cx + 1.dp.toPx(), stemBaseY - boxSize * 0.12f)
			quadraticTo(
				cx + boxSize * 0.26f,
				stemBaseY - boxSize * 0.18f,
				cx + boxSize * 0.21f,
				stemBaseY - boxSize * 0.33f
			)
			quadraticTo(
				cx + boxSize * 0.06f,
				stemBaseY - boxSize * 0.29f,
				cx + 1.dp.toPx(),
				stemBaseY - boxSize * 0.14f
			)
			close()
		}
		drawPath(path = rightLeaf, color = leafDark)

		val topLeaf = Path().apply {
			moveTo(cx, stemBaseY - boxSize * 0.26f)
			quadraticTo(
				cx - boxSize * 0.10f,
				stemBaseY - boxSize * 0.42f,
				cx,
				stemBaseY - boxSize * 0.45f
			)
			quadraticTo(
				cx + boxSize * 0.10f,
				stemBaseY - boxSize * 0.42f,
				cx,
				stemBaseY - boxSize * 0.26f
			)
			close()
		}
		drawPath(path = topLeaf, color = leafLight)

		val scanY = top + boxSize * scanFraction
		drawLine(
			brush = Brush.horizontalGradient(
				colors = listOf(
					Color.Transparent,
					frameColor.copy(alpha = 0.75f),
					Color.Transparent
				),
				startX = left + 4.dp.toPx(),
				endX = left + boxSize - 4.dp.toPx()
			),
			start = Offset(left + 4.dp.toPx(), scanY),
			end = Offset(left + boxSize - 4.dp.toPx(), scanY),
			strokeWidth = 2.dp.toPx(),
			cap = StrokeCap.Round
		)

		val bracketLen = boxSize * 0.2f
		val bracketStroke = 2.6.dp.toPx()
		val inset = 2.dp.toPx()
		val r = left + boxSize - inset
		val b = top + boxSize - inset
		val l = left + inset
		val t = top + inset

		val corners = listOf(
			Pair(Offset(l, t + bracketLen), Offset(l, t)) to Offset(l + bracketLen, t),
			Pair(Offset(r - bracketLen, t), Offset(r, t)) to Offset(r, t + bracketLen),
			Pair(Offset(l, b - bracketLen), Offset(l, b)) to Offset(l + bracketLen, b),
			Pair(Offset(r - bracketLen, b), Offset(r, b)) to Offset(r, b - bracketLen)
		)
		corners.forEach { (seg1, endPt) ->
			val cornerPath = Path().apply {
				moveTo(seg1.first.x, seg1.first.y)
				lineTo(seg1.second.x, seg1.second.y)
				lineTo(endPt.x, endPt.y)
			}
			drawPath(
				path = cornerPath,
				color = frameColor,
				style = Stroke(width = bracketStroke, cap = StrokeCap.Round)
			)
		}
	}
}

@Composable
private fun StepsMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "steps_mission")
	val stepPhase by transition.animateFloat(
		initialValue = 0f,
		targetValue = 2f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1400, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "steps_phase"
	)
	val leftActive = stepPhase < 1f
	val pulse = stepPhase % 1f

	val activeColor = MaterialTheme.colorScheme.primary
	val idleColor = if (isSelected) {
		MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f)
	} else {
		MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
	}

	Canvas(modifier = modifier.fillMaxSize()) {
		val cx = size.width / 2f
		val cy = size.height / 2f

		fun drawFootprint(center: Offset, angleDeg: Float, color: Color, showRipple: Boolean) {
			if (showRipple) {
				drawCircle(
					color = activeColor.copy(alpha = (1f - pulse) * 0.35f),
					radius = 12.dp.toPx() + pulse * 14.dp.toPx(),
					center = center,
					style = Stroke(width = 2.dp.toPx())
				)
			}
			rotate(degrees = angleDeg, pivot = center) {
				drawOval(
					color = color,
					topLeft = Offset(center.x - 8.dp.toPx(), center.y - 14.dp.toPx()),
					size = Size(16.dp.toPx(), 18.dp.toPx())
				)
				drawOval(
					color = color,
					topLeft = Offset(center.x - 6.dp.toPx(), center.y + 7.dp.toPx()),
					size = Size(12.dp.toPx(), 10.dp.toPx())
				)
			}
		}

		drawFootprint(
			center = Offset(cx - 16.dp.toPx(), cy + 6.dp.toPx()),
			angleDeg = -10f,
			color = if (leftActive) activeColor else idleColor,
			showRipple = leftActive
		)
		drawFootprint(
			center = Offset(cx + 16.dp.toPx(), cy - 6.dp.toPx()),
			angleDeg = 10f,
			color = if (!leftActive) activeColor else idleColor,
			showRipple = !leftActive
		)
	}
}

@Composable
private fun PushUpsMissionIllustration(
	isSelected: Boolean,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition(label = "push_ups_mission")
	val bend by transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "push_up_bend"
	)

	val bodyColor = MaterialTheme.colorScheme.primary
	val armColor = MaterialTheme.colorScheme.secondary
	val matColor = if (isSelected) {
		MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
	} else {
		MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
	}

	Canvas(modifier = modifier.fillMaxSize()) {
		val cx = size.width / 2f
		val floorY = size.height * 0.72f
		val span = size.minDimension * 0.66f
		val matHalf = span * 0.62f

		// Ground mat
		drawLine(
			color = matColor,
			start = Offset(cx - matHalf, floorY + 2.dp.toPx()),
			end = Offset(cx + matHalf, floorY + 2.dp.toPx()),
			strokeWidth = 3.dp.toPx(),
			cap = StrokeCap.Round
		)

		val toes = Offset(cx - span * 0.48f, floorY)
		val hand = Offset(cx + span * 0.24f, floorY)
		val shoulderHeight = (24.dp.toPx()) * (1f - 0.56f * bend)
		val shoulder = Offset(cx + span * 0.20f, floorY - shoulderHeight)

		// Elbow bends backward as torso lowers
		val elbow = Offset(
			x = hand.x - 10.dp.toPx() * bend - 2.dp.toPx(),
			y = (hand.y + shoulder.y) / 2f
		)

		// Head aligned past the shoulders along the plank angle
		val bodyDx = shoulder.x - toes.x
		val bodyDy = shoulder.y - toes.y
		val bodyLen = kotlin.math.hypot(bodyDx.toDouble(), bodyDy.toDouble()).toFloat().coerceAtLeast(1f)
		val ux = bodyDx / bodyLen
		val uy = bodyDy / bodyLen
		val headCenter = Offset(
			x = shoulder.x + ux * 10.dp.toPx(),
			y = shoulder.y + uy * 10.dp.toPx() - 2.dp.toPx()
		)

		// Effort chevrons near the top of the rep
		val sparkAlpha = (1f - bend).coerceIn(0f, 1f) * 0.7f
		if (sparkAlpha > 0.08f) {
			val sparkTop = shoulder.y - 12.dp.toPx() - (1f - bend) * 3.dp.toPx()
			val chevron = Path().apply {
				moveTo(shoulder.x - 6.dp.toPx(), sparkTop + 4.dp.toPx())
				lineTo(shoulder.x, sparkTop)
				lineTo(shoulder.x + 6.dp.toPx(), sparkTop + 4.dp.toPx())
			}
			drawPath(
				path = chevron,
				color = bodyColor.copy(alpha = sparkAlpha),
				style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
			)
		}

		// Articulated arm
		drawLine(
			color = armColor,
			start = hand,
			end = elbow,
			strokeWidth = 3.4.dp.toPx(),
			cap = StrokeCap.Round
		)
		drawLine(
			color = armColor,
			start = elbow,
			end = shoulder,
			strokeWidth = 3.4.dp.toPx(),
			cap = StrokeCap.Round
		)

		// Plank torso & legs
		drawLine(
			color = bodyColor,
			start = toes,
			end = shoulder,
			strokeWidth = 4.6.dp.toPx(),
			cap = StrokeCap.Round
		)

		// Head
		drawCircle(
			color = bodyColor,
			radius = 6.dp.toPx(),
			center = headCenter
		)
	}
}
