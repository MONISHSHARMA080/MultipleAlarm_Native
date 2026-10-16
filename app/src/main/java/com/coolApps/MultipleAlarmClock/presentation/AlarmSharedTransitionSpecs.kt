package com.coolApps.MultipleAlarmClock.presentation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Rect

/**
 * Single source of truth for the alarm card ↔ edit screen shared-element motion.
 * Used by AlarmCard, EditAlarmScreen and TimeRow so both ends of the transition always agree.
 */

/** Container (card ⇄ screen) — M3 Expressive spatial spring with natural fluid bounce. */
private val AlarmContainerSpring = spring<Rect>(
	dampingRatio = 0.8f,
	stiffness = 380f
)

/**
 * Start time, arrow and end time — expressive spatial spring so digits glide smoothly
 * across the screen with subtle settle.
 */
private val AlarmTimeSpring = spring<Rect>(
	dampingRatio = 0.88f,
	stiffness = 340f
)

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmBoundsTransform = BoundsTransform { _, _ -> AlarmContainerSpring }

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmContainerBoundsTransform = BoundsTransform { _, _ -> AlarmContainerSpring }

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmTimeBoundsTransform = BoundsTransform { _, _ -> AlarmTimeSpring }
