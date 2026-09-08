package com.coolApps.MultipleAlarmClock
//
///*
// * CustomTimePicker.kt
// *
// * A self-contained, Material3-styled analog TimePicker for Jetpack Compose, adapted from the
// * stock androidx.compose.material3 TimePicker implementation.
// *
// * WHY THIS EXISTS / WHAT'S DIFFERENT FROM STOCK:
// * The real M3 TimePicker leans on a lot of `internal` symbols (color/dimension/shape tokens,
// * localized `Strings`, an `IntList` collection type, an accessibility-service helper, locale-aware
// * number formatting, etc.) that live inside the material3 module and are not visible to app code.
// * Those have been swapped for public equivalents below (MaterialTheme.colorScheme roles, plain
// * strings, android.text.format.DateFormat, etc). The dimensions/colors are close approximations
// * of the M3 spec, not the exact private values -- tweak `PickerDimens`/`CustomTimePickerDefaults`
// * to taste.
// *
// * Everything built on PUBLIC Compose APIs (the Modifier.Node pointer-input system, Layout /
// * MeasurePolicy, Animatable, semantics, MutatorMutex) was kept essentially verbatim from stock.
// *
// * NEW FEATURE - disabling times before a cutoff:
// *  - `disabledBeforeHour` / `disabledBeforeMinute` on [CustomTimePicker] take a 24-hour cutoff
// *    (e.g. 3:30 PM -> hour = 15, minute = 30). Internally compared in minutes-since-midnight, so
// *    it works the same whether the picker is displaying 12h or 24h format.
// *  - Hour numbers on the dial are grayed out only when the WHOLE hour is before the cutoff. The
// *    boundary hour itself (the "3" when the cutoff is 3:30) stays normal, since part of it is
// *    still selectable.
// *  - Minute numbers are grayed out with exact-minute precision once you're on the boundary hour
// *    (3:30 cutoff grays :00-:25, leaves :30-:55 normal).
// *  - Tapping directly on a disabled number does NOT move the selector; instead
// *    `onDisabledTimeClick(hour, minute)` fires so you can show a snackbar/toast/etc.
// *  - Dragging the selector is intentionally left unrestricted -- it can rest anywhere (including
// *    disabled zones), matching a smooth native drag feel. Only the visual gray-out + tap-blocking
// *    signal "don't pick this".
// *  - Graying out only applies to the analog dial. The digital hour/minute display buttons above
// *    it render normally regardless of the cutoff.
// *  - The text-input variant (`TimeInput`) from stock was intentionally dropped since it's a
// *    separate composable and disabling wasn't needed there -- easy to add back if you want it.
// */
//
//
//import android.content.Context
//import android.text.format.DateFormat
//import android.view.accessibility.AccessibilityManager
//import androidx.annotation.IntRange
//import androidx.compose.animation.Crossfade
//import androidx.compose.animation.core.Animatable
//import androidx.compose.animation.core.AnimationSpec
//import androidx.compose.animation.core.SnapSpec
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.MutatePriority
//import androidx.compose.foundation.MutatorMutex
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.focusable
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.RowScope
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.selection.selectableGroup
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.minimumInteractiveComponentSize
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.Immutable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.Stable
//import androidx.compose.runtime.State
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.saveable.Saver
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.drawWithContent
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Shape
//import androidx.compose.ui.layout.Layout
//import androidx.compose.ui.layout.LayoutCoordinates
//import androidx.compose.ui.layout.Measurable
//import androidx.compose.ui.layout.MeasurePolicy
//import androidx.compose.ui.layout.MeasureResult
//import androidx.compose.ui.layout.MeasureScope
//import androidx.compose.ui.layout.boundsInParent
//import androidx.compose.ui.layout.layoutId
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
//import androidx.compose.ui.node.DelegatingNode
//import androidx.compose.ui.node.LayoutAwareModifierNode
//import androidx.compose.ui.node.ModifierNodeElement
//import androidx.compose.ui.input.pointer.PointerEvent
//import androidx.compose.ui.input.pointer.PointerEventPass
//import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
//import androidx.compose.ui.node.PointerInputModifierNode
//import androidx.compose.ui.node.requireDensity
//import androidx.compose.ui.platform.InspectorInfo
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.semantics.Role
//import androidx.compose.ui.semantics.clearAndSetSemantics
//import androidx.compose.ui.semantics.contentDescription
//import androidx.compose.ui.semantics.disabled
//import androidx.compose.ui.semantics.isTraversalGroup
//import androidx.compose.ui.semantics.onClick
//import androidx.compose.ui.semantics.role
//import androidx.compose.ui.semantics.selectableGroup
//import androidx.compose.ui.semantics.selected
//import androidx.compose.ui.semantics.semantics
//import androidx.compose.ui.semantics.traversalIndex
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.style.LineHeightStyle
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Constraints
//import androidx.compose.ui.unit.Density
//import androidx.compose.ui.unit.DpOffset
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.util.fastFilter
//import androidx.compose.ui.util.fastFirst
//import androidx.compose.ui.util.fastFirstOrNull
//import androidx.compose.ui.util.fastForEachIndexed
//import androidx.compose.ui.util.fastMap
//import androidx.compose.ui.zIndex
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlin.math.PI
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.hypot
//import kotlin.math.round
//import kotlin.math.roundToInt
//import kotlin.math.sin
//
//// ============================================================================================
//// Public entry point
//// ============================================================================================
//
///**
// * Material3-styled time picker with a clock-face dial, matching the look of the stock
// * `androidx.compose.material3.TimePicker`, plus the ability to gray out and block selection of
// * times before a given cutoff.
// *
// * Sample:
// * ```
// * val state = rememberCustomTimePickerState(initialHour = 14, initialMinute = 0)
// * CustomTimePicker(
// *     state = state,
// *     disabledBeforeHour = 15,   // 3 PM, 24-hour value
// *     disabledBeforeMinute = 30, // :30
// *     onDisabledTimeClick = { hour, minute ->
// *         // e.g. show a snackbar: "That time isn't available"
// *     },
// * )
// * ```
// *
// * @param state state for this time picker, see [rememberCustomTimePickerState].
// * @param modifier the [Modifier] to be applied to this time picker.
// * @param colors [CustomTimePickerColors] used to resolve colors in different states.
// * @param layoutType whether to show the vertical or horizontal layout.
// * @param disabledBeforeHour the 24-hour hour (0-23) before which times are disabled. Pass `null`
// *   (the default) to disable nothing. Always a 24-hour value regardless of `state.is24hour` --
// *   e.g. 3:30 PM is `hour = 15`.
// * @param disabledBeforeMinute the minute (0-59) component of the cutoff, used together with
// *   [disabledBeforeHour]. Ignored if [disabledBeforeHour] is `null`.
// * @param onDisabledTimeClick called with the (hour, minute) the user tapped on the dial when it
// *   falls before the cutoff. The selector does not move and the state is not changed. Dragging
// *   into a disabled area is intentionally NOT blocked and does not invoke this callback -- only a
// *   direct tap on a grayed-out number does.
// */
//@Composable
//fun CustomTimePicker(
//		state: CustomTimePickerState,
//		modifier: Modifier = Modifier,
//		colors: CustomTimePickerColors = CustomTimePickerDefaults.colors(),
//		layoutType: CustomTimePickerLayoutType = CustomTimePickerDefaults.layoutType(),
//		disabledBeforeHour: Int? = null,
//		disabledBeforeMinute: Int = 0,
//		onDisabledTimeClick: (hour: Int, minute: Int) -> Unit = { _, _ -> },
//) {
//	val isTouchExplorationEnabled by rememberIsTouchExplorationEnabled()
//	val userOverride = remember { Ref<Boolean>() }
//
//	val disabledBeforeTotalMinutes =
//		remember(disabledBeforeHour, disabledBeforeMinute) {
//			disabledBeforeHour?.let { it * 60 + disabledBeforeMinute }
//		}
//
//	val analogState =
//		remember(state, disabledBeforeTotalMinutes, onDisabledTimeClick) {
//			CustomAnalogTimePickerState(
//				state = state,
//				disabledBeforeTotalMinutes = disabledBeforeTotalMinutes,
//				onDisabledTimeClick = onDisabledTimeClick,
//				userOverride = userOverride,
//			)
//		}
//
//	LaunchedEffect(state.hour, state.minute) {
//		if (userOverride.value == true) {
//			analogState.hour = state.hour
//			analogState.minute = state.minute
//		}
//		userOverride.value = true
//	}
//
//	if (layoutType == CustomTimePickerLayoutType.Vertical) {
//		VerticalTimePicker(
//			state = analogState,
//			modifier = modifier,
//			colors = colors,
//			autoSwitchToMinute = !isTouchExplorationEnabled,
//		)
//	} else {
//		HorizontalTimePicker(
//			state = analogState,
//			modifier = modifier,
//			colors = colors,
//			autoSwitchToMinute = !isTouchExplorationEnabled,
//		)
//	}
//}
//
//// ============================================================================================
//// State
//// ============================================================================================
//
///** Represents the different configurations for the layout of the time picker. */
//@Immutable
//@JvmInline
//value class CustomTimePickerLayoutType internal constructor(internal val value: Int) {
//	companion object {
//		/** Horizontal layout. Use in landscape. */
//		val Horizontal = CustomTimePickerLayoutType(0)
//
//		/** Vertical layout. Use in portrait. */
//		val Vertical = CustomTimePickerLayoutType(1)
//	}
//
//	override fun toString() =
//		when (this) {
//			Horizontal -> "Horizontal"
//			Vertical -> "Vertical"
//			else -> "Unknown"
//		}
//}
//
///** Whether the hour or minute component is being actively selected. */
//@JvmInline
//value class CustomTimePickerSelectionMode private constructor(val value: Int) {
//	companion object {
//		val Hour = CustomTimePickerSelectionMode(0)
//		val Minute = CustomTimePickerSelectionMode(1)
//	}
//
//	override fun toString(): String =
//		when (this) {
//			Hour -> "Hour"
//			Minute -> "Minute"
//			else -> ""
//		}
//}
//
///**
// * A state object that can be hoisted to observe the time picker state. It holds the current
// * values and allows for directly setting those values.
// *
// * @see rememberCustomTimePickerState to construct the default implementation.
// */
//interface CustomTimePickerState {
//	/** The currently selected minute (0-59). */
//	@get:IntRange(from = 0, to = 59) @setparam:IntRange(from = 0, to = 59) var minute: Int
//
//	/** The currently selected hour, always in 24-hour form (0-23). */
//	@get:IntRange(from = 0, to = 23) @setparam:IntRange(from = 0, to = 23) var hour: Int
//
//	/** Whether the picker displays 24-hour format (`true`) or 12-hour with AM/PM (`false`). */
//	var is24hour: Boolean
//
//	/** Whether the hour or minute component is being actively selected. */
//	var selection: CustomTimePickerSelectionMode
//}
//
///** Whether the selected time falls within 12 PM (inclusive) to 12 AM (exclusive). */
//val CustomTimePickerState.isPm
//	get() = hour >= 12
//
///** The hour to display given the current [CustomTimePickerState.is24hour] setting. */
//internal val CustomTimePickerState.hourForDisplay: Int
//	get() =
//		when {
//			is24hour -> hour % 24
//			hour % 12 == 0 -> 12
//			isPm -> hour - 12
//			else -> hour
//		}
//
///**
// * Creates a [CustomTimePickerState] for a time picker that is remembered across compositions and
// * configuration changes.
// *
// * @param initialHour starting hour (0-23).
// * @param initialMinute starting minute (0-59).
// * @param is24Hour `false` for 12-hour format with AM/PM, `true` for 24-hour. Defaults to the
// *   system setting.
// */
//@Composable
//fun rememberCustomTimePickerState(
//		initialHour: Int = 0,
//		initialMinute: Int = 0,
//		is24Hour: Boolean = isSystem24HourFormat(),
//): CustomTimePickerState {
//	val state =
//		rememberSaveable(saver = CustomTimePickerStateImpl.Saver()) {
//			CustomTimePickerStateImpl(
//				initialHour = initialHour,
//				initialMinute = initialMinute,
//				is24Hour = is24Hour,
//			)
//		}
//	return state
//}
//
///** Factory function for the default implementation of [CustomTimePickerState]. */
//fun CustomTimePickerState(initialHour: Int, initialMinute: Int, is24Hour: Boolean): CustomTimePickerState =
//	CustomTimePickerStateImpl(initialHour, initialMinute, is24Hour)
//
//private class CustomTimePickerStateImpl(initialHour: Int, initialMinute: Int, is24Hour: Boolean) :
//	CustomTimePickerState {
//
//	init {
//		require(initialHour in 0..23) { "initialHour should be in [0..23] range" }
//		require(initialMinute in 0..59) { "initialMinute should be in [0..59] range" }
//	}
//
//	override var is24hour: Boolean = is24Hour
//	override var selection by mutableStateOf(CustomTimePickerSelectionMode.Hour)
//
//	private val hourState = mutableIntStateOf(initialHour)
//	private val minuteState = mutableIntStateOf(initialMinute)
//
//	override var minute: Int
//		get() = minuteState.intValue
//		set(value) {
//			minuteState.intValue = value
//		}
//
//	override var hour: Int
//		get() = hourState.intValue
//		set(value) {
//			hourState.intValue = value
//		}
//
//	companion object {
//		fun Saver(): Saver<CustomTimePickerStateImpl, *> =
//			Saver(
//				save = { listOf(it.hour, it.minute, it.is24hour) },
//				restore = { value ->
//					CustomTimePickerStateImpl(
//						initialHour = value[0] as Int,
//						initialMinute = value[1] as Int,
//						is24Hour = value[2] as Boolean,
//					)
//				},
//			)
//	}
//}
//
//// ============================================================================================
//// Colors
//// ============================================================================================
//
///**
// * Colors used by [CustomTimePicker] in different states.
// *
// * @param clockDialColor background color of the clock dial.
// * @param clockDialSelectedContentColor color of the numbers when selected / under the selector.
// * @param clockDialUnselectedContentColor color of the numbers when unselected and enabled.
// * @param clockDialDisabledContentColor color of numbers that fall before the disabled-before
// *   cutoff. Defaults to a low-emphasis variant of [clockDialUnselectedContentColor].
// * @param selectorColor color of the dial's selector handle/line.
// * @param containerColor the container color of the time picker.
// * @param periodSelectorBorderColor border color of the AM/PM toggle.
// * @param periodSelectorSelectedContainerColor selected container color of the AM/PM toggle.
// * @param periodSelectorUnselectedContainerColor unselected container color of the AM/PM toggle.
// * @param periodSelectorSelectedContentColor selected content color of the AM/PM toggle.
// * @param periodSelectorUnselectedContentColor unselected content color of the AM/PM toggle.
// * @param timeSelectorSelectedContainerColor selected container color of the hour/minute buttons.
// * @param timeSelectorUnselectedContainerColor unselected container color of the hour/minute
// *   buttons.
// * @param timeSelectorSelectedContentColor selected content color of the hour/minute buttons.
// * @param timeSelectorUnselectedContentColor unselected content color of the hour/minute buttons.
// */
//@Immutable
//class CustomTimePickerColors(
//		val clockDialColor: Color,
//		val selectorColor: Color,
//		val containerColor: Color,
//		val periodSelectorBorderColor: Color,
//		val clockDialSelectedContentColor: Color,
//		val clockDialUnselectedContentColor: Color,
//		val clockDialDisabledContentColor: Color,
//		val periodSelectorSelectedContainerColor: Color,
//		val periodSelectorUnselectedContainerColor: Color,
//		val periodSelectorSelectedContentColor: Color,
//		val periodSelectorUnselectedContentColor: Color,
//		val timeSelectorSelectedContainerColor: Color,
//		val timeSelectorUnselectedContainerColor: Color,
//		val timeSelectorSelectedContentColor: Color,
//		val timeSelectorUnselectedContentColor: Color,
//) {
//	fun copy(
//			clockDialColor: Color = this.clockDialColor,
//			selectorColor: Color = this.selectorColor,
//			containerColor: Color = this.containerColor,
//			periodSelectorBorderColor: Color = this.periodSelectorBorderColor,
//			clockDialSelectedContentColor: Color = this.clockDialSelectedContentColor,
//			clockDialUnselectedContentColor: Color = this.clockDialUnselectedContentColor,
//			clockDialDisabledContentColor: Color = this.clockDialDisabledContentColor,
//			periodSelectorSelectedContainerColor: Color = this.periodSelectorSelectedContainerColor,
//			periodSelectorUnselectedContainerColor: Color = this.periodSelectorUnselectedContainerColor,
//			periodSelectorSelectedContentColor: Color = this.periodSelectorSelectedContentColor,
//			periodSelectorUnselectedContentColor: Color = this.periodSelectorUnselectedContentColor,
//			timeSelectorSelectedContainerColor: Color = this.timeSelectorSelectedContainerColor,
//			timeSelectorUnselectedContainerColor: Color = this.timeSelectorUnselectedContainerColor,
//			timeSelectorSelectedContentColor: Color = this.timeSelectorSelectedContentColor,
//			timeSelectorUnselectedContentColor: Color = this.timeSelectorUnselectedContentColor,
//	) =
//		CustomTimePickerColors(
//			clockDialColor,
//			selectorColor,
//			containerColor,
//			periodSelectorBorderColor,
//			clockDialSelectedContentColor,
//			clockDialUnselectedContentColor,
//			clockDialDisabledContentColor,
//			periodSelectorSelectedContainerColor,
//			periodSelectorUnselectedContainerColor,
//			periodSelectorSelectedContentColor,
//			periodSelectorUnselectedContentColor,
//			timeSelectorSelectedContainerColor,
//			timeSelectorUnselectedContainerColor,
//			timeSelectorSelectedContentColor,
//			timeSelectorUnselectedContentColor,
//		)
//
//	@Stable
//	internal fun periodSelectorContainerColor(selected: Boolean) =
//		if (selected) periodSelectorSelectedContainerColor else periodSelectorUnselectedContainerColor
//
//	@Stable
//	internal fun periodSelectorContentColor(selected: Boolean) =
//		if (selected) periodSelectorSelectedContentColor else periodSelectorUnselectedContentColor
//
//	@Stable
//	internal fun timeSelectorContainerColor(selected: Boolean) =
//		if (selected) timeSelectorSelectedContainerColor else timeSelectorUnselectedContainerColor
//
//	@Stable
//	internal fun timeSelectorContentColor(selected: Boolean) =
//		if (selected) timeSelectorSelectedContentColor else timeSelectorUnselectedContentColor
//
//	override fun equals(other: Any?): Boolean {
//		if (this === other) return true
//		if (other !is CustomTimePickerColors) return false
//		return clockDialColor == other.clockDialColor &&
//				selectorColor == other.selectorColor &&
//				containerColor == other.containerColor &&
//				periodSelectorBorderColor == other.periodSelectorBorderColor &&
//				clockDialSelectedContentColor == other.clockDialSelectedContentColor &&
//				clockDialUnselectedContentColor == other.clockDialUnselectedContentColor &&
//				clockDialDisabledContentColor == other.clockDialDisabledContentColor &&
//				periodSelectorSelectedContainerColor == other.periodSelectorSelectedContainerColor &&
//				periodSelectorUnselectedContainerColor == other.periodSelectorUnselectedContainerColor &&
//				periodSelectorSelectedContentColor == other.periodSelectorSelectedContentColor &&
//				periodSelectorUnselectedContentColor == other.periodSelectorUnselectedContentColor &&
//				timeSelectorSelectedContainerColor == other.timeSelectorSelectedContainerColor &&
//				timeSelectorUnselectedContainerColor == other.timeSelectorUnselectedContainerColor &&
//				timeSelectorSelectedContentColor == other.timeSelectorSelectedContentColor &&
//				timeSelectorUnselectedContentColor == other.timeSelectorUnselectedContentColor
//	}
//
//	override fun hashCode(): Int {
//		var result = clockDialColor.hashCode()
//		result = 31 * result + selectorColor.hashCode()
//		result = 31 * result + containerColor.hashCode()
//		result = 31 * result + periodSelectorBorderColor.hashCode()
//		result = 31 * result + clockDialSelectedContentColor.hashCode()
//		result = 31 * result + clockDialUnselectedContentColor.hashCode()
//		result = 31 * result + clockDialDisabledContentColor.hashCode()
//		result = 31 * result + periodSelectorSelectedContainerColor.hashCode()
//		result = 31 * result + periodSelectorUnselectedContainerColor.hashCode()
//		result = 31 * result + periodSelectorSelectedContentColor.hashCode()
//		result = 31 * result + periodSelectorUnselectedContentColor.hashCode()
//		result = 31 * result + timeSelectorSelectedContainerColor.hashCode()
//		result = 31 * result + timeSelectorUnselectedContainerColor.hashCode()
//		result = 31 * result + timeSelectorSelectedContentColor.hashCode()
//		result = 31 * result + timeSelectorUnselectedContentColor.hashCode()
//		return result
//	}
//}
//
///** Contains the default values used by [CustomTimePicker]. */
//@Stable
//object CustomTimePickerDefaults {
//
//	/**
//	 * Default colors used by [CustomTimePicker]. Values are close approximations of the M3 spec
//	 * built from public `MaterialTheme.colorScheme` roles -- adjust to match your theme.
//	 */
//	@Composable
//	fun colors(
//			clockDialColor: Color = MaterialTheme.colorScheme.surfaceVariant,
//			clockDialSelectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
//			clockDialUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
//			clockDialDisabledContentColor: Color =
//				MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
//			selectorColor: Color = MaterialTheme.colorScheme.primary,
//			containerColor: Color = MaterialTheme.colorScheme.surface,
//			periodSelectorBorderColor: Color = MaterialTheme.colorScheme.outline,
//			periodSelectorSelectedContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
//			periodSelectorUnselectedContainerColor: Color = Color.Transparent,
//			periodSelectorSelectedContentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
//			periodSelectorUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
//			timeSelectorSelectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
//			timeSelectorUnselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
//			timeSelectorSelectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
//			timeSelectorUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
//	) =
//		CustomTimePickerColors(
//			clockDialColor = clockDialColor,
//			selectorColor = selectorColor,
//			containerColor = containerColor,
//			periodSelectorBorderColor = periodSelectorBorderColor,
//			clockDialSelectedContentColor = clockDialSelectedContentColor,
//			clockDialUnselectedContentColor = clockDialUnselectedContentColor,
//			clockDialDisabledContentColor = clockDialDisabledContentColor,
//			periodSelectorSelectedContainerColor = periodSelectorSelectedContainerColor,
//			periodSelectorUnselectedContainerColor = periodSelectorUnselectedContainerColor,
//			periodSelectorSelectedContentColor = periodSelectorSelectedContentColor,
//			periodSelectorUnselectedContentColor = periodSelectorUnselectedContentColor,
//			timeSelectorSelectedContainerColor = timeSelectorSelectedContainerColor,
//			timeSelectorUnselectedContainerColor = timeSelectorUnselectedContainerColor,
//			timeSelectorSelectedContentColor = timeSelectorSelectedContentColor,
//			timeSelectorUnselectedContentColor = timeSelectorUnselectedContentColor,
//		)
//
//	/** Default layout type, based on current screen orientation. */
//	@Composable
//	fun layoutType(): CustomTimePickerLayoutType {
//		val configuration = LocalConfiguration.current
//		return if (configuration.screenWidthDp > configuration.screenHeightDp) {
//			CustomTimePickerLayoutType.Horizontal
//		} else {
//			CustomTimePickerLayoutType.Vertical
//		}
//	}
//}
//
//// ============================================================================================
//// Analog dial state (adds the disabled-time logic on top of the public state)
//// ============================================================================================
//
//internal class CustomAnalogTimePickerState(
//		val state: CustomTimePickerState,
//		val disabledBeforeTotalMinutes: Int?,
//		val onDisabledTimeClick: (hour: Int, minute: Int) -> Unit,
//		val userOverride: Ref<Boolean> = Ref(),
//) : CustomTimePickerState by state {
//
//	var currentDiameter by mutableStateOf(0.dp)
//
//	val currentAngle: Float
//		get() = anim.value
//
//	private var hourAngle = RadiansPerHour * (state.hour % 12) - FullCircle / 4
//	private var minuteAngle = RadiansPerMinute * state.minute - FullCircle / 4
//	private var anim = Animatable(hourAngle)
//	private val mutex = MutatorMutex()
//
//	/** True if the exact ([hour], [minute]) combination is before the cutoff. */
//	internal fun isTimeDisabled(hour: Int, minute: Int): Boolean {
//		val threshold = disabledBeforeTotalMinutes ?: return false
//		return (hour * 60 + minute) < threshold
//	}
//
//	/** True only if EVERY minute of [hour] is before the cutoff (used to gray out hour numbers). */
//	internal fun isHourFullyDisabled(hour: Int): Boolean = isTimeDisabled(hour, 59)
//
//	/** True if [minute], within the currently selected hour, is before the cutoff. */
//	internal fun isMinuteDisabled(minute: Int): Boolean = isTimeDisabled(state.hour, minute)
//
//	val clockFaceValues: List<Int>
//		get() = if (selection == CustomTimePickerSelectionMode.Minute) Minutes else Hours
//
//	suspend fun animateToCurrent(animationSpec: AnimationSpec<Float>) {
//		if (!isUpdated()) return
//		val end =
//			if (selection == CustomTimePickerSelectionMode.Hour) {
//				endValueForAnimation(hourAngle)
//			} else {
//				endValueForAnimation(minuteAngle)
//			}
//		mutex.mutate(priority = MutatePriority.PreventUserInput) { anim.animateTo(end, animationSpec) }
//	}
//
//	private fun isUpdated(): Boolean {
//		if (
//			selection == CustomTimePickerSelectionMode.Hour &&
//			anim.targetValue.normalize() == hourAngle.normalize()
//		) {
//			return false
//		}
//		if (
//			selection == CustomTimePickerSelectionMode.Minute &&
//			anim.targetValue.normalize() == minuteAngle.normalize()
//		) {
//			return false
//		}
//		return true
//	}
//
//	private fun endValueForAnimation(new: Float): Float {
//		var diff = anim.value - new
//		while (diff > HalfCircle) diff -= FullCircle
//		while (diff <= -HalfCircle) diff += FullCircle
//		return anim.value - diff
//	}
//
//	suspend fun onGestureEnd(animationSpec: AnimationSpec<Float>) {
//		val end =
//			endValueForAnimation(
//				if (selection == CustomTimePickerSelectionMode.Hour) hourAngle else minuteAngle
//			)
//		mutex.mutate(priority = MutatePriority.PreventUserInput) { anim.animateTo(end, animationSpec) }
//	}
//
//	suspend fun rotateTo(angle: Float, animationSpec: AnimationSpec<Float>, animate: Boolean = false) {
//		userOverride.value = false
//		mutex.mutate(MutatePriority.UserInput) {
//			if (selection == CustomTimePickerSelectionMode.Hour) {
//				hourAngle = angle.toHour() % 12 * RadiansPerHour
//				state.hour = hourAngle.toHour() % 12 + if (isPm) 12 else 0
//			} else {
//				minuteAngle = angle.toMinute() * RadiansPerMinute
//				state.minute = minuteAngle.toMinute()
//			}
//
//			if (!animate) {
//				anim.snapTo(offsetAngle(angle))
//			} else {
//				val endAngle = endValueForAnimation(offsetAngle(angle))
//				anim.animateTo(endAngle, animationSpec)
//			}
//		}
//	}
//
//	override var minute: Int
//		get() = state.minute
//		set(value) {
//			minuteAngle = RadiansPerMinute * value - FullCircle / 4
//			state.minute = value
//			if (selection == CustomTimePickerSelectionMode.Minute) {
//				anim = Animatable(minuteAngle)
//			}
//		}
//
//	override var hour: Int
//		get() = state.hour
//		set(value) {
//			hourAngle = RadiansPerHour * (value % 12) - FullCircle / 4
//			state.hour = value
//			if (selection == CustomTimePickerSelectionMode.Hour) {
//				anim = Animatable(hourAngle)
//			}
//		}
//
//	private fun Float.normalize(): Float {
//		var normalizedAngle = this % (2 * PI)
//		if (normalizedAngle < 0) normalizedAngle += 2 * PI
//		return normalizedAngle.toFloat()
//	}
//
//	private fun Float.toHour(): Int {
//		val hourOffset: Float = RadiansPerHour / 2
//		val totalOffset = hourOffset + QuarterCircle
//		return ((this + totalOffset) / RadiansPerHour).toInt() % 12
//	}
//
//	private fun Float.toMinute(): Int {
//		val minuteOffset: Float = RadiansPerMinute / 2
//		val totalOffset = minuteOffset + QuarterCircle
//		return ((this + totalOffset) / RadiansPerMinute).toInt() % 60
//	}
//
//	private fun offsetAngle(angle: Float): Float {
//		val ret = angle + QuarterCircle.toFloat()
//		return if (ret < 0) ret + FullCircle else ret
//	}
//
//	/**
//	 * Handles a tap (physical or accessibility) at [x], [y] within the dial. Computes what the
//	 * tap WOULD select; if that lands before the disabled-before cutoff, [onDisabledTimeClick]
//	 * fires instead of moving the selector. Dragging (see [rotateTo] called directly from the
//	 * drag handler) is not routed through here and is therefore never blocked.
//	 */
//	suspend fun onTap(
//			x: Float,
//			y: Float,
//			maxDist: Float,
//			autoSwitchToMinute: Boolean,
//			center: IntOffset,
//			animationSpec: AnimationSpec<Float>,
//	) {
//		var angle = atan(y - center.y, x - center.x)
//		angle =
//			if (selection == CustomTimePickerSelectionMode.Minute) {
//				round(angle / RadiansPerMinute / 5f) * 5f * RadiansPerMinute
//			} else {
//				round(angle / RadiansPerHour) * RadiansPerHour
//			}
//
//		if (selection == CustomTimePickerSelectionMode.Minute) {
//			val targetMinute = angle.toMinute()
//			if (isMinuteDisabled(targetMinute)) {
//				onDisabledTimeClick(hour, targetMinute)
//				return
//			}
//		} else {
//			val rawHour = angle.toHour()
//			val currentDist = dist(x, y, center.x, center.y)
//			// Mirrors moveSelector()'s inner/outer-ring -> AM/PM mapping, without mutating state.
//			val effectiveIsPm = if (is24hour) currentDist < maxDist else isPm
//			val targetHour = rawHour % 12 + if (effectiveIsPm) 12 else 0
//			if (isHourFullyDisabled(targetHour)) {
//				onDisabledTimeClick(targetHour, minute)
//				return
//			}
//		}
//
//		moveSelector(x, y, maxDist, center)
//		rotateTo(angle, animationSpec = animationSpec, animate = true)
//
//		if (selection == CustomTimePickerSelectionMode.Hour && autoSwitchToMinute) {
//			delay(100)
//		}
//		if (autoSwitchToMinute) {
//			selection = CustomTimePickerSelectionMode.Minute
//		}
//	}
//}
//
//internal val CustomAnalogTimePickerState.selectorPos: DpOffset
//	get() {
//		val scale: Float = currentDiameter / PickerDimens.ClockDialContainerSize
//		val handleRadiusDp = (PickerDimens.ClockDialSelectorHandleContainerSize / 2f) * scale
//		val selectorLength =
//			if (is24hour && this.isPm && selection == CustomTimePickerSelectionMode.Hour) {
//				currentDiameter * InnerCircleToSizeRatio
//			} else {
//				currentDiameter * OuterCircleToSizeRatio
//			}
//				.minus(handleRadiusDp)
//				.coerceAtLeast(0.dp)
//
//		val length = selectorLength + handleRadiusDp
//		val offsetX = length * cos(currentAngle) + currentDiameter / 2
//		val offsetY = length * sin(currentAngle) + currentDiameter / 2
//		return DpOffset(offsetX, offsetY)
//	}
//
///** Mutates [CustomTimePickerState.hour] to flip AM/PM based on tap distance from center (24h only). */
//private fun CustomTimePickerState.moveSelector(x: Float, y: Float, maxDist: Float, center: IntOffset) {
//	if (selection == CustomTimePickerSelectionMode.Hour && is24hour) {
//		val currentDist = dist(x, y, center.x, center.y)
//		if (isPm) {
//			hour -= if (currentDist >= maxDist) 12 else 0
//		} else {
//			hour += if (currentDist < maxDist) 12 else 0
//		}
//	}
//}
//
//// ============================================================================================
//// Layout composables
//// ============================================================================================
//
//@Composable
//internal fun VerticalTimePicker(
//		state: CustomAnalogTimePickerState,
//		modifier: Modifier = Modifier,
//		colors: CustomTimePickerColors,
//		autoSwitchToMinute: Boolean,
//) {
//	Column(
//		modifier = modifier.semantics { isTraversalGroup = true },
//		horizontalAlignment = Alignment.CenterHorizontally,
//	) {
//		VerticalClockDisplay(state = state, colors = colors)
//		Spacer(modifier = Modifier.height(PickerDimens.ClockDisplayBottomMargin))
//		ClockFace(
//			modifier = Modifier.size(PickerDimens.ClockDialContainerSize),
//			state = state,
//			colors = colors,
//			autoSwitchToMinute = autoSwitchToMinute,
//		)
//		Spacer(modifier = Modifier.height(PickerDimens.ClockFaceBottomMargin))
//	}
//}
//
//@Composable
//internal fun HorizontalTimePicker(
//		state: CustomAnalogTimePickerState,
//		modifier: Modifier = Modifier,
//		colors: CustomTimePickerColors,
//		autoSwitchToMinute: Boolean,
//) {
//	Row(
//		modifier = modifier.semantics { isTraversalGroup = true },
//		verticalAlignment = Alignment.CenterVertically,
//	) {
//		HorizontalClockDisplay(state, colors)
//		Spacer(modifier = Modifier.width(PickerDimens.ClockDisplayBottomMargin))
//		ClockFace(
//			modifier = Modifier.then(ClockFaceSizeModifier()),
//			state = state,
//			colors = colors,
//			autoSwitchToMinute = autoSwitchToMinute,
//		)
//	}
//}
//
//@Composable
//private fun HorizontalClockDisplay(state: CustomTimePickerState, colors: CustomTimePickerColors) {
//	Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
//		ClockDisplayNumbers(state, colors)
//		if (!state.is24hour) {
//			Box(modifier = Modifier.padding(top = PickerDimens.PeriodToggleMargin)) {
//				HorizontalPeriodToggle(
//					modifier =
//						Modifier.size(
//							PickerDimens.PeriodSelectorHorizontalContainerWidth,
//							PickerDimens.PeriodSelectorHorizontalContainerHeight,
//						),
//					state = state,
//					colors = colors,
//				)
//			}
//		}
//	}
//}
//
//@Composable
//private fun VerticalClockDisplay(state: CustomTimePickerState, colors: CustomTimePickerColors) {
//	Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
//		ClockDisplayNumbers(state, colors)
//		if (!state.is24hour) {
//			Box(modifier = Modifier.padding(start = PickerDimens.PeriodToggleMargin)) {
//				VerticalPeriodToggle(
//					modifier =
//						Modifier.size(
//							PickerDimens.PeriodSelectorVerticalContainerWidth,
//							PickerDimens.PeriodSelectorVerticalContainerHeight,
//						),
//					state = state,
//					colors = colors,
//				)
//			}
//		}
//	}
//}
//
//@Composable
//private fun ClockDisplayNumbers(state: CustomTimePickerState, colors: CustomTimePickerColors) {
//	Row {
//		TimeSelector(
//			modifier =
//				Modifier.size(
//					PickerDimens.TimeSelectorContainerWidth,
//					PickerDimens.TimeSelectorContainerHeight,
//				),
//			value = state.hourForDisplay,
//			state = state,
//			selection = CustomTimePickerSelectionMode.Hour,
//			colors = colors,
//		)
//		DisplaySeparator(Modifier.size(PickerDimens.DisplaySeparatorWidth, PickerDimens.PeriodSelectorVerticalContainerHeight))
//		TimeSelector(
//			modifier =
//				Modifier.size(
//					PickerDimens.TimeSelectorContainerWidth,
//					PickerDimens.TimeSelectorContainerHeight,
//				),
//			value = state.minute,
//			state = state,
//			selection = CustomTimePickerSelectionMode.Minute,
//			colors = colors,
//		)
//	}
//}
//
//@Composable
//private fun DisplaySeparator(modifier: Modifier) {
//	val style =
//		TextStyle(
//			textAlign = TextAlign.Center,
//			fontSize = MaterialTheme.typography.displayMedium.fontSize,
//			lineHeightStyle =
//				LineHeightStyle(
//					alignment = LineHeightStyle.Alignment.Center,
//					trim = LineHeightStyle.Trim.Both,
//				),
//		)
//	Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
//		Text(text = ":", color = MaterialTheme.colorScheme.onSurface, style = style)
//	}
//}
//
//@Composable
//private fun TimeSelector(
//		modifier: Modifier,
//		value: Int,
//		state: CustomTimePickerState,
//		selection: CustomTimePickerSelectionMode,
//		colors: CustomTimePickerColors,
//) {
//	val selected = state.selection == selection
//	val selectorContentDescription =
//		if (selection == CustomTimePickerSelectionMode.Hour) "Select hour" else "Select minute"
//
//	val containerColor = colors.timeSelectorContainerColor(selected)
//	val contentColor = colors.timeSelectorContentColor(selected)
//	Surface(
//		modifier =
//			modifier.semantics(mergeDescendants = true) {
//				role = Role.RadioButton
//				this.contentDescription = selectorContentDescription
//			},
//		onClick = { if (selection != state.selection) state.selection = selection },
//		selected = selected,
//		shape = PickerDimens.TimeSelectorShape,
//		color = containerColor,
//	) {
//		Box(contentAlignment = Alignment.Center) {
//			Text(
//				modifier =
//					Modifier.semantics {
//						contentDescription = numberContentDescription(selection, state.is24hour, value)
//					},
//				text = value.toLocalString(minDigits = 2),
//				color = contentColor,
//				style = MaterialTheme.typography.displayMedium,
//			)
//		}
//	}
//}
//
//@Composable
//private fun HorizontalPeriodToggle(
//		modifier: Modifier,
//		state: CustomTimePickerState,
//		colors: CustomTimePickerColors,
//) {
//	val measurePolicy = remember {
//		MeasurePolicy { measurables, constraints ->
//			val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
//			val spacerPlaceable =
//				spacer.measure(
//					constraints.copy(minWidth = 0, maxWidth = PickerDimens.PeriodSelectorOutlineWidth.roundToPx())
//				)
//			val items =
//				measurables.fastFilter { it.layoutId != "Spacer" }.fastMap { item ->
//					item.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth / 2))
//				}
//			layout(constraints.maxWidth, constraints.maxHeight) {
//				items[0].place(0, 0)
//				items[1].place(items[0].width, 0)
//				spacerPlaceable.place(items[0].width - spacerPlaceable.width / 2, 0)
//			}
//		}
//	}
//	PeriodToggleImpl(
//		modifier = modifier,
//		state = state,
//		colors = colors,
//		measurePolicy = measurePolicy,
//		startShape = PickerDimens.PeriodSelectorStartShape,
//		endShape = PickerDimens.PeriodSelectorEndShape,
//	)
//}
//
//@Composable
//private fun VerticalPeriodToggle(
//		modifier: Modifier,
//		state: CustomTimePickerState,
//		colors: CustomTimePickerColors,
//) {
//	val measurePolicy = remember {
//		MeasurePolicy { measurables, constraints ->
//			val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
//			val spacerPlaceable =
//				spacer.measure(
//					constraints.copy(minHeight = 0, maxHeight = PickerDimens.PeriodSelectorOutlineWidth.roundToPx())
//				)
//			val items =
//				measurables.fastFilter { it.layoutId != "Spacer" }.fastMap { item ->
//					item.measure(constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight / 2))
//				}
//			layout(constraints.maxWidth, constraints.maxHeight) {
//				items[0].place(0, 0)
//				items[1].place(0, items[0].height)
//				spacerPlaceable.place(0, items[0].height - spacerPlaceable.height / 2)
//			}
//		}
//	}
//	PeriodToggleImpl(
//		modifier = modifier,
//		state = state,
//		colors = colors,
//		measurePolicy = measurePolicy,
//		startShape = PickerDimens.PeriodSelectorTopShape,
//		endShape = PickerDimens.PeriodSelectorBottomShape,
//	)
//}
//
//@Composable
//private fun PeriodToggleImpl(
//		modifier: Modifier,
//		state: CustomTimePickerState,
//		colors: CustomTimePickerColors,
//		measurePolicy: MeasurePolicy,
//		startShape: Shape,
//		endShape: Shape,
//) {
//	val borderStroke = BorderStroke(PickerDimens.PeriodSelectorOutlineWidth, colors.periodSelectorBorderColor)
//	Layout(
//		modifier =
//			modifier
//				.semantics {
//					isTraversalGroup = true
//					contentDescription = "AM or PM"
//				}
//				.selectableGroup()
//				.border(border = borderStroke, shape = PickerDimens.PeriodSelectorOutlineShape),
//		measurePolicy = measurePolicy,
//		content = {
//			ToggleItem(
//				checked = !state.isPm,
//				shape = startShape,
//				onClick = { if (state.isPm) state.hour -= 12 },
//				colors = colors,
//			) {
//				Text(text = "AM")
//			}
//			Spacer(
//				Modifier.layoutId("Spacer")
//					.androidx_zIndex(2f)
//					.androidx_fillMaxSize()
//					.background(color = colors.periodSelectorBorderColor)
//			)
//			ToggleItem(
//				checked = state.isPm,
//				shape = endShape,
//				onClick = { if (!state.isPm) state.hour += 12 },
//				colors = colors,
//			) {
//				Text(text = "PM")
//			}
//		},
//	)
//}
//
//@Composable
//private fun ToggleItem(
//		checked: Boolean,
//		shape: Shape,
//		onClick: () -> Unit,
//		colors: CustomTimePickerColors,
//		content: @Composable RowScope.() -> Unit,
//) {
//	val contentColor = colors.periodSelectorContentColor(checked)
//	val containerColor = colors.periodSelectorContainerColor(checked)
//	TextButton(
//		modifier =
//			Modifier.androidx_zIndex(if (checked) 0f else 1f)
//				.androidx_fillMaxSize()
//				.semantics { selected = checked },
//		contentPadding = PaddingValues(0.dp),
//		shape = shape,
//		onClick = onClick,
//		content = content,
//		colors = ButtonDefaults.textButtonColors(contentColor = contentColor, containerColor = containerColor),
//	)
//}
//
//// ============================================================================================
//// Clock face (dial)
//// ============================================================================================
//
//private enum class LayoutId {
//	Selector,
//	InnerCircle,
//}
//
//@Composable
//internal fun ClockFace(
//		modifier: Modifier,
//		state: CustomAnalogTimePickerState,
//		colors: CustomTimePickerColors,
//		autoSwitchToMinute: Boolean,
//) {
//	Crossfade(
//		modifier =
//			modifier
//				.background(shape = CircleShape, color = colors.clockDialColor)
//				.then(
//					ClockDialModifier(
//						state = state,
//						autoSwitchToMinute = autoSwitchToMinute,
//						selection = state.selection,
//						animationSpec = DefaultAnimationSpec,
//					)
//				)
//				.drawSelector(state, colors),
//		targetState = state.clockFaceValues,
//		animationSpec = DefaultAnimationSpec,
//	) { screen ->
//		CircularLayout(
//			modifier = Modifier.size(PickerDimens.ClockDialContainerSize).semantics { selectableGroup() },
//			radiusToSizeRatio = OuterCircleToSizeRatio,
//		) {
//			val isMinuteMode = state.selection == CustomTimePickerSelectionMode.Minute
//			repeat(screen.size) { index ->
//				val outerValue = if (!state.is24hour || isMinuteMode) screen[index] else screen[index] % 12
//				val disabled =
//					if (isMinuteMode) {
//						state.isMinuteDisabled(outerValue)
//					} else {
//						val actualHour =
//							if (state.is24hour) outerValue else (outerValue % 12) + if (state.isPm) 12 else 0
//						state.isHourFullyDisabled(actualHour)
//					}
//				ClockText(
//					modifier = Modifier.semantics { traversalIndex = index.toFloat() + 1f },
//					state = state,
//					value = outerValue,
//					disabled = disabled,
//					autoSwitchToMinute = autoSwitchToMinute,
//					colors = colors,
//				)
//			}
//
//			if (state.selection == CustomTimePickerSelectionMode.Hour && state.is24hour) {
//				CircularLayout(
//					modifier =
//						Modifier.layoutId(LayoutId.InnerCircle)
//							.size(PickerDimens.ClockDialContainerSize)
//							.background(shape = CircleShape, color = Color.Transparent),
//					radiusToSizeRatio = InnerCircleToSizeRatio,
//				) {
//					repeat(ExtraHours.size) { index ->
//						val innerValue = ExtraHours[index] // already the actual 12-23 hour
//						ClockText(
//							modifier = Modifier.semantics { traversalIndex = 12 + index.toFloat() },
//							state = state,
//							value = innerValue,
//							disabled = state.isHourFullyDisabled(innerValue),
//							autoSwitchToMinute = autoSwitchToMinute,
//							colors = colors,
//						)
//					}
//				}
//			}
//		}
//	}
//}
//
///**
// * Draws the selector handle/line/dot behind the numbers (drawContent draws the numbers on top,
// * each already tinted correctly for selected/unselected/disabled by [ClockText]).
// */
//private fun Modifier.drawSelector(state: CustomAnalogTimePickerState, colors: CustomTimePickerColors): Modifier =
//	this.androidx_drawWithContent {
//		val selectorOffsetPx = Offset(state.selectorPos.x.toPx(), state.selectorPos.y.toPx())
//		val selectorRadius =
//			PickerDimens.ClockDialSelectorHandleContainerSize.toPx() / 2f * state.currentDiameter.roundToPx() /
//					PickerDimens.ClockDialContainerSize.roundToPx()
//		val selectorColor = colors.selectorColor
//
//		drawCircle(radius = selectorRadius, center = selectorOffsetPx, color = selectorColor)
//
//		val strokeWidth = PickerDimens.ClockDialSelectorTrackContainerWidth.toPx()
//		val lineEnd =
//			selectorOffsetPx.minus(
//				Offset(selectorRadius * cos(state.currentAngle), selectorRadius * sin(state.currentAngle))
//			)
//		drawLine(start = center, end = lineEnd, strokeWidth = strokeWidth, color = selectorColor)
//		drawCircle(
//			radius = PickerDimens.ClockDialSelectorCenterContainerSize.toPx() / 2,
//			center = center,
//			color = selectorColor,
//		)
//
//		drawContent()
//	}
//
//@Composable
//private fun ClockText(
//		modifier: Modifier,
//		state: CustomAnalogTimePickerState,
//		value: Int,
//		disabled: Boolean,
//		autoSwitchToMinute: Boolean,
//		colors: CustomTimePickerColors,
//) {
//	val style = MaterialTheme.typography.bodyLarge
//	val density: Density = LocalDensity.current
//	val maxDist = with(density) { PickerDimens.MaxDistance.toPx() }
//	var center by remember { mutableStateOf(Offset.Zero) }
//	var parentCenter by remember { mutableStateOf(IntOffset.Zero) }
//	var boundsInParent by remember { mutableStateOf(Rect.Zero) }
//	val scope = rememberCoroutineScope()
//
//	val text = value.toLocalString()
//	val selected by
//	remember(state) {
//		derivedStateOf {
//			val selectorPos = state.selectorPos
//			val offset = with(density) { Offset(selectorPos.x.toPx(), selectorPos.y.toPx()) }
//			boundsInParent.contains(offset)
//		}
//	}
//
//	val textColor =
//		when {
//			disabled -> colors.clockDialDisabledContentColor
//			selected -> colors.clockDialSelectedContentColor
//			else -> colors.clockDialUnselectedContentColor
//		}
//
//	val baseDescription = numberContentDescription(state.selection, state.is24hour, value)
//	val contentDescriptionText = if (disabled) "$baseDescription, unavailable" else baseDescription
//
//	Box(
//		contentAlignment = Alignment.Center,
//		modifier =
//			modifier
//				.onGloballyPositioned { coordinates: LayoutCoordinates ->
//					parentCenter =
//						coordinates.parentCoordinates?.size?.let { IntOffset(it.width / 2, it.height / 2) }
//							?: IntOffset.Zero
//					boundsInParent = coordinates.boundsInParent()
//					center = boundsInParent.center
//				}
//				.minimumInteractiveComponentSize()
//				.size(PickerDimens.MinimumInteractiveSize)
//				.focusable()
//				.semantics(mergeDescendants = true) {
//					onClick {
//						scope.launch {
//							state.onTap(
//								x = center.x,
//								y = center.y,
//								maxDist = maxDist,
//								autoSwitchToMinute = autoSwitchToMinute,
//								center = parentCenter,
//								animationSpec = SnapSpec(),
//							)
//						}
//						true
//					}
//					this.selected = selected
//					if (disabled) disabled()
//				},
//	) {
//		Text(
//			modifier = Modifier.clearAndSetSemantics { contentDescription = contentDescriptionText },
//			text = text,
//			color = textColor,
//			style = style,
//		)
//	}
//}
//
//private fun numberContentDescription(
//		selection: CustomTimePickerSelectionMode,
//		is24Hour: Boolean,
//		number: Int,
//): String =
//	when {
//		selection == CustomTimePickerSelectionMode.Minute -> "$number minutes"
//		is24Hour -> "$number hours"
//		else -> "$number o'clock"
//	}
//
//// ============================================================================================
//// Pointer input (Modifier.Node) -- public Compose APIs, structurally unchanged from stock
//// ============================================================================================
//
//internal data class ClockDialModifier(
//		private val state: CustomAnalogTimePickerState,
//		private val autoSwitchToMinute: Boolean,
//		private val selection: CustomTimePickerSelectionMode,
//		private val animationSpec: AnimationSpec<Float>,
//) : ModifierNodeElement<ClockDialNode>() {
//
//	override fun create(): ClockDialNode =
//		ClockDialNode(state = state, autoSwitchToMinute = autoSwitchToMinute, selection = selection, animationSpec = animationSpec)
//
//	override fun update(node: ClockDialNode) {
//		node.updateNode(state = state, autoSwitchToMinute = autoSwitchToMinute, selection = selection, animationSpec = animationSpec)
//	}
//
//	override fun InspectorInfo.inspectableProperties() {
//		// Nothing shown in the inspector.
//	}
//}
//
//internal class ClockDialNode(
//		private var state: CustomAnalogTimePickerState,
//		private var autoSwitchToMinute: Boolean,
//		private var selection: CustomTimePickerSelectionMode,
//		private var animationSpec: AnimationSpec<Float>,
//) : DelegatingNode(), PointerInputModifierNode, CompositionLocalConsumerModifierNode, LayoutAwareModifierNode {
//
//	private var offsetX = 0f
//	private var offsetY = 0f
//	private var center: IntOffset by mutableStateOf(IntOffset.Zero)
//	private val maxDist
//		get() =
//			with(requireDensity()) {
//				PickerDimens.MaxDistance.toPx() * state.currentDiameter.roundToPx() /
//						PickerDimens.ClockDialContainerSize.roundToPx()
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
//							state.onTap(it.x, it.y, maxDist, autoSwitchToMinute, center, animationSpec)
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
//								state.selection = CustomTimePickerSelectionMode.Minute
//							}
//							state.onGestureEnd(animationSpec)
//						}
//					}
//				) { _, dragAmount ->
//					coroutineScope.launch {
//						offsetX += dragAmount.x
//						offsetY += dragAmount.y
//						// Intentionally NOT disabled-checked: dragging is left free-form.
//						state.rotateTo(atan(offsetY - center.y, offsetX - center.x), animationSpec)
//						state.moveSelectorPublic(x = offsetX, y = offsetY, maxDist = maxDist, center = center)
//					}
//				}
//			}
//		)
//
//	override fun onRemeasured(size: IntSize) {
//		center = IntOffset(size.width / 2, size.height / 2)
//		state.currentDiameter = with(requireDensity()) { size.width.toDp() }
//	}
//
//	override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
//		pointerInputTapNode.onPointerEvent(pointerEvent, pass, bounds)
//		pointerInputDragNode.onPointerEvent(pointerEvent, pass, bounds)
//	}
//
//	override fun onCancelPointerInput() {
//		pointerInputTapNode.onCancelPointerInput()
//		pointerInputDragNode.onCancelPointerInput()
//	}
//
//	fun updateNode(
//			state: CustomAnalogTimePickerState,
//			autoSwitchToMinute: Boolean,
//			selection: CustomTimePickerSelectionMode,
//			animationSpec: AnimationSpec<Float>,
//	) {
//		this.state = state
//		this.autoSwitchToMinute = autoSwitchToMinute
//		this.animationSpec = animationSpec
//		if (this.selection != selection) {
//			this.selection = selection
//			coroutineScope.launch { state.animateToCurrent(animationSpec) }
//		}
//	}
//}
//
///** Exposes the private [moveSelector] extension to the drag handler above without blocking on it. */
//internal fun CustomAnalogTimePickerState.moveSelectorPublic(x: Float, y: Float, maxDist: Float, center: IntOffset) {
//	(this as CustomTimePickerState).let {
//		if (it.selection == CustomTimePickerSelectionMode.Hour && it.is24hour) {
//			val currentDist = dist(x, y, center.x, center.y)
//			if (it.isPm) {
//				it.hour -= if (currentDist >= maxDist) 12 else 0
//			} else {
//				it.hour += if (currentDist < maxDist) 12 else 0
//			}
//		}
//	}
//}
//
///** Distributes children evenly on a circle of radius = height * [radiusToSizeRatio]. */
//@Composable
//private fun CircularLayout(
//		modifier: Modifier = Modifier,
//		radiusToSizeRatio: Float,
//		content: @Composable () -> Unit,
//) {
//	Layout(modifier = modifier, content = content) { measurables, constraints ->
//		val radiusPx = constraints.maxHeight * radiusToSizeRatio
//		val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
//		val placeable =
//			measurables
//				.fastFilter { it.layoutId != LayoutId.Selector && it.layoutId != LayoutId.InnerCircle }
//				.fastMap { measurable -> measurable.measure(itemConstraints) }
//		val selectorMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.Selector }
//		val innerMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.InnerCircle }
//		val theta = FullCircle / placeable.count()
//		val selectorPlaceable = selectorMeasurable?.measure(itemConstraints)
//		val innerCirclePlaceable = innerMeasurable?.measure(itemConstraints)
//
//		layout(width = constraints.minWidth, height = constraints.minHeight) {
//			selectorPlaceable?.place(0, 0)
//			placeable.fastForEachIndexed { i, it ->
//				val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
//				val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
//				val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
//				val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
//				it.place(x = offsetX.roundToInt(), y = offsetY.roundToInt())
//			}
//			innerCirclePlaceable?.place(
//				(constraints.minWidth - innerCirclePlaceable.width) / 2,
//				(constraints.minHeight - innerCirclePlaceable.height) / 2,
//			)
//		}
//	}
//}
//
//internal class ClockFaceSizeModifier : androidx.compose.ui.layout.LayoutModifier {
//	override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
//		val max = constraints.maxHeight.toDp()
//		val size =
//			when {
//				max >= PickerDimens.TimePickerMaxHeight -> PickerDimens.ClockDialContainerSize
//				max >= PickerDimens.TimePickerMidHeight -> PickerDimens.ClockDialMidContainerSize
//				else -> PickerDimens.ClockDialMinContainerSize
//			}.roundToPx()
//		val placeable = measurable.measure(Constraints.fixed(size, size))
//		return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
//	}
//}
//
//// ============================================================================================
//// Small helpers (stand-ins for internal M3 utilities)
//// ============================================================================================
//
///** Trivial mutable holder, standing in for the internal M3 `Ref<T>` class. */
//internal class Ref<T> {
//	var value: T? = null
//}
//
///** Simplified locale formatting stand-in for the internal M3 `Int.toLocalString`. */
//private fun Int.toLocalString(minDigits: Int = 1): String = toString().padStart(minDigits, '0')
//
///** Stand-in for the internal `is24HourFormat` composable property. */
//@Composable
//private fun isSystem24HourFormat(): Boolean {
//	val context = LocalContext.current
//	return remember(context) { DateFormat.is24HourFormat(context) }
//}
//
///**
// * Stand-in for the internal `rememberAccessibilityServiceState()` used by stock to disable the
// * auto hour->minute jump for screen-reader users. Uses the public Android AccessibilityManager.
// */
//@Composable
//private fun rememberIsTouchExplorationEnabled(): State<Boolean> {
//	val context = LocalContext.current
//	val accessibilityManager =
//		remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }
//	val state = remember { mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true) }
//	androidx.compose.runtime.DisposableEffect(accessibilityManager) {
//		val listener =
//			AccessibilityManager.TouchExplorationStateChangeListener { enabled -> state.value = enabled }
//		accessibilityManager?.addTouchExplorationStateChangeListener(listener)
//		onDispose { accessibilityManager?.removeTouchExplorationStateChangeListener(listener) }
//	}
//	return state
//}
//
//private fun dist(x1: Float, y1: Float, x2: Int, y2: Int): Float {
//	val x = x2 - x1
//	val y = y2 - y1
//	return hypot(x.toDouble(), y.toDouble()).toFloat()
//}
//
//private fun atan(y: Float, x: Float): Float {
//	val ret = atan2(y, x) - QuarterCircle.toFloat()
//	return if (ret < 0) ret + FullCircle else ret
//}
//
//// Small local aliases so the Modifier chain above reads closely to the stock source without
//// pulling in extra unqualified imports that could clash with your own project's utilities.
//private fun Modifier.androidx_zIndex(zIndex: Float): Modifier = this.then(Modifier.zIndex(zIndex))
//private fun Modifier.androidx_fillMaxSize(): Modifier = this.then(Modifier.fillMaxSize())
//private fun Modifier.androidx_drawWithContent(
//		onDraw: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit
//): Modifier = this.then(Modifier.drawWithContent(onDraw))
//
//// ============================================================================================
//// Constants & dimensions
//// ============================================================================================
//
//private const val FullCircle: Float = (PI * 2).toFloat()
//private const val HalfCircle: Float = FullCircle / 2f
//private const val QuarterCircle = PI / 2
//private const val RadiansPerMinute: Float = FullCircle / 60
//private const val RadiansPerHour: Float = FullCircle / 12f
//
//private val DefaultAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 200)
//
//private val Minutes = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
//private val Hours = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
//private val ExtraHours: List<Int> = Hours.map { (it % 12) + 12 }
//
///**
// * Dimensions/shapes approximating the M3 TimePicker spec using public values. The exact private
// * token values aren't visible outside the material3 module, so these are close approximations --
// * tune freely to match pixel-for-pixel parity with your design if needed. Values marked "(given)"
// * came directly from the stock source you shared and are exact, not approximated.
// */
//private object PickerDimens {
//	val ClockDialContainerSize = 256.dp
//	val ClockDialSelectorHandleContainerSize = 48.dp
//	val ClockDialSelectorTrackContainerWidth = 2.dp
//	val ClockDialSelectorCenterContainerSize = 8.dp
//
//	val TimeSelectorContainerWidth = 96.dp
//	val TimeSelectorContainerHeight = 80.dp
//	val TimeSelectorShape = RoundedCornerShape(24.dp)
//
//	val PeriodSelectorVerticalContainerWidth = 52.dp
//	val PeriodSelectorVerticalContainerHeight = 80.dp
//	val PeriodSelectorHorizontalContainerWidth = 216.dp
//	val PeriodSelectorHorizontalContainerHeight = 38.dp
//	val PeriodSelectorOutlineWidth = 1.dp
//	private val PeriodSelectorCornerRadius = 12.dp
//	val PeriodSelectorOutlineShape = RoundedCornerShape(PeriodSelectorCornerRadius)
//	val PeriodSelectorStartShape =
//		RoundedCornerShape(topStart = PeriodSelectorCornerRadius, bottomStart = PeriodSelectorCornerRadius, topEnd = 0.dp, bottomEnd = 0.dp)
//	val PeriodSelectorEndShape =
//		RoundedCornerShape(topEnd = PeriodSelectorCornerRadius, bottomEnd = PeriodSelectorCornerRadius, topStart = 0.dp, bottomStart = 0.dp)
//	val PeriodSelectorTopShape =
//		RoundedCornerShape(topStart = PeriodSelectorCornerRadius, topEnd = PeriodSelectorCornerRadius, bottomStart = 0.dp, bottomEnd = 0.dp)
//	val PeriodSelectorBottomShape =
//		RoundedCornerShape(bottomStart = PeriodSelectorCornerRadius, bottomEnd = PeriodSelectorCornerRadius, topStart = 0.dp, topEnd = 0.dp)
//
//	// -- given: taken verbatim from the stock source you pasted --
//	val ClockDisplayBottomMargin = 36.dp
//	val ClockFaceBottomMargin = 24.dp
//	val DisplaySeparatorWidth = 24.dp
//	val PeriodToggleMargin = 12.dp
//	val MaxDistance = 74.dp
//	val MinimumInteractiveSize = 48.dp
//	val TimePickerMaxHeight = 384.dp
//	val TimePickerMidHeight = 330.dp
//	val ClockDialMidContainerSize = 238.dp
//	val ClockDialMinContainerSize = 200.dp
//}
//
//private val OuterCircleToSizeRatio: Float = 101.dp / PickerDimens.ClockDialContainerSize
//private val InnerCircleToSizeRatio: Float = 69.dp / PickerDimens.ClockDialContainerSize