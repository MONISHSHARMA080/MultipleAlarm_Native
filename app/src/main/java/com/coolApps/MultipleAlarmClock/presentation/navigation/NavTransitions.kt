package com.coolApps.MultipleAlarmClock.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Shared-axis (X) style transitions. The slide covers only a fraction of the width so it scales
 * with any window size, and the fades are staggered so the two screens never overlap at ~50% alpha.
 */
object NavTransitions {
	private const val SLIDE_DURATION_MS = 300
	private const val POP_SLIDE_DURATION_MS = 250
	private const val FADE_IN_DURATION_MS = 210
	private const val FADE_IN_DELAY_MS = 90
	private const val FADE_OUT_DURATION_MS = 90
	private const val SLIDE_FRACTION = 4 // slide distance = width / SLIDE_FRACTION

	/** Forward: new screen enters from the end, old screen leaves toward the start. */
	fun forward(): ContentTransform = sharedAxis(
		enterFromEnd = true,
		slideDurationMs = SLIDE_DURATION_MS
	)

	/** Back / predictive-back release: previous screen enters from the start, current leaves toward the end. */
	fun pop(): ContentTransform = sharedAxis(
		enterFromEnd = false,
		slideDurationMs = POP_SLIDE_DURATION_MS
	)

	private fun sharedAxis(enterFromEnd: Boolean, slideDurationMs: Int): ContentTransform {
		val direction = if (enterFromEnd) 1 else -1
		val slideSpec = tween<IntOffset>(slideDurationMs, easing = FastOutSlowInEasing)
		return slideInHorizontally(
			animationSpec = slideSpec,
			initialOffsetX = { direction * it / SLIDE_FRACTION }
		) + fadeIn(
			tween(FADE_IN_DURATION_MS, delayMillis = FADE_IN_DELAY_MS, easing = LinearEasing)
		) togetherWith slideOutHorizontally(
			animationSpec = slideSpec,
			targetOffsetX = { -direction * it / SLIDE_FRACTION }
		) + fadeOut(
			tween(FADE_OUT_DURATION_MS, easing = LinearEasing)
		)
	}
}
