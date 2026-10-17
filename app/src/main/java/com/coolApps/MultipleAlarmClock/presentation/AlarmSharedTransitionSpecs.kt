package com.coolApps.MultipleAlarmClock.presentation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.geometry.Rect

/**
 * Single source of truth for the alarm card ↔ edit screen shared-element motion.
 * Used by AlarmCard, EditAlarmScreen and TimeRow so both ends of the transition always agree.
 */

/** Container (card ⇄ screen) — M3 Expressive spatial spring with natural fluid bounce. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AlarmContainerSpatialSpec = MotionScheme.expressive().defaultSpatialSpec<Rect>()

/**
 * Start time, arrow and end time — expressive spatial spring so digits glide smoothly
 * across the screen with subtle settle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AlarmTimeSpatialSpec = MotionScheme.expressive().defaultSpatialSpec<Rect>()

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmBoundsTransform = BoundsTransform { _, _ -> AlarmContainerSpatialSpec }

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmContainerBoundsTransform = BoundsTransform { _, _ -> AlarmContainerSpatialSpec }

@OptIn(ExperimentalSharedTransitionApi::class)
val AlarmTimeBoundsTransform = BoundsTransform { _, _ -> AlarmTimeSpatialSpec }
