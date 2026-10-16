package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * M3 Expressive shared-axis (X) transitions using physics-based spatial springs for slide/scale
 * and critically-damped effects springs for fade.
 */
object NavTransitions {
	private const val SLIDE_FRACTION = 4 // slide distance = width / SLIDE_FRACTION

	/** Forward: new screen enters from the end, old screen leaves toward the start. */
	fun forward(): ContentTransform = sharedAxis(enterFromEnd = true)

	/** Back / predictive-back release: previous screen enters from the start, current leaves toward the end. */
	fun pop(): ContentTransform = sharedAxis(enterFromEnd = false)

	private fun sharedAxis(enterFromEnd: Boolean): ContentTransform {
		val direction = if (enterFromEnd) 1 else -1
		val spatialIntOffsetSpring = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 400f)
		val spatialFloatSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 400f)
		val effectsSpring = spring<Float>(dampingRatio = 1.0f, stiffness = 1200f)

		return (slideInHorizontally(
			animationSpec = spatialIntOffsetSpring,
			initialOffsetX = { direction * it / SLIDE_FRACTION }
		) + scaleIn(
			initialScale = 0.96f,
			animationSpec = spatialFloatSpring
		) + fadeIn(
			animationSpec = effectsSpring
		)) togetherWith (slideOutHorizontally(
			animationSpec = spatialIntOffsetSpring,
			targetOffsetX = { -direction * it / SLIDE_FRACTION }
		) + scaleOut(
			targetScale = 0.96f,
			animationSpec = spatialFloatSpring
		) + fadeOut(
			animationSpec = effectsSpring
		))
	}
}
