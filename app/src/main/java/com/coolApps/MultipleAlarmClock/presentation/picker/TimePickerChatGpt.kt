//package com.coolApps.MultipleAlarmClock
//
//import android.R.attr.onClick
//import androidx.collection.IntList
//import androidx.compose.animation.Crossfade
//import androidx.compose.animation.core.AnimationSpec
//import androidx.compose.animation.core.SnapSpec
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.MutatePriority
//import androidx.compose.foundation.MutatorMutex
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.RowScope
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.CornerBasedShape
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.LocalContentColor
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TimePickerColors
//import androidx.compose.material3.TimePickerDefaults
//import androidx.compose.material3.TimePickerLayoutType
//import androidx.compose.material3.TimePickerSelectionMode
//import androidx.compose.material3.TimePickerState
//import androidx.compose.material3.isPm
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.input.pointer.PointerEvent
//import androidx.compose.ui.input.pointer.PointerEventPass
//import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
//import androidx.compose.ui.layout.MeasurePolicy
//import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
//import androidx.compose.ui.node.DelegatingNode
//import androidx.compose.ui.node.LayoutAwareModifierNode
//import androidx.compose.ui.node.ModifierNodeElement
//import androidx.compose.ui.node.PointerInputModifierNode
//import androidx.compose.ui.node.Ref
//import androidx.compose.ui.node.requireDensity
//import androidx.compose.ui.platform.InspectorInfo
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.unit.center
//import androidx.compose.ui.unit.dp
//import com.google.android.material.math.MathUtils.dist
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.launch
//
//internal class RestrictedAnalogTimePickerState @OptIn(ExperimentalMaterial3Api::class) constructor(
//		val state: TimePickerState,
//		val restriction: TimePickerRestriction,
//		val userOverride: Ref<Boolean> = Ref<Boolean>(),
//) : TimePickerState by state {
//
//	var currentDiameter by mutableStateOf(0.dp)
//
//	val currentAngle: Float
//		get() = anim.value
//
//	private var hourAngle =
//		RadiansPerHour * (state.hour % 12) - FullCircle / 4
//
//	private var minuteAngle =
//		RadiansPerMinute * state.minute - FullCircle / 4
//
//	private var anim = Animatable(hourAngle)
//
//	private val mutex = MutatorMutex()
//
//	val clockFaceValues: IntList
//		get() =
//			if (selection == TimePickerSelectionMode.Minute) {
//				Minutes
//			} else {
//				Hours
//			}
//
//	suspend fun animateToCurrent(
//			animationSpec: AnimationSpec<Float>,
//	) {
//		if (!isUpdated()) {
//			return
//		}
//
//		val end =
//			if (selection == TimePickerSelectionMode.Hour) {
//				endValueForAnimation(hourAngle)
//			} else {
//				endValueForAnimation(minuteAngle)
//			}
//
//		mutex.mutate(priority = MutatePriority.PreventUserInput) {
//			anim.animateTo(end, animationSpec)
//		}
//	}
//
//	private fun isUpdated(): Boolean {
//		if (
//			selection == TimePickerSelectionMode.Hour &&
//			anim.targetValue.normalize() == hourAngle.normalize()
//		) {
//			return false
//		}
//
//		if (
//			selection == TimePickerSelectionMode.Minute &&
//			anim.targetValue.normalize() == minuteAngle.normalize()
//		) {
//			return false
//		}
//
//		return true
//	}
//
//	private fun endValueForAnimation(
//			new: Float,
//	): Float {
//		var diff = anim.value - new
//
//		while (diff > HalfCircle) {
//			diff -= FullCircle
//		}
//
//		while (diff <= -HalfCircle) {
//			diff += FullCircle
//		}
//
//		return anim.value - diff
//	}
//
//	suspend fun onGestureEnd(
//			animationSpec: AnimationSpec<Float>,
//	) {
//		val end =
//			endValueForAnimation(
//				if (selection == TimePickerSelectionMode.Hour) {
//					hourAngle
//				} else {
//					minuteAngle
//				}
//			)
//
//		mutex.mutate(priority = MutatePriority.PreventUserInput) {
//			anim.animateTo(end, animationSpec)
//		}
//	}
//
//	/**
//	 * Attempts to select [hour]/[minute].
//	 *
//	 * Returns false if the requested time is disabled.
//	 */
//	fun trySetTime(
//			hour: Int,
//			minute: Int,
//	): Boolean {
//		if (!state.isTimeAllowed(hour, minute, restriction)) {
//			state.notifyInvalidTime(
//				hour = hour,
//				minute = minute,
//				restriction = restriction,
//			)
//			return false
//		}
//
//		state.hour = hour
//		state.minute = minute
//
//		hourAngle =
//			RadiansPerHour * (hour % 12) - FullCircle / 4
//
//		minuteAngle =
//			RadiansPerMinute * minute - FullCircle / 4
//
//		if (selection == TimePickerSelectionMode.Hour) {
//			anim = Animatable(hourAngle)
//		} else {
//			anim = Animatable(minuteAngle)
//		}
//
//		return true
//	}
//
//	suspend fun rotateTo(
//			angle: Float,
//			animationSpec: AnimationSpec<Float>,
//			animate: Boolean = false,
//	) {
//		userOverride.value = false
//
//		mutex.mutate(MutatePriority.UserInput) {
//			if (selection == TimePickerSelectionMode.Hour) {
//				val selectedDisplayHour =
//					angle.toHour() % 12
//
//				val selectedHour =
//					if (is24hour) {
//						if (isPm) {
//							selectedDisplayHour + 12
//						} else {
//							selectedDisplayHour
//						}
//					} else {
//						if (isPm) {
//							if (selectedDisplayHour == 0) {
//								12
//							} else {
//								selectedDisplayHour + 12
//							}
//						} else {
//							if (selectedDisplayHour == 12) {
//								0
//							} else {
//								selectedDisplayHour
//							}
//						}
//					}
//
//				val candidateMinute = state.minute
//
//				if (
//					!state.isTimeAllowed(
//						hour = selectedHour,
//						minute = candidateMinute,
//						restriction = restriction,
//					)
//				) {
//					state.notifyInvalidTime(
//						hour = selectedHour,
//						minute = candidateMinute,
//						restriction = restriction,
//					)
//					return@mutate
//				}
//
//				hourAngle =
//					RadiansPerHour *
//							selectedDisplayHour -
//							FullCircle / 4
//
//				state.hour = selectedHour
//
//				if (!animate) {
//					anim.snapTo(offsetAngle(angle))
//				} else {
//					val endAngle =
//						endValueForAnimation(offsetAngle(angle))
//
//					anim.animateTo(endAngle, animationSpec)
//				}
//			} else {
//				val selectedMinute =
//					angle.toMinute()
//
//				val candidateHour =
//					state.hour
//
//				if (
//					!state.isTimeAllowed(
//						hour = candidateHour,
//						minute = selectedMinute,
//						restriction = restriction,
//					)
//				) {
//					state.notifyInvalidTime(
//						hour = candidateHour,
//						minute = selectedMinute,
//						restriction = restriction,
//					)
//					return@mutate
//				}
//
//				minuteAngle =
//					RadiansPerMinute *
//							selectedMinute -
//							FullCircle / 4
//
//				state.minute = selectedMinute
//
//				if (!animate) {
//					anim.snapTo(offsetAngle(angle))
//				} else {
//					val endAngle =
//						endValueForAnimation(offsetAngle(angle))
//
//					anim.animateTo(endAngle, animationSpec)
//				}
//			}
//		}
//	}
//
//	override var minute: Int
//		get() = state.minute
//
//		set(value) {
//			val normalized =
//				value.coerceIn(0, 59)
//
//			val currentHour = state.hour
//
//			if (
//				!state.isTimeAllowed(
//					hour = currentHour,
//					minute = normalized,
//					restriction = restriction,
//				)
//			) {
//				state.notifyInvalidTime(
//					hour = currentHour,
//					minute = normalized,
//					restriction = restriction,
//				)
//				return
//			}
//
//			minuteAngle =
//				RadiansPerMinute * normalized -
//						FullCircle / 4
//
//			state.minute = normalized
//
//			if (selection == TimePickerSelectionMode.Minute) {
//				anim = Animatable(minuteAngle)
//			}
//		}
//
//	override var hour: Int
//		get() = state.hour
//
//		set(value) {
//			val normalized =
//				value.coerceIn(0, 23)
//
//			val currentMinute =
//				state.minute
//
//			if (
//				!state.isTimeAllowed(
//					hour = normalized,
//					minute = currentMinute,
//					restriction = restriction,
//				)
//			) {
//				state.notifyInvalidTime(
//					hour = normalized,
//					minute = currentMinute,
//					restriction = restriction,
//				)
//				return
//			}
//
//			hourAngle =
//				RadiansPerHour *
//						(normalized % 12) -
//						FullCircle / 4
//
//			state.hour = normalized
//
//			if (selection == TimePickerSelectionMode.Hour) {
//				anim = Animatable(hourAngle)
//			}
//		}
//
//	private fun Float.normalize(): Float {
//		var normalizedAngle =
//			this % FullCircle
//
//		if (normalizedAngle < 0) {
//			normalizedAngle += FullCircle
//		}
//
//		return normalizedAngle
//	}
//
//	private fun Float.toHour(): Int {
//		val hourOffset =
//			RadiansPerHour / 2
//
//		val totalOffset =
//			hourOffset + QuarterCircle
//
//		return (
//				(this + totalOffset) /
//						RadiansPerHour
//				).toInt() % 12
//	}
//
//	private fun Float.toMinute(): Int {
//		val minuteOffset =
//			RadiansPerMinute / 2
//
//		val totalOffset =
//			minuteOffset + QuarterCircle
//
//		return (
//				(this + totalOffset) /
//						RadiansPerMinute
//				).toInt() % 60
//	}
//
//	private fun offsetAngle(
//			angle: Float,
//	): Float {
//		val ret =
//			angle + QuarterCircle.toFloat()
//
//		return if (ret < 0) {
//			ret + FullCircle
//		} else {
//			ret
//		}
//	}
//}
//
//
//@Composable
//@ExperimentalMaterial3Api
//fun RestrictedTimePicker(
//		state: TimePickerState,
//		restriction: TimePickerRestriction = TimePickerRestriction(),
//		modifier: Modifier = Modifier,
//		colors: TimePickerColors = TimePickerDefaults.colors(),
//		layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType(),
//) {
//	val a11yServicesEnabled by rememberAccessibilityServiceState()
//	val userOverride = remember { Ref<Boolean>() }
//
//	val analogState =
//		remember(state, restriction) {
//			RestrictedAnalogTimePickerState(
//				state = state,
//				restriction = restriction,
//				userOverride = userOverride,
//			)
//		}
//
//	LaunchedEffect(state.hour, state.minute) {
//		if (userOverride.value == true) {
//			analogState.hour = state.hour
//			analogState.minute = state.minute
//		}
//
//		userOverride.value = true
//	}
//
//	if (layoutType == TimePickerLayoutType.Vertical) {
//		RestrictedVerticalTimePicker(
//			state = analogState,
//			modifier = modifier,
//			colors = colors,
//			autoSwitchToMinute = !a11yServicesEnabled,
//		)
//	} else {
//		RestrictedHorizontalTimePicker(
//			state = analogState,
//			modifier = modifier,
//			colors = colors,
//			autoSwitchToMinute = !a11yServicesEnabled,
//		)
//	}
//}
//
//
//internal class RestrictedAnalogTimePickerState(
//		val state: TimePickerState,
//		val restriction: TimePickerRestriction,
//		val userOverride: Ref<Boolean> = Ref<Boolean>(),
//) : TimePickerState by state {
//
//	var currentDiameter by mutableStateOf(0.dp)
//
//	val currentAngle: Float
//		get() = anim.value
//
//	private var hourAngle =
//		RadiansPerHour * (state.hour % 12) - FullCircle / 4
//
//	private var minuteAngle =
//		RadiansPerMinute * state.minute - FullCircle / 4
//
//	private var anim = Animatable(hourAngle)
//
//	private val mutex = MutatorMutex()
//
//	val clockFaceValues: IntList
//		get() =
//			if (selection == TimePickerSelectionMode.Minute) {
//				Minutes
//			} else {
//				Hours
//			}
//
//	suspend fun animateToCurrent(
//			animationSpec: AnimationSpec<Float>,
//	) {
//		if (!isUpdated()) {
//			return
//		}
//
//		val end =
//			if (selection == TimePickerSelectionMode.Hour) {
//				endValueForAnimation(hourAngle)
//			} else {
//				endValueForAnimation(minuteAngle)
//			}
//
//		mutex.mutate(priority = MutatePriority.PreventUserInput) {
//			anim.animateTo(end, animationSpec)
//		}
//	}
//
//	private fun isUpdated(): Boolean {
//		if (
//			selection == TimePickerSelectionMode.Hour &&
//			anim.targetValue.normalize() == hourAngle.normalize()
//		) {
//			return false
//		}
//
//		if (
//			selection == TimePickerSelectionMode.Minute &&
//			anim.targetValue.normalize() == minuteAngle.normalize()
//		) {
//			return false
//		}
//
//		return true
//	}
//
//	private fun endValueForAnimation(
//			new: Float,
//	): Float {
//		var diff = anim.value - new
//
//		while (diff > HalfCircle) {
//			diff -= FullCircle
//		}
//
//		while (diff <= -HalfCircle) {
//			diff += FullCircle
//		}
//
//		return anim.value - diff
//	}
//
//	suspend fun onGestureEnd(
//			animationSpec: AnimationSpec<Float>,
//	) {
//		val end =
//			endValueForAnimation(
//				if (selection == TimePickerSelectionMode.Hour) {
//					hourAngle
//				} else {
//					minuteAngle
//				}
//			)
//
//		mutex.mutate(priority = MutatePriority.PreventUserInput) {
//			anim.animateTo(end, animationSpec)
//		}
//	}
//
//	/**
//	 * Attempts to select [hour]/[minute].
//	 *
//	 * Returns false if the requested time is disabled.
//	 */
//	fun trySetTime(
//			hour: Int,
//			minute: Int,
//	): Boolean {
//		if (!state.isTimeAllowed(hour, minute, restriction)) {
//			state.notifyInvalidTime(
//				hour = hour,
//				minute = minute,
//				restriction = restriction,
//			)
//			return false
//		}
//
//		state.hour = hour
//		state.minute = minute
//
//		hourAngle =
//			RadiansPerHour * (hour % 12) - FullCircle / 4
//
//		minuteAngle =
//			RadiansPerMinute * minute - FullCircle / 4
//
//		if (selection == TimePickerSelectionMode.Hour) {
//			anim = Animatable(hourAngle)
//		} else {
//			anim = Animatable(minuteAngle)
//		}
//
//		return true
//	}
//
//	suspend fun rotateTo(
//			angle: Float,
//			animationSpec: AnimationSpec<Float>,
//			animate: Boolean = false,
//	) {
//		userOverride.value = false
//
//		mutex.mutate(MutatePriority.UserInput) {
//			if (selection == TimePickerSelectionMode.Hour) {
//				val selectedDisplayHour =
//					angle.toHour() % 12
//
//				val selectedHour =
//					if (is24hour) {
//						if (isPm) {
//							selectedDisplayHour + 12
//						} else {
//							selectedDisplayHour
//						}
//					} else {
//						if (isPm) {
//							if (selectedDisplayHour == 0) {
//								12
//							} else {
//								selectedDisplayHour + 12
//							}
//						} else {
//							if (selectedDisplayHour == 12) {
//								0
//							} else {
//								selectedDisplayHour
//							}
//						}
//					}
//
//				val candidateMinute = state.minute
//
//				if (
//					!state.isTimeAllowed(
//						hour = selectedHour,
//						minute = candidateMinute,
//						restriction = restriction,
//					)
//				) {
//					state.notifyInvalidTime(
//						hour = selectedHour,
//						minute = candidateMinute,
//						restriction = restriction,
//					)
//					return@mutate
//				}
//
//				hourAngle =
//					RadiansPerHour *
//							selectedDisplayHour -
//							FullCircle / 4
//
//				state.hour = selectedHour
//
//				if (!animate) {
//					anim.snapTo(offsetAngle(angle))
//				} else {
//					val endAngle =
//						endValueForAnimation(offsetAngle(angle))
//
//					anim.animateTo(endAngle, animationSpec)
//				}
//			} else {
//				val selectedMinute =
//					angle.toMinute()
//
//				val candidateHour =
//					state.hour
//
//				if (
//					!state.isTimeAllowed(
//						hour = candidateHour,
//						minute = selectedMinute,
//						restriction = restriction,
//					)
//				) {
//					state.notifyInvalidTime(
//						hour = candidateHour,
//						minute = selectedMinute,
//						restriction = restriction,
//					)
//					return@mutate
//				}
//
//				minuteAngle =
//					RadiansPerMinute *
//							selectedMinute -
//							FullCircle / 4
//
//				state.minute = selectedMinute
//
//				if (!animate) {
//					anim.snapTo(offsetAngle(angle))
//				} else {
//					val endAngle =
//						endValueForAnimation(offsetAngle(angle))
//
//					anim.animateTo(endAngle, animationSpec)
//				}
//			}
//		}
//	}
//
//	override var minute: Int
//		get() = state.minute
//
//		set(value) {
//			val normalized =
//				value.coerceIn(0, 59)
//
//			val currentHour = state.hour
//
//			if (
//				!state.isTimeAllowed(
//					hour = currentHour,
//					minute = normalized,
//					restriction = restriction,
//				)
//			) {
//				state.notifyInvalidTime(
//					hour = currentHour,
//					minute = normalized,
//					restriction = restriction,
//				)
//				return
//			}
//
//			minuteAngle =
//				RadiansPerMinute * normalized -
//						FullCircle / 4
//
//			state.minute = normalized
//
//			if (selection == TimePickerSelectionMode.Minute) {
//				anim = Animatable(minuteAngle)
//			}
//		}
//
//	override var hour: Int
//		get() = state.hour
//
//		set(value) {
//			val normalized =
//				value.coerceIn(0, 23)
//
//			val currentMinute =
//				state.minute
//
//			if (
//				!state.isTimeAllowed(
//					hour = normalized,
//					minute = currentMinute,
//					restriction = restriction,
//				)
//			) {
//				state.notifyInvalidTime(
//					hour = normalized,
//					minute = currentMinute,
//					restriction = restriction,
//				)
//				return
//			}
//
//			hourAngle =
//				RadiansPerHour *
//						(normalized % 12) -
//						FullCircle / 4
//
//			state.hour = normalized
//
//			if (selection == TimePickerSelectionMode.Hour) {
//				anim = Animatable(hourAngle)
//			}
//		}
//
//	private fun Float.normalize(): Float {
//		var normalizedAngle =
//			this % FullCircle
//
//		if (normalizedAngle < 0) {
//			normalizedAngle += FullCircle
//		}
//
//		return normalizedAngle
//	}
//
//	private fun Float.toHour(): Int {
//		val hourOffset =
//			RadiansPerHour / 2
//
//		val totalOffset =
//			hourOffset + QuarterCircle
//
//		return (
//				(this + totalOffset) /
//						RadiansPerHour
//				).toInt() % 12
//	}
//
//	private fun Float.toMinute(): Int {
//		val minuteOffset =
//			RadiansPerMinute / 2
//
//		val totalOffset =
//			minuteOffset + QuarterCircle
//
//		return (
//				(this + totalOffset) /
//						RadiansPerMinute
//				).toInt() % 60
//	}
//
//	private fun offsetAngle(
//			angle: Float,
//	): Float {
//		val ret =
//			angle + QuarterCircle.toFloat()
//
//		return if (ret < 0) {
//			ret + FullCircle
//		} else {
//			ret
//		}
//	}
//}
//
//
//private suspend fun RestrictedAnalogTimePickerState.onTap(
//		x: Float,
//		y: Float,
//		maxDist: Float,
//		autoSwitchToMinute: Boolean,
//		center: IntOffset,
//		animationSpec: AnimationSpec<Float>,
//) {
//	var angle =
//		atan(
//			y - center.y,
//			x - center.x,
//		)
//
//	if (selection == TimePickerSelectionMode.Minute) {
//		angle =
//			round(
//				angle /
//						RadiansPerMinute /
//						5f
//			) *
//					5f *
//					RadiansPerMinute
//	} else {
//		angle =
//			round(
//				angle /
//						RadiansPerHour
//			) *
//					RadiansPerHour
//	}
//
//	val selectedHour: Int
//	val selectedMinute: Int
//
//	if (selection == TimePickerSelectionMode.Hour) {
//		val displayHour =
//			angle.toHour()
//
//		selectedHour =
//			if (is24hour) {
//				if (isPm) {
//					displayHour + 12
//				} else {
//					displayHour
//				}
//			} else {
//				if (isPm) {
//					if (displayHour == 12) {
//						12
//					} else {
//						displayHour + 12
//					}
//				} else {
//					if (displayHour == 12) {
//						0
//					} else {
//						displayHour
//					}
//				}
//			}
//
//		selectedMinute = minute
//	} else {
//		selectedHour = hour
//		selectedMinute = angle.toMinute()
//	}
//
//	if (
//		!state.isTimeAllowed(
//			hour = selectedHour,
//			minute = selectedMinute,
//			restriction = restriction,
//		)
//	) {
//		state.notifyInvalidTime(
//			hour = selectedHour,
//			minute = selectedMinute,
//			restriction = restriction,
//		)
//		return
//	}
//
//	moveSelector(
//		x = x,
//		y = y,
//		maxDist = maxDist,
//		center = center,
//	)
//
//	rotateTo(
//		angle = angle,
//		animationSpec = animationSpec,
//		animate = true,
//	)
//
//	if (
//		selection == TimePickerSelectionMode.Hour &&
//		autoSwitchToMinute
//	) {
//		delay(100)
//	}
//
//	if (autoSwitchToMinute) {
//		selection = TimePickerSelectionMode.Minute
//	}
//}
//
//
//private fun RestrictedAnalogTimePickerState.moveSelector(
//		x: Float,
//		y: Float,
//		maxDist: Float,
//		center: IntOffset,
//) {
//	if (
//		selection == TimePickerSelectionMode.Hour &&
//		is24hour
//	) {
//		val currentDist =
//			dist(
//				x,
//				y,
//				center.x,
//				center.y,
//			)
//
//		val displayHour =
//			hour % 12
//
//		val candidateHour =
//			if (currentDist >= maxDist) {
//				displayHour
//			} else {
//				displayHour + 12
//			}
//
//		if (
//			state.isTimeAllowed(
//				hour = candidateHour,
//				minute = minute,
//				restriction = restriction,
//			)
//		) {
//			if (isPm) {
//				hour -=
//					if (currentDist >= maxDist) {
//						12
//					} else {
//						0
//					}
//			} else {
//				hour +=
//					if (currentDist < maxDist) {
//						12
//					} else {
//						0
//					}
//			}
//		} else {
//			state.notifyInvalidTime(
//				hour = candidateHour,
//				minute = minute,
//				restriction = restriction,
//			)
//		}
//	}
//}
//@Composable
//internal fun RestrictedClockFace(
//		modifier: Modifier,
//		state: RestrictedAnalogTimePickerState,
//		colors: TimePickerColors,
//		autoSwitchToMinute: Boolean,
//) {
//	Crossfade(
//		modifier =
//			modifier
//				.background(
//					shape = CircleShape,
//					color = colors.clockDialColor,
//				)
//				.then(
//					RestrictedClockDialModifier(
//						state = state,
//						autoSwitchToMinute = autoSwitchToMinute,
//						selection = state.selection,
//						animationSpec =
//							MotionSchemeKeyTokens
//								.DefaultSpatial
//								.value(),
//					)
//				)
//				.drawSelector(
//					state = state,
//					colors = colors,
//				),
//		targetState = state.clockFaceValues,
//		animationSpec =
//			MotionSchemeKeyTokens
//				.DefaultEffects
//				.value(),
//	) { screen ->
//
//		CircularLayout(
//			modifier =
//				Modifier
//					.size(ClockDialContainerSize)
//					.semantics {
//						selectableGroup()
//					},
//			radiusToSizeRatio =
//				OuterCircleToSizeRatio,
//		) {
//			CompositionLocalProvider(
//				LocalContentColor provides
//						colors.clockDialContentColor(false)
//			) {
//				repeat(screen.size) { index ->
//
//					val outerValue =
//						if (
//							!state.is24hour ||
//							state.selection ==
//							TimePickerSelectionMode.Minute
//						) {
//							screen[index]
//						} else {
//							screen[index] % 12
//						}
//
//					RestrictedClockText(
//						modifier =
//							Modifier.semantics {
//								traversalIndex =
//									index.toFloat() + 1f
//							},
//						state = state,
//						value = outerValue,
//						autoSwitchToMinute =
//							autoSwitchToMinute,
//					)
//				}
//
//				if (
//					state.selection ==
//					TimePickerSelectionMode.Hour &&
//					state.is24hour
//				) {
//					CircularLayout(
//						modifier =
//							Modifier
//								.layoutId(
//									LayoutId.InnerCircle
//								)
//								.size(
//									ClockDialContainerSize
//								)
//								.background(
//									shape = CircleShape,
//									color = Color.Transparent,
//								),
//						radiusToSizeRatio =
//							InnerCircleToSizeRatio,
//					) {
//						repeat(ExtraHours.size) { index ->
//							val innerValue =
//								ExtraHours[index]
//
//							RestrictedClockText(
//								modifier =
//									Modifier.semantics {
//										traversalIndex =
//											12 +
//													index.toFloat()
//									},
//								state = state,
//								value = innerValue,
//								autoSwitchToMinute =
//									autoSwitchToMinute,
//							)
//						}
//					}
//				}
//			}
//		}
//	}
//}
//@Composable
//private fun RestrictedClockText(
//		modifier: Modifier,
//		state: RestrictedAnalogTimePickerState,
//		value: Int,
//		autoSwitchToMinute: Boolean,
//) {
//	val style =
//		ClockDialLabelTextFont.value
//
//	val density =
//		LocalDensity.current
//
//	val maxDist =
//		with(density) {
//			MaxDistance.toPx()
//		}
//
//	var center by remember {
//		mutableStateOf(Offset.Zero)
//	}
//
//	var parentCenter by remember {
//		mutableStateOf(IntOffset.Zero)
//	}
//
//	var boundsInParent by remember {
//		mutableStateOf(Rect.Zero)
//	}
//
//	val scope =
//		rememberCoroutineScope()
//
//	val contentDescription =
//		numberContentDescription(
//			selection = state.selection,
//			is24Hour = state.is24hour,
//			number = value,
//		)
//
//	val selected by
//	remember(state) {
//		derivedStateOf {
//			val selectorPos =
//				state.selectorPos
//
//			val offset =
//				with(density) {
//					Offset(
//						selectorPos.x.toPx(),
//						selectorPos.y.toPx(),
//					)
//				}
//
//			boundsInParent.contains(offset)
//		}
//	}
//
//	val actualHour =
//		when {
//			state.selection ==
//					TimePickerSelectionMode.Minute -> {
//				state.hour
//			}
//
//			state.is24hour -> {
//				value
//			}
//
//			state.isPm -> {
//				if (value == 12) {
//					12
//				} else {
//					value + 12
//				}
//			}
//
//			else -> {
//				if (value == 12) {
//					0
//				} else {
//					value
//				}
//			}
//		}
//
//	val actualMinute =
//		if (
//			state.selection ==
//			TimePickerSelectionMode.Minute
//		) {
//			value
//		} else {
//			state.minute
//		}
//
//	val enabled =
//		state.state.isTimeAllowed(
//			hour = actualHour,
//			minute = actualMinute,
//			restriction = state.restriction,
//		)
//
//	val disabledColor =
//		LocalContentColor.current.copy(
//			alpha = 0.38f,
//		)
//
//	Box(
//		contentAlignment = Alignment.Center,
//		modifier =
//			modifier
//				.onGloballyPositioned {
//					parentCenter =
//						it.parentCoordinates?.size?.center
//							?: IntOffset.Zero
//
//					boundsInParent =
//						it.boundsInParent()
//
//					center =
//						boundsInParent.center
//				}
//				.minimumInteractiveComponentSize()
//				.size(MinimumInteractiveSize)
//				.focusable(enabled = enabled)
//				.semantics(
//					mergeDescendants = true
//				) {
//					if (enabled) {
//						onClick {
//							scope.launch {
//								state.onTap(
//									x = center.x,
//									y = center.y,
//									maxDist = maxDist,
//									autoSwitchToMinute =
//										autoSwitchToMinute,
//									center = parentCenter,
//									animationSpec =
//										SnapSpec(),
//								)
//							}
//
//							true
//						}
//					}
//
//					this.selected = selected
//
//					if (!enabled) {
//						disabled()
//					}
//				},
//	) {
//		Text(
//			modifier =
//				Modifier.clearAndSetSemantics {
//					this.contentDescription =
//						contentDescription
//				},
//			text = value.toLocalString(),
//			style = style,
//			color =
//				if (enabled) {
//					LocalContentColor.current
//				} else {
//					disabledColor
//				},
//		)
//	}
//}
//
//internal data class RestrictedClockDialModifier(
//		private val state: RestrictedAnalogTimePickerState,
//		private val autoSwitchToMinute: Boolean,
//		private val selection: TimePickerSelectionMode,
//		private val animationSpec: AnimationSpec<Float>,
//) : ModifierNodeElement<RestrictedClockDialNode>() {
//
//	override fun create(): RestrictedClockDialNode =
//		RestrictedClockDialNode(
//			state = state,
//			autoSwitchToMinute = autoSwitchToMinute,
//			selection = selection,
//			animationSpec = animationSpec,
//		)
//
//	override fun update(
//			node: RestrictedClockDialNode,
//	) {
//		node.updateNode(
//			state = state,
//			autoSwitchToMinute = autoSwitchToMinute,
//			selection = selection,
//			animationSpec = animationSpec,
//		)
//	}
//
//	override fun InspectorInfo.inspectableProperties() {
//		// Intentionally empty.
//	}
//}
//internal class RestrictedClockDialNode(
//		private var state: RestrictedAnalogTimePickerState,
//		private var autoSwitchToMinute: Boolean,
//		private var selection: TimePickerSelectionMode,
//		private var animationSpec: AnimationSpec<Float>,
//) :
//	DelegatingNode(),
//	PointerInputModifierNode,
//	CompositionLocalConsumerModifierNode,
//	LayoutAwareModifierNode {
//
//	private var offsetX = 0f
//	private var offsetY = 0f
//
//	private var center: IntOffset by
//	mutableStateOf(IntOffset.Zero)
//
//	private val maxDist: Float
//		get() =
//			with(requireDensity()) {
//				MaxDistance.toPx() *
//						state.currentDiameter.roundToPx() /
//						ClockDialContainerSize.roundToPx()
//			}
//
//	private val pointerInputTapNode =
//		delegate(
//			SuspendingPointerInputModifierNode {
//				detectTapGestures(
//					onPress = {
//						offsetX = it.x
//						offsetY = it.y
//					},
//					onTap = {
//						coroutineScope.launch {
//							state.onTap(
//								x = it.x,
//								y = it.y,
//								maxDist = maxDist,
//								autoSwitchToMinute =
//									autoSwitchToMinute,
//								center = center,
//								animationSpec =
//									animationSpec,
//							)
//						}
//					},
//				)
//			}
//		)
//
//	private val pointerInputDragNode =
//		delegate(
//			SuspendingPointerInputModifierNode {
//				detectDragGestures(
//					onDragEnd = {
//						coroutineScope.launch {
//							if (autoSwitchToMinute) {
//								state.selection =
//									TimePickerSelectionMode.Minute
//							}
//
//							state.onGestureEnd(
//								animationSpec
//							)
//						}
//					}
//				) { _, dragAmount ->
//					coroutineScope.launch {
//						offsetX += dragAmount.x
//						offsetY += dragAmount.y
//
//						state.rotateTo(
//							angle =
//								atan(
//									offsetY - center.y,
//									offsetX - center.x,
//								),
//							animationSpec =
//								animationSpec,
//						)
//
//						state.moveSelector(
//							x = offsetX,
//							y = offsetY,
//							maxDist = maxDist,
//							center = center,
//						)
//					}
//				}
//			}
//		)
//
//	override fun onRemeasured(
//			size: IntSize,
//	) {
//		center = size.center
//
//		state.currentDiameter =
//			with(requireDensity()) {
//				size.width.toDp()
//			}
//	}
//
//	override fun onPointerEvent(
//			pointerEvent: PointerEvent,
//			pass: PointerEventPass,
//			bounds: IntSize,
//	) {
//		pointerInputTapNode.onPointerEvent(
//			pointerEvent,
//			pass,
//			bounds,
//		)
//
//		pointerInputDragNode.onPointerEvent(
//			pointerEvent,
//			pass,
//			bounds,
//		)
//	}
//
//	override fun onCancelPointerInput() {
//		pointerInputTapNode.onCancelPointerInput()
//		pointerInputDragNode.onCancelPointerInput()
//	}
//
//	fun updateNode(
//			state: RestrictedAnalogTimePickerState,
//			autoSwitchToMinute: Boolean,
//			selection: TimePickerSelectionMode,
//			animationSpec: AnimationSpec<Float>,
//	) {
//		this.state = state
//		this.autoSwitchToMinute = autoSwitchToMinute
//		this.animationSpec = animationSpec
//
//		if (this.selection != selection) {
//			this.selection = selection
//
//			coroutineScope.launch {
//				state.animateToCurrent(animationSpec)
//			}
//		}
//	}
//}
//@Composable
//@ExperimentalMaterial3Api
//internal fun RestrictedVerticalTimePicker(
//		state: RestrictedAnalogTimePickerState,
//		modifier: Modifier = Modifier,
//		colors: TimePickerColors = TimePickerDefaults.colors(),
//		autoSwitchToMinute: Boolean,
//) {
//	Column(
//		modifier =
//			modifier.semantics {
//				isTraversalGroup = true
//			},
//		horizontalAlignment =
//			Alignment.CenterHorizontally,
//	) {
//		VerticalClockDisplay(
//			state = state,
//			colors = colors,
//		)
//
//		Spacer(
//			modifier =
//				Modifier.height(
//					ClockDisplayBottomMargin
//				)
//		)
//
//		RestrictedClockFace(
//			modifier =
//				Modifier.size(
//					ClockDialContainerSize
//				),
//			state = state,
//			colors = colors,
//			autoSwitchToMinute =
//				autoSwitchToMinute,
//		)
//
//		Spacer(
//			modifier =
//				Modifier.height(
//					ClockFaceBottomMargin
//				)
//		)
//	}
//}
//@Composable
//internal fun RestrictedHorizontalTimePicker(
//		state: RestrictedAnalogTimePickerState,
//		modifier: Modifier = Modifier,
//		colors: TimePickerColors = TimePickerDefaults.colors(),
//		autoSwitchToMinute: Boolean,
//) {
//	Row(
//		modifier =
//			modifier.semantics {
//				isTraversalGroup = true
//			},
//		verticalAlignment =
//			Alignment.CenterVertically,
//	) {
//		HorizontalClockDisplay(
//			state,
//			colors,
//		)
//
//		Spacer(
//			modifier =
//				Modifier.width(
//					ClockDisplayBottomMargin
//				)
//		)
//
//		RestrictedClockFace(
//			modifier =
//				Modifier.then(
//					ClockFaceSizeModifier()
//				),
//			state = state,
//			colors = colors,
//			autoSwitchToMinute =
//				autoSwitchToMinute,
//		)
//	}
//}
//@Composable
//private fun RestrictedPeriodToggleImpl(
//		modifier: Modifier,
//		state: RestrictedAnalogTimePickerState,
//		colors: TimePickerColors,
//		measurePolicy: MeasurePolicy,
//		startShape: Shape,
//		endShape: Shape,
//) {
//	val borderStroke =
//		BorderStroke(
//			TimePickerTokens.PeriodSelectorOutlineWidth,
//			colors.periodSelectorBorderColor,
//		)
//
//	val shape =
//		PeriodSelectorContainerShape.value as CornerBasedShape
//
//	val contentDescription =
//		getString(
//			Strings.TimePickerPeriodToggle
//		)
//
//	val canSelectAm =
//		state.is24hour ||
//				state.restriction.minimumTime == null ||
//				(state.restriction.minimumTime.totalMinutes < 12 * 60)
//
//	val canSelectPm =
//		state.is24hour ||
//				state.restriction.minimumTime == null ||
//				(state.restriction.minimumTime.totalMinutes <= 23 * 60 + 59)
//
//	Layout(
//		modifier =
//			modifier
//				.semantics {
//					isTraversalGroup = true
//					this.contentDescription =
//						contentDescription
//				}
//				.selectableGroup()
//				.border(
//					border = borderStroke,
//					shape = shape,
//				),
//		measurePolicy = measurePolicy,
//	) {
//		ToggleItem(
//			checked = !state.isPm,
//			shape = startShape,
//			enabled = canSelectAm,
//			onClick = {
//				if (
//					state.isPm &&
//					canSelectAm
//				) {
//					val targetHour =
//						state.hour - 12
//
//					if (
//						state.state.isTimeAllowed(
//							hour = targetHour,
//							minute = state.minute,
//							restriction = state.restriction,
//						)
//					) {
//						state.hour =
//							targetHour
//					} else {
//						state.state.notifyInvalidTime(
//							hour = targetHour,
//							minute = state.minute,
//							restriction = state.restriction,
//						)
//					}
//				}
//			},
//			colors = colors,
//		) {
//			Text(
//				text =
//					getString(
//						string = Strings.TimePickerAM
//					)
//			)
//		}
//
//		Spacer(
//			Modifier.layoutId("Spacer")
//				.zIndex(SeparatorZIndex)
//				.fillMaxSize()
//				.background(
//					color =
//						colors.periodSelectorBorderColor
//				)
//		)
//
//		ToggleItem(
//			checked = state.isPm,
//			shape = endShape,
//			enabled = canSelectPm,
//			onClick = {
//				if (
//					!state.isPm &&
//					canSelectPm
//				) {
//					val targetHour =
//						state.hour + 12
//
//					if (
//						state.state.isTimeAllowed(
//							hour = targetHour,
//							minute = state.minute,
//							restriction = state.restriction,
//						)
//					) {
//						state.hour =
//							targetHour
//					} else {
//						state.state.notifyInvalidTime(
//							hour = targetHour,
//							minute = state.minute,
//							restriction = state.restriction,
//						)
//					}
//				}
//			},
//			colors = colors,
//		) {
//			Text(
//				text =
//					getString(
//						string = Strings.TimePickerPM
//					)
//			)
//		}
//	}
//}
//@Composable
//private fun ToggleItem(
//		checked: Boolean,
//		enabled: Boolean,
//		shape: Shape,
//		onClick: () -> Unit,
//		colors: TimePickerColors,
//		content: @Composable RowScope.() -> Unit,
//) {
//	val contentColor =
//		if (enabled) {
//			colors.periodSelectorContentColor(
//				checked
//			)
//		} else {
//			colors.periodSelectorContentColor(
//				checked = false
//			).copy(alpha = 0.38f)
//		}
//
//	val containerColor =
//		colors.periodSelectorContainerColor(
//			checked
//		)
//
//	TextButton(
//		modifier =
//			Modifier
//				.zIndex(
//					if (checked) 0f else 1f
//				)
//				.fillMaxSize()
//				.semantics {
//					selected = checked
//					disabled()
//				}
//				.then(
//					if (!enabled) {
//						Modifier.clearAndSetSemantics {
//							disabled()
//						}
//					} else {
//						Modifier
//					}
//				),
//		enabled = enabled,
//		contentPadding = PaddingValues(0.dp),
//		shape = shape,
//		onClick = onClick,
//		content = content,
//		colors =
//			ButtonDefaults.textButtonColors(
//				contentColor = contentColor,
//				containerColor = containerColor,
//				disabledContentColor =
//					contentColor,
//				disabledContainerColor =
//					containerColor,
//			),
//	)
//}
//private fun restrictedTimeInputOnChange(
//		selection: TimePickerSelectionMode,
//		state: RestrictedAnalogTimePickerState,
//		value: TextFieldValue,
//		prevValue: TextFieldValue,
//		max: Int,
//		userOverride: Ref<Boolean>,
//		onNewValue: (TextFieldValue) -> Unit,
//) {
//	userOverride.value = false
//
//	if (value.text == prevValue.text) {
//		onNewValue(value)
//		return
//	}
//
//	if (value.text.isEmpty()) {
//		if (selection == TimePickerSelectionMode.Hour) {
//			val fallbackHour =
//				if (
//					state.isPm &&
//					!state.is24hour
//				) {
//					12
//				} else {
//					0
//				}
//
//			if (
//				state.state.isTimeAllowed(
//					hour = fallbackHour,
//					minute = state.minute,
//					restriction = state.restriction,
//				)
//			) {
//				state.hour = fallbackHour
//			}
//		} else {
//			if (
//				state.state.isTimeAllowed(
//					hour = state.hour,
//					minute = 0,
//					restriction = state.restriction,
//				)
//			) {
//				state.minute = 0
//			}
//		}
//
//		onNewValue(
//			value.copy(text = "")
//		)
//		return
//	}
//
//	try {
//		val newValue =
//			if (
//				value.text.length == 3 &&
//				value.selection.start == 1
//			) {
//				value.text[0].digitToInt()
//			} else {
//				value.text.toInt()
//			}
//
//		if (newValue > max) {
//			return
//		}
//
//		if (
//			selection ==
//			TimePickerSelectionMode.Hour
//		) {
//			val targetHour =
//				if (state.is24hour) {
//					newValue
//				} else if (state.isPm) {
//					if (newValue == 12) {
//						12
//					} else {
//						newValue + 12
//					}
//				} else {
//					if (newValue == 12) {
//						0
//					} else {
//						newValue
//					}
//				}
//
//			if (
//				state.state.isTimeAllowed(
//					hour = targetHour,
//					minute = state.minute,
//					restriction = state.restriction,
//				)
//			) {
//				state.hour = targetHour
//
//				if (
//					newValue > 1 &&
//					!state.is24hour
//				) {
//					state.selection =
//						TimePickerSelectionMode.Minute
//				}
//
//				onNewValue(
//					if (value.text.length <= 2) {
//						value
//					} else {
//						value.copy(
//							text =
//								value.text[0]
//									.toString()
//						)
//					}
//				)
//			} else {
//				state.state.notifyInvalidTime(
//					hour = targetHour,
//					minute = state.minute,
//					restriction = state.restriction,
//				)
//			}
//		} else {
//			val targetMinute = newValue
//
//			if (
//				state.state.isTimeAllowed(
//					hour = state.hour,
//					minute = targetMinute,
//					restriction = state.restriction,
//				)
//			) {
//				state.minute = targetMinute
//
//				onNewValue(
//					if (value.text.length <= 2) {
//						value
//					} else {
//						value.copy(
//							text =
//								value.text[0]
//									.toString()
//						)
//					}
//				)
//			} else {
//				state.state.notifyInvalidTime(
//					hour = state.hour,
//					minute = targetMinute,
//					restriction = state.restriction,
//				)
//			}
//		}
//	} catch (_: NumberFormatException) {
//		// Ignore invalid numeric input.
//	} catch (_: IllegalArgumentException) {
//		// Ignore invalid state transitions.
//	}
//}