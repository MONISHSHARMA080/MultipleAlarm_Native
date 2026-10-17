package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset

/**
 * M3 Expressive shared-axis (X) transitions using physics-based spatial springs for slide/scale
 * and critically-damped effects springs for fade.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object NavTransitions {
	private const val SLIDE_FRACTION = 4 // slide distance = width / SLIDE_FRACTION

	private val spatialIntOffsetSpec = MotionScheme.expressive().defaultSpatialSpec<IntOffset>()
	private val spatialFloatSpec = MotionScheme.expressive().defaultSpatialSpec<Float>()
	private val effectsSpec = MotionScheme.expressive().defaultEffectsSpec<Float>()

	/** Forward: new screen enters from the end, old screen leaves toward the start. */
	fun forward(): ContentTransform = sharedAxis(enterFromEnd = true)

	/** Back / predictive-back release: previous screen enters from the start, current leaves toward the end. */
	fun pop(): ContentTransform = sharedAxis(enterFromEnd = false)

	private fun sharedAxis(enterFromEnd: Boolean): ContentTransform {
		val direction = if (enterFromEnd) 1 else -1

		return (slideInHorizontally(
			animationSpec = spatialIntOffsetSpec,
			initialOffsetX = { direction * it / SLIDE_FRACTION }
		) + scaleIn(
			initialScale = 0.96f,
			animationSpec = spatialFloatSpec
		) + fadeIn(
			animationSpec = effectsSpec
		)) togetherWith (slideOutHorizontally(
			animationSpec = spatialIntOffsetSpec,
			targetOffsetX = { -direction * it / SLIDE_FRACTION }
		) + scaleOut(
			targetScale = 0.96f,
			animationSpec = spatialFloatSpec
		) + fadeOut(
			animationSpec = effectsSpec
		))
	}
}
