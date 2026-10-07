package com.coolApps.MultipleAlarmClock.presentation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Rect

/**
 * Single source of truth for the alarm card ↔ edit screen shared-element motion.
 * Used by AlarmCard, EditAlarmScreen and TimeRow so both ends of the transition always agree.
 */

/** Container (card ⇄ screen) — gentle, slightly softened from before with a hint of bounce. */
private val AlarmContainerSpring = spring<Rect>(
	dampingRatio = Spring.DampingRatioLowBouncy,
	stiffness = 330f
)

/**
 * Start time, arrow and end time. Softer and critically damped-ish so the digits glide
 * (readable throughout) instead of snapping across the screen, and never overshoot.
 */
private val AlarmTimeSpring = spring<Rect>(
	dampingRatio = 0.9f,
	stiffness = 190f
)

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmContainerBoundsTransform = BoundsTransform { _, _ -> AlarmContainerSpring }

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmTimeBoundsTransform = BoundsTransform { _, _ -> AlarmTimeSpring }
