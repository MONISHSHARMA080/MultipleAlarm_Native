package com.coolApps.MultipleAlarmClock

import androidx.annotation.FloatRange
import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.intListOf
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerSelectionMode
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.isPm
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FullCircle: Float = (PI * 2).toFloat()
private const val HalfCircle: Float = FullCircle / 2f
private const val QuarterCircle = PI / 2
private const val RadiansPerMinute: Float = FullCircle / 60
private const val RadiansPerHour: Float = FullCircle / 12f
private const val SeparatorZIndex = 2f

private val ClockDialContainerSize = 256.dp
private val OuterCircleToSizeRatio: Float = 101.dp / ClockDialContainerSize
private val InnerCircleToSizeRatio: Float = 69.dp / ClockDialContainerSize
private val ClockDisplayBottomMargin = 36.dp
private val ClockFaceBottomMargin = 24.dp
private val DisplaySeparatorWidth = 24.dp

private val SupportLabelTop = 7.dp
private val TimeInputBottomPadding = 24.dp
private val MaxDistance = 74.dp
private val MinimumInteractiveSize = 48.dp
private val Minutes = intListOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
private val Hours = intListOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
private val ExtraHours: IntList =
    MutableIntList(Hours.size).apply { Hours.forEach { add((it % 12 + 12)) } }
private val PeriodToggleMargin = 12.dp

private val TimePickerMaxHeight = 384.dp
private val TimePickerMidHeight = 330.dp
private val ClockDialMidContainerSize = 238.dp
private val ClockDialMinContainerSize = 200.dp
private val ClockDialSelectorHandleContainerSize = 48.dp
private val ClockDialSelectorCenterContainerSize = 8.dp
private val ClockDialSelectorTrackContainerWidth = 2.dp
private val PeriodSelectorContainerWidth = 52.dp
private val PeriodSelectorContainerHeight = 80.dp
private val PeriodSelectorHorizontalContainerWidth = 216.dp
private val PeriodSelectorHorizontalContainerHeight = 40.dp
private val PeriodSelectorVerticalContainerWidth = 52.dp
private val PeriodSelectorVerticalContainerHeight = 80.dp
private val TimeSelectorContainerWidth = 96.dp
private val TimeSelectorContainerHeight = 80.dp
private val TimeFieldContainerWidth = 96.dp
private val TimeFieldContainerHeight = 80.dp
private val PeriodSelectorOutlineWidth = 1.dp

internal class UserOverrideRef(var value: Boolean = false)

internal fun Float.toHour(): Int {
    val hourOffset: Float = RadiansPerHour / 2
    val totalOffset = hourOffset + QuarterCircle
    return ((this + totalOffset) / RadiansPerHour).toInt() % 12
}

internal fun Float.toMinute(): Int {
    val minuteOffset: Float = RadiansPerMinute / 2
    val totalOffset = minuteOffset + QuarterCircle
    return ((this + totalOffset) / RadiansPerMinute).toInt() % 60
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun TimePickerColors.timeSelectorContainerColor(selected: Boolean): Color =
    if (selected) timeSelectorSelectedContainerColor else timeSelectorUnselectedContainerColor

@OptIn(ExperimentalMaterial3Api::class)
internal fun TimePickerColors.timeSelectorContentColor(selected: Boolean): Color =
    if (selected) timeSelectorSelectedContentColor else timeSelectorUnselectedContentColor

@OptIn(ExperimentalMaterial3Api::class)
internal fun TimePickerColors.periodSelectorContainerColor(selected: Boolean): Color =
    if (selected) periodSelectorSelectedContainerColor else periodSelectorUnselectedContainerColor

@OptIn(ExperimentalMaterial3Api::class)
internal fun TimePickerColors.periodSelectorContentColor(selected: Boolean): Color =
    if (selected) periodSelectorSelectedContentColor else periodSelectorUnselectedContentColor

@OptIn(ExperimentalMaterial3Api::class)
internal fun TimePickerColors.clockDialContentColor(selected: Boolean): Color =
    if (selected) clockDialSelectedContentColor else clockDialUnselectedContentColor

@OptIn(ExperimentalMaterial3Api::class)
internal val TimePickerState.hourForDisplay: Int
    get() = when {
        is24hour -> hour % 24
        hour % 12 == 0 -> 12
        isPm -> hour - 12
        else -> hour
    }

/**
 * Custom Material 3 [TimePicker] supporting minTime restriction (disabled/grayed-out hours & minutes).
 *
 * @param state state for this timepicker
 * @param modifier the [Modifier] to be applied to this time picker
 * @param colors colors [TimePickerColors] used for this time picker
 * @param layoutType layout configuration (horizontal or vertical)
 * @param minHour minimum allowed hour (0..23). Hours prior to this will be disabled/grayed out.
 * @param minMinute minimum allowed minute (0..59) when selected hour equals [minHour].
 * @param onDisabledTimeSelected lambda invoked when the user attempts to select a disabled time.
 */
@Composable
@ExperimentalMaterial3Api
fun TimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType(),
    minHour: Int? = null,
    minMinute: Int? = null,
    onDisabledTimeSelected: (() -> Unit)? = null,
) {
    val userOverride = remember { UserOverrideRef() }
    val analogState = remember(state) { AnalogTimePickerState(state, userOverride) }

    LaunchedEffect(minHour, minMinute) {
        if (minHour != null) {
            val minM = minMinute ?: 0
            if (state.hour < minHour || (state.hour == minHour && state.minute < minM)) {
                state.hour = minHour
                state.minute = minM
            }
        }
    }

    LaunchedEffect(state.hour, state.minute) {
        if (userOverride.value) {
            analogState.hour = state.hour
            analogState.minute = state.minute
        }
        userOverride.value = true
    }

    if (layoutType == TimePickerLayoutType.Vertical) {
        VerticalTimePicker(
            state = analogState,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = true,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
    } else {
        HorizontalTimePicker(
            state = analogState,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = true,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
    }
}

/**
 * Custom Material 3 [TimeInput] supporting minTime restriction.
 */
@Composable
@ExperimentalMaterial3Api
fun TimeInput(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    minHour: Int? = null,
    minMinute: Int? = null,
    onDisabledTimeSelected: (() -> Unit)? = null,
) {
    TimeInputImpl(
        modifier = modifier,
        colors = colors,
        state = state,
        minHour = minHour,
        minMinute = minMinute,
        onDisabledTimeSelected = onDisabledTimeSelected,
    )
}

internal fun isHourDisabled(hour24: Int, minHour: Int?): Boolean {
    if (minHour == null) return false
    return hour24 < minHour
}

internal fun isMinuteDisabled(
    currentHour24: Int,
    minute: Int,
    minHour: Int?,
    minMinute: Int?,
): Boolean {
    if (minHour == null) return false
    if (currentHour24 < minHour) return true
    if (currentHour24 > minHour) return false
    if (minMinute == null) return false
    return minute < minMinute
}

internal fun isPeriodAmDisabled(minHour: Int?): Boolean {
    if (minHour == null) return false
    return minHour >= 12
}

internal fun isPeriodPmDisabled(minHour: Int?): Boolean {
    if (minHour == null) return false
    return minHour > 23
}

@OptIn(ExperimentalMaterial3Api::class)
internal class AnalogTimePickerState(
    val state: TimePickerState,
    val userOverride: UserOverrideRef = UserOverrideRef(),
) : TimePickerState by state {

    var currentDiameter by mutableStateOf(0.dp)

    val currentAngle: Float
        get() = anim.value

    private var hourAngle = RadiansPerHour * (state.hour % 12) - FullCircle / 4
    private var minuteAngle = RadiansPerMinute * state.minute - FullCircle / 4

    suspend fun animateToCurrent(animationSpec: AnimationSpec<Float>) {
        if (!isUpdated()) {
            return
        }

        val end =
            if (selection == TimePickerSelectionMode.Hour) {
                endValueForAnimation(hourAngle)
            } else {
                endValueForAnimation(minuteAngle)
            }
        mutex.mutate(priority = MutatePriority.PreventUserInput) { anim.animateTo(end, animationSpec) }
    }

    private fun isUpdated(): Boolean {
        if (
            selection == TimePickerSelectionMode.Hour &&
                anim.targetValue.normalize() == hourAngle.normalize()
        ) {
            return false
        }

        if (
            selection == TimePickerSelectionMode.Minute &&
                anim.targetValue.normalize() == minuteAngle.normalize()
        ) {
            return false
        }

        return true
    }

    val clockFaceValues: IntList
        get() = if (selection == TimePickerSelectionMode.Minute) Minutes else Hours

    private fun endValueForAnimation(new: Float): Float {
        var diff = anim.value - new
        while (diff > HalfCircle) {
            diff -= FullCircle
        }
        while (diff <= -HalfCircle) {
            diff += FullCircle
        }

        return anim.value - diff
    }

    private var anim = Animatable(hourAngle)

    suspend fun onGestureEnd(animationSpec: AnimationSpec<Float>) {
        val end =
            endValueForAnimation(
                if (selection == TimePickerSelectionMode.Hour) {
                    hourAngle
                } else {
                    minuteAngle
                }
            )

        mutex.mutate(priority = MutatePriority.PreventUserInput) { anim.animateTo(end, animationSpec) }
    }

    suspend fun rotateTo(
        angle: Float,
        animationSpec: AnimationSpec<Float>,
        animate: Boolean = false,
    ) {
        userOverride.value = false
        mutex.mutate(MutatePriority.UserInput) {
            if (selection == TimePickerSelectionMode.Hour) {
                hourAngle = angle.toHour() % 12 * RadiansPerHour
                state.hour = hourAngle.toHour() % 12 + if (isPm) 12 else 0
            } else {
                minuteAngle = angle.toMinute() * RadiansPerMinute
                state.minute = minuteAngle.toMinute()
            }

            if (!animate) {
                anim.snapTo(offsetAngle(angle))
            } else {
                val endAngle = endValueForAnimation(offsetAngle(angle))
                anim.animateTo(endAngle, animationSpec)
            }
        }
    }

    override var minute: Int
        get() = state.minute
        set(value) {
            minuteAngle = RadiansPerMinute * value - FullCircle / 4
            state.minute = value
            if (selection == TimePickerSelectionMode.Minute) {
                anim = Animatable(minuteAngle)
            }
            updateBaseStateMinute()
        }

    private fun updateBaseStateMinute() = Snapshot.withoutReadObservation { state.minute = minute }

    override var hour: Int
        get() = state.hour
        set(value) {
            hourAngle = RadiansPerHour * (value % 12) - FullCircle / 4
            state.hour = value
            if (selection == TimePickerSelectionMode.Hour) {
                anim = Animatable(hourAngle)
            }
        }

    private fun Float.normalize(): Float {
        var normalizedAngle = this % (2 * PI)

        if (normalizedAngle < 0) {
            normalizedAngle += 2 * PI
        }

        return normalizedAngle.toFloat()
    }

    private val mutex = MutatorMutex()

    private fun offsetAngle(angle: Float): Float {
        val ret = angle + QuarterCircle.toFloat()
        return if (ret < 0) ret + FullCircle else ret
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun TimePickerState.moveSelector(x: Float, y: Float, maxDist: Float, center: IntOffset) {
    if (selection == TimePickerSelectionMode.Hour && is24hour) {
        val currentDist = dist(x, y, center.x, center.y)
        if (isPm) {
            hour -= if (currentDist >= maxDist) 12 else 0
        } else {
            hour += if (currentDist < maxDist) 12 else 0
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun AnalogTimePickerState.onTap(
    x: Float,
    y: Float,
    maxDist: Float,
    autoSwitchToMinute: Boolean,
    center: IntOffset,
    animationSpec: AnimationSpec<Float>,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    var angle = atan(y - center.y, x - center.x)
    if (selection == TimePickerSelectionMode.Minute) {
        angle = round(angle / RadiansPerMinute / 5f) * 5f * RadiansPerMinute
        val targetMinute = angle.toMinute()
        if (isMinuteDisabled(state.hour, targetMinute, minHour, minMinute)) {
            onDisabledTimeSelected?.invoke()
            return
        }
    } else {
        angle = round(angle / RadiansPerHour) * RadiansPerHour
        val hourOffset = angle.toHour() % 12
        val currentDist = dist(x, y, center.x, center.y)
        val isPmSelection = if (is24hour) {
            currentDist < maxDist
        } else {
            isPm
        }
        val targetHour = if (is24hour) {
            if (currentDist < maxDist) (hourOffset % 12 + 12) else (hourOffset % 12)
        } else {
            hourOffset % 12 + if (isPmSelection) 12 else 0
        }
        if (isMinuteDisabled(targetHour, state.minute, minHour, minMinute)) {
            onDisabledTimeSelected?.invoke()
            return
        }
    }

    moveSelector(x, y, maxDist, center)
    rotateTo(angle, animationSpec = animationSpec, animate = true)

    if (selection == TimePickerSelectionMode.Hour && autoSwitchToMinute) {
        delay(100)
        selection = TimePickerSelectionMode.Minute
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal val AnalogTimePickerState.selectorPos: DpOffset
    get() {
        val scale: Float = currentDiameter / ClockDialContainerSize
        val handleRadiusDp = (ClockDialSelectorHandleContainerSize / 2f) * scale
        val selectorLength =
            if (is24hour && this.isPm && selection == TimePickerSelectionMode.Hour) {
                    currentDiameter * InnerCircleToSizeRatio
                } else {
                    currentDiameter * OuterCircleToSizeRatio
                }
                .minus(handleRadiusDp)
                .coerceAtLeast(0.dp)

        val length = selectorLength + handleRadiusDp
        val offsetX = length * cos(currentAngle) + currentDiameter / 2
        val offsetY = length * sin(currentAngle) + currentDiameter / 2

        return DpOffset(offsetX, offsetY)
    }

@Composable
@ExperimentalMaterial3Api
internal fun VerticalTimePicker(
    state: AnalogTimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalClockDisplay(
            state = state,
            colors = colors,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
        Spacer(modifier = Modifier.height(ClockDisplayBottomMargin))
        ClockFace(
            modifier = Modifier.then(ClockFaceSizeModifier()),
            state = state,
            colors = colors,
            autoSwitchToMinute = autoSwitchToMinute,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
        Spacer(modifier = Modifier.height(ClockFaceBottomMargin))
    }
}

@Composable
@ExperimentalMaterial3Api
internal fun HorizontalTimePicker(
    state: AnalogTimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    Row(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalClockDisplay(
            state = state,
            colors = colors,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
        Spacer(modifier = Modifier.width(ClockDisplayBottomMargin))
        ClockFace(
            modifier = Modifier.then(ClockFaceSizeModifier()),
            state = state,
            colors = colors,
            autoSwitchToMinute = autoSwitchToMinute,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
    }
}

@Composable
@ExperimentalMaterial3Api
private fun TimeInputImpl(
    modifier: Modifier,
    colors: TimePickerColors,
    state: TimePickerState,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    fun hourTextValue() = TextFieldValue(state.hourForDisplay.toLocalString(minDigits = 2))
    fun minuteTextValue() = TextFieldValue(state.minute.toLocalString(minDigits = 2))

    var hourValue by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(hourTextValue()) }

    var minuteValue by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(minuteTextValue()) }

    val userOverride = remember { UserOverrideRef() }
    LaunchedEffect(state.hour, state.minute) {
        if (userOverride.value) {
            hourValue = hourTextValue()
            minuteValue = minuteTextValue()
        }
        userOverride.value = true
    }

    Row(
        modifier = modifier.padding(bottom = TimeInputBottomPadding),
        verticalAlignment = Alignment.Top,
    ) {
        val textStyle =
            MaterialTheme.typography.displayLarge.copy(
                textAlign = TextAlign.Center,
                color = colors.timeSelectorContentColor(true),
            )

        CompositionLocalProvider(
            LocalTextStyle provides textStyle,
            LocalLayoutDirection provides LayoutDirection.Ltr,
        ) {
            Row {
                TimePickerTextField(
                    modifier =
                        Modifier.onKeyEvent { event ->
                            val isDigit = event.key.keyCode in 7..16
                            val switchFocus =
                                isDigit &&
                                    hourValue.selection.start == 2 &&
                                    hourValue.text.length == 2

                            if (switchFocus) {
                                state.selection = TimePickerSelectionMode.Minute
                            }

                            false
                        },
                    value = hourValue,
                    onValueChange = { newValue ->
                        timeInputOnChange(
                            selection = TimePickerSelectionMode.Hour,
                            state = state,
                            value = newValue,
                            prevValue = hourValue,
                            max = if (state.is24hour) 23 else 12,
                            userOverride = userOverride,
                            minHour = minHour,
                            minMinute = minMinute,
                            onDisabledTimeSelected = onDisabledTimeSelected,
                        ) {
                            hourValue = it
                        }
                    },
                    state = state,
                    selection = TimePickerSelectionMode.Hour,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { state.selection = TimePickerSelectionMode.Minute }
                        ),
                    colors = colors,
                )
                DisplaySeparator(
                    Modifier.size(DisplaySeparatorWidth, PeriodSelectorContainerHeight)
                )
                TimePickerTextField(
                    modifier =
                        Modifier.onPreviewKeyEvent { event ->
                            val isBackspace = event.key == Key.Backspace
                            val switchFocus =
                                isBackspace && minuteValue.selection.start == 0

                            if (switchFocus) {
                                state.selection = TimePickerSelectionMode.Hour
                            }

                            switchFocus
                        },
                    value = minuteValue,
                    onValueChange = { newValue ->
                        timeInputOnChange(
                            selection = TimePickerSelectionMode.Minute,
                            state = state,
                            value = newValue,
                            prevValue = minuteValue,
                            max = 59,
                            userOverride = userOverride,
                            minHour = minHour,
                            minMinute = minMinute,
                            onDisabledTimeSelected = onDisabledTimeSelected,
                        ) {
                            minuteValue = it
                        }
                    },
                    state = state,
                    selection = TimePickerSelectionMode.Minute,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { state.selection = TimePickerSelectionMode.Minute }
                        ),
                    colors = colors,
                )
            }
        }

        if (!state.is24hour) {
            Box(Modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier =
                        Modifier.size(PeriodSelectorContainerWidth, PeriodSelectorContainerHeight),
                    state = state,
                    colors = colors,
                    minHour = minHour,
                    minMinute = minMinute,
                    onDisabledTimeSelected = onDisabledTimeSelected,
                )
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun HorizontalClockDisplay(
    state: TimePickerState,
    colors: TimePickerColors,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(top = PeriodToggleMargin)) {
                HorizontalPeriodToggle(
                    modifier =
                        Modifier.size(
                            PeriodSelectorHorizontalContainerWidth,
                            PeriodSelectorHorizontalContainerHeight,
                        ),
                    state = state,
                    colors = colors,
                    minHour = minHour,
                    minMinute = minMinute,
                    onDisabledTimeSelected = onDisabledTimeSelected,
                )
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun VerticalClockDisplay(
    state: TimePickerState,
    colors: TimePickerColors,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    Row(horizontalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier =
                        Modifier.size(
                            PeriodSelectorVerticalContainerWidth,
                            PeriodSelectorVerticalContainerHeight,
                        ),
                    state = state,
                    colors = colors,
                    minHour = minHour,
                    minMinute = minMinute,
                    onDisabledTimeSelected = onDisabledTimeSelected,
                )
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun ClockDisplayNumbers(state: TimePickerState, colors: TimePickerColors) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.displayLarge,
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Row {
            TimeSelector(
                modifier = Modifier.size(TimeSelectorContainerWidth, TimeSelectorContainerHeight),
                value = state.hourForDisplay,
                state = state,
                selection = TimePickerSelectionMode.Hour,
                colors = colors,
            )
            DisplaySeparator(
                Modifier.size(DisplaySeparatorWidth, PeriodSelectorVerticalContainerHeight)
            )
            TimeSelector(
                modifier = Modifier.size(TimeSelectorContainerWidth, TimeSelectorContainerHeight),
                value = state.minute,
                state = state,
                selection = TimePickerSelectionMode.Minute,
                colors = colors,
            )
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun HorizontalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
            val spacerPlaceable =
                spacer.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = PeriodSelectorOutlineWidth.roundToPx(),
                    )
                )

            val items =
                measurables
                    .fastFilter { it.layoutId != "Spacer" }
                    .fastMap { item ->
                        item.measure(
                            constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth / 2)
                        )
                    }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(items[0].width, 0)
                spacerPlaceable.place(items[0].width - spacerPlaceable.width / 2, 0)
            }
        }
    }

    val shape = RoundedCornerShape(8.dp)

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.start(),
        endShape = shape.end(),
        minHour = minHour,
        minMinute = minMinute,
        onDisabledTimeSelected = onDisabledTimeSelected,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun VerticalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
            val spacerPlaceable =
                spacer.measure(
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = PeriodSelectorOutlineWidth.roundToPx(),
                    )
                )

            val items =
                measurables
                    .fastFilter { it.layoutId != "Spacer" }
                    .fastMap { item ->
                        item.measure(
                            constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight / 2)
                        )
                    }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(0, items[0].height)
                spacerPlaceable.place(0, items[0].height - spacerPlaceable.height / 2)
            }
        }
    }

    val shape = RoundedCornerShape(8.dp)

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.top(),
        endShape = shape.bottom(),
        minHour = minHour,
        minMinute = minMinute,
        onDisabledTimeSelected = onDisabledTimeSelected,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun PeriodToggleImpl(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
    measurePolicy: MeasurePolicy,
    startShape: Shape,
    endShape: Shape,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    val borderStroke =
        BorderStroke(PeriodSelectorOutlineWidth, colors.periodSelectorBorderColor)
    val shape = RoundedCornerShape(8.dp)
    val amDisabled = isPeriodAmDisabled(minHour)
    val pmDisabled = isPeriodPmDisabled(minHour)

    Layout(
        modifier =
            modifier
                .semantics {
                    isTraversalGroup = true
                    contentDescription = "AM/PM Toggle"
                }
                .selectableGroup()
                .border(border = borderStroke, shape = shape),
        measurePolicy = measurePolicy,
        content = {
            ToggleItem(
                checked = !state.isPm,
                isDisabled = amDisabled,
                shape = startShape,
                onClick = {
                    if (amDisabled) {
                        onDisabledTimeSelected?.invoke()
                    } else if (state.isPm) {
                        val targetHour = state.hour - 12
                        if (isMinuteDisabled(targetHour, state.minute, minHour, minMinute)) {
                            onDisabledTimeSelected?.invoke()
                        } else {
                            state.hour -= 12
                        }
                    }
                },
                colors = colors,
            ) {
                Text(text = "AM")
            }
            Spacer(
                Modifier.layoutId("Spacer")
                    .zIndex(SeparatorZIndex)
                    .fillMaxSize()
                    .background(color = colors.periodSelectorBorderColor)
            )
            ToggleItem(
                checked = state.isPm,
                isDisabled = pmDisabled,
                shape = endShape,
                onClick = {
                    if (pmDisabled) {
                        onDisabledTimeSelected?.invoke()
                    } else if (!state.isPm) {
                        val targetHour = state.hour + 12
                        if (isMinuteDisabled(targetHour, state.minute, minHour, minMinute)) {
                            onDisabledTimeSelected?.invoke()
                        } else {
                            state.hour += 12
                        }
                    }
                },
                colors = colors,
            ) {
                Text("PM")
            }
        },
    )
}

@Composable
@ExperimentalMaterial3Api
private fun ToggleItem(
    checked: Boolean,
    isDisabled: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    colors: TimePickerColors,
    content: @Composable RowScope.() -> Unit,
) {
    val rawContentColor = colors.periodSelectorContentColor(checked)
    val rawContainerColor = colors.periodSelectorContainerColor(checked)
    val contentColor = if (isDisabled) rawContentColor.copy(alpha = 0.38f) else rawContentColor
    val containerColor = if (isDisabled) rawContainerColor.copy(alpha = 0.38f) else rawContainerColor

    TextButton(
        modifier =
            Modifier.zIndex(if (checked) 0f else 1f).fillMaxSize().semantics { selected = checked },
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        onClick = onClick,
        content = content,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = contentColor,
                containerColor = containerColor,
            ),
    )
}

@Composable
private fun DisplaySeparator(modifier: Modifier) {
    val style =
        LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
        )

    Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(text = ":", color = MaterialTheme.colorScheme.onSurface, style = style)
    }
}

@Composable
@ExperimentalMaterial3Api
private fun TimeSelector(
    modifier: Modifier,
    value: Int,
    state: TimePickerState,
    selection: TimePickerSelectionMode,
    colors: TimePickerColors,
) {
    val selected = state.selection == selection
    val selectorContentDescription =
        if (selection == TimePickerSelectionMode.Hour) "Select Hour" else "Select Minute"

    val containerColor = colors.timeSelectorContainerColor(selected)
    val contentColor = colors.timeSelectorContentColor(selected)
    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                role = Role.RadioButton
                contentDescription = selectorContentDescription
            },
        onClick = {
            if (selection != state.selection) {
                state.selection = selection
            }
        },
        selected = selected,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        val valueContentDescription =
            numberContentDescription(
                selection = selection,
                is24Hour = state.is24hour,
                number = value,
            )

        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.semantics { contentDescription = valueContentDescription },
                text = value.toLocalString(minDigits = 2),
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class ClockDialModifier(
    private val state: AnalogTimePickerState,
    private val autoSwitchToMinute: Boolean,
    private val selection: TimePickerSelectionMode,
    private val animationSpec: AnimationSpec<Float>,
    private val minHour: Int?,
    private val minMinute: Int?,
    private val onDisabledTimeSelected: (() -> Unit)?,
) : ModifierNodeElement<ClockDialNode>() {

    override fun create(): ClockDialNode =
        ClockDialNode(
            state = state,
            autoSwitchToMinute = autoSwitchToMinute,
            selection = selection,
            animationSpec = animationSpec,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )

    override fun update(node: ClockDialNode) {
        node.updateNode(
            state = state,
            autoSwitchToMinute = autoSwitchToMinute,
            selection = selection,
            animationSpec = animationSpec,
            minHour = minHour,
            minMinute = minMinute,
            onDisabledTimeSelected = onDisabledTimeSelected,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ClockDialModifier"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class ClockDialNode(
    private var state: AnalogTimePickerState,
    private var autoSwitchToMinute: Boolean,
    private var selection: TimePickerSelectionMode,
    private var animationSpec: AnimationSpec<Float>,
    private var minHour: Int?,
    private var minMinute: Int?,
    private var onDisabledTimeSelected: (() -> Unit)?,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode,
    LayoutAwareModifierNode {

    private var offsetX = 0f
    private var offsetY = 0f
    private var center: IntOffset by mutableStateOf(IntOffset.Zero)
    private val maxDist
        get() =
            with(requireDensity()) {
                MaxDistance.toPx() * state.currentDiameter.roundToPx() /
                    ClockDialContainerSize.roundToPx()
            }

    private val pointerInputTapNode =
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onPress = {
                        offsetX = it.x
                        offsetY = it.y
                    },
                    onTap = {
                        coroutineScope.launch {
                            state.onTap(
                                x = it.x,
                                y = it.y,
                                maxDist = maxDist,
                                autoSwitchToMinute = autoSwitchToMinute,
                                center = center,
                                animationSpec = animationSpec,
                                minHour = minHour,
                                minMinute = minMinute,
                                onDisabledTimeSelected = onDisabledTimeSelected,
                            )
                        }
                    },
                )
            }
        )

    private val pointerInputDragNode =
        delegate(
            SuspendingPointerInputModifierNode {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (autoSwitchToMinute) {
                                state.selection = TimePickerSelectionMode.Minute
                            }
                            state.onGestureEnd(animationSpec)
                        }
                    }
                ) { _, dragAmount ->
                    coroutineScope.launch {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        val angle = atan(offsetY - center.y, offsetX - center.x)

                        if (selection == TimePickerSelectionMode.Hour) {
                            val hourOffset = angle.toHour() % 12
                            val isPmSelection = if (state.is24hour) (dist(offsetX, offsetY, center.x, center.y) < maxDist) else state.isPm
                            val targetHour = if (state.is24hour) {
                                if (dist(offsetX, offsetY, center.x, center.y) < maxDist) (hourOffset % 12 + 12) else (hourOffset % 12)
                            } else {
                                hourOffset % 12 + if (isPmSelection) 12 else 0
                            }
                            if (isMinuteDisabled(targetHour, state.minute, minHour, minMinute)) {
                                onDisabledTimeSelected?.invoke()
                                return@launch
                            }
                        } else {
                            val targetMinute = angle.toMinute()
                            if (isMinuteDisabled(state.hour, targetMinute, minHour, minMinute)) {
                                onDisabledTimeSelected?.invoke()
                                return@launch
                            }
                        }

                        state.rotateTo(angle, animationSpec)
                        state.moveSelector(
                            x = offsetX,
                            y = offsetY,
                            maxDist = maxDist,
                            center = center,
                        )
                    }
                }
            }
        )

    override fun onRemeasured(size: IntSize) {
        center = IntOffset(size.width / 2, size.height / 2)
        state.currentDiameter = with(requireDensity()) { size.width.toDp() }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        pointerInputTapNode.onPointerEvent(pointerEvent, pass, bounds)
        pointerInputDragNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputTapNode.onCancelPointerInput()
        pointerInputDragNode.onCancelPointerInput()
    }

    fun updateNode(
        state: AnalogTimePickerState,
        autoSwitchToMinute: Boolean,
        selection: TimePickerSelectionMode,
        animationSpec: AnimationSpec<Float>,
        minHour: Int?,
        minMinute: Int?,
        onDisabledTimeSelected: (() -> Unit)?,
    ) {
        this.state = state
        this.autoSwitchToMinute = autoSwitchToMinute
        this.animationSpec = animationSpec
        this.minHour = minHour
        this.minMinute = minMinute
        this.onDisabledTimeSelected = onDisabledTimeSelected
        if (this.selection != selection) {
            this.selection = selection
            coroutineScope.launch { state.animateToCurrent(animationSpec) }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
internal fun ClockFace(
    modifier: Modifier,
    state: AnalogTimePickerState,
    colors: TimePickerColors,
    autoSwitchToMinute: Boolean,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    Crossfade(
        modifier =
            modifier
                .background(shape = CircleShape, color = colors.clockDialColor)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .then(
                    ClockDialModifier(
                        state = state,
                        autoSwitchToMinute = autoSwitchToMinute,
                        selection = state.selection,
                        animationSpec = tween(300),
                        minHour = minHour,
                        minMinute = minMinute,
                        onDisabledTimeSelected = onDisabledTimeSelected,
                    )
                )
                .drawSelector(state, colors),
        targetState = state.clockFaceValues,
        animationSpec = tween(300),
    ) { screen ->
        CircularLayout(
            modifier = Modifier.fillMaxSize().semantics { selectableGroup() },
            radiusToSizeRatio = OuterCircleToSizeRatio,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.clockDialContentColor(false)
            ) {
                repeat(screen.size) { index ->
                    val outerValue =
                        if (!state.is24hour || state.selection == TimePickerSelectionMode.Minute) {
                            screen[index]
                        } else {
                            screen[index] % 12
                        }

                    val isDisabled = if (state.selection == TimePickerSelectionMode.Hour) {
                        val h24 = if (state.is24hour) {
                            screen[index] % 24
                        } else {
                            (screen[index] % 12) + (if (state.isPm) 12 else 0)
                        }
                        isHourDisabled(h24, minHour)
                    } else {
                        isMinuteDisabled(state.hour, screen[index], minHour, minMinute)
                    }

                    ClockText(
                        modifier = Modifier.semantics { traversalIndex = index.toFloat() + 1f },
                        state = state,
                        value = outerValue,
                        autoSwitchToMinute = autoSwitchToMinute,
                        colors = colors,
                        isDisabled = isDisabled,
                        minHour = minHour,
                        minMinute = minMinute,
                        onDisabledTimeSelected = onDisabledTimeSelected,
                    )
                }

                if (state.selection == TimePickerSelectionMode.Hour && state.is24hour) {
                    CircularLayout(
                        modifier =
                            Modifier.layoutId(LayoutId.InnerCircle)
                                .fillMaxSize()
                                .background(shape = CircleShape, color = Color.Transparent),
                        radiusToSizeRatio = InnerCircleToSizeRatio,
                    ) {
                        repeat(ExtraHours.size) { index ->
                            val innerValue = ExtraHours[index]
                            val isDisabled = isHourDisabled(innerValue, minHour)

                            ClockText(
                                modifier =
                                    Modifier.semantics { traversalIndex = 12 + index.toFloat() },
                                state = state,
                                value = innerValue,
                                autoSwitchToMinute = autoSwitchToMinute,
                                colors = colors,
                                isDisabled = isDisabled,
                                minHour = minHour,
                                minMinute = minMinute,
                                onDisabledTimeSelected = onDisabledTimeSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
private fun Modifier.drawSelector(
    state: AnalogTimePickerState,
    colors: TimePickerColors,
): Modifier =
    this.drawWithContent {
        val selectorOffsetPx = Offset(state.selectorPos.x.toPx(), state.selectorPos.y.toPx())

        val selectorRadius =
            ClockDialSelectorHandleContainerSize.toPx() / 2f * state.currentDiameter.roundToPx() /
                ClockDialContainerSize.roundToPx()
        val selectorColor = colors.selectorColor

        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = Color.Black,
            blendMode = BlendMode.Clear,
        )

        drawContent()

        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = selectorColor,
            blendMode = BlendMode.Xor,
        )

        val strokeWidth = ClockDialSelectorTrackContainerWidth.toPx()
        val lineLength =
            selectorOffsetPx.minus(
                Offset(
                    (selectorRadius * cos(state.currentAngle)),
                    (selectorRadius * sin(state.currentAngle)),
                )
            )

        drawLine(
            start = Offset(size.width / 2, size.height / 2),
            strokeWidth = strokeWidth,
            end = lineLength,
            color = selectorColor,
            blendMode = BlendMode.SrcOver,
        )

        drawCircle(
            radius = ClockDialSelectorCenterContainerSize.toPx() / 2,
            center = Offset(size.width / 2, size.height / 2),
            color = selectorColor,
        )

        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = colors.clockDialContentColor(selected = true),
            blendMode = BlendMode.DstOver,
        )
    }

@Composable
@ExperimentalMaterial3Api
private fun ClockText(
    modifier: Modifier,
    state: AnalogTimePickerState,
    value: Int,
    autoSwitchToMinute: Boolean,
    colors: TimePickerColors,
    isDisabled: Boolean,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
) {
    val style = MaterialTheme.typography.bodyLarge
    val density: Density = LocalDensity.current
    val maxDist = with(density) { MaxDistance.toPx() }
    var center by remember { mutableStateOf(Offset.Zero) }
    var parentCenter by remember { mutableStateOf(IntOffset.Zero) }
    var boundsInParent by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()

    val text = value.toLocalString()
    val selected by
        remember(state) {
            derivedStateOf {
                val selectorPos = state.selectorPos
                val offset = with(density) { Offset(selectorPos.x.toPx(), selectorPos.y.toPx()) }
                boundsInParent.contains(offset)
            }
        }

    val textColor = colors.clockDialContentColor(selected)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .graphicsLayer {
                    alpha = if (isDisabled) 0.38f else 1.0f
                }
                .onGloballyPositioned {
                    parentCenter = it.parentCoordinates?.size?.let { s -> IntOffset(s.width / 2, s.height / 2) } ?: IntOffset.Zero
                    boundsInParent = it.boundsInParent()
                    center = boundsInParent.center
                }
                .minimumInteractiveComponentSize()
                .size(MinimumInteractiveSize)
                .focusable()
                .semantics(mergeDescendants = true) {
                    onClick {
                        if (isDisabled) {
                            onDisabledTimeSelected?.invoke()
                        } else {
                            scope.launch {
                                state.onTap(
                                    x = center.x,
                                    y = center.y,
                                    maxDist = maxDist,
                                    autoSwitchToMinute = autoSwitchToMinute,
                                    center = parentCenter,
                                    animationSpec = SnapSpec(),
                                    minHour = minHour,
                                    minMinute = minMinute,
                                    onDisabledTimeSelected = onDisabledTimeSelected,
                                )
                            }
                        }
                        true
                    }
                    this.selected = selected
                },
    ) {
        Text(
            modifier =
                Modifier.clearAndSetSemantics {
                    contentDescription = "$value"
                },
            text = text,
            style = style,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun timeInputOnChange(
    selection: TimePickerSelectionMode,
    state: TimePickerState,
    value: TextFieldValue,
    prevValue: TextFieldValue,
    max: Int,
    userOverride: UserOverrideRef,
    minHour: Int?,
    minMinute: Int?,
    onDisabledTimeSelected: (() -> Unit)?,
    onNewValue: (value: TextFieldValue) -> Unit,
) {
    userOverride.value = false
    if (value.text == prevValue.text) {
        onNewValue(value)
        return
    }

    if (value.text.isEmpty()) {
        if (selection == TimePickerSelectionMode.Hour) {
            val targetHour = if (state.isPm && !state.is24hour) 12 else 0
            if (isHourDisabled(targetHour, minHour)) {
                onDisabledTimeSelected?.invoke()
                return
            }
            state.hour = targetHour
        } else {
            if (isMinuteDisabled(state.hour, 0, minHour, minMinute)) {
                onDisabledTimeSelected?.invoke()
                return
            }
            state.minute = 0
        }
        onNewValue(value.copy(text = ""))
        return
    }

    try {
        val newValue =
            if (value.text.length == 3 && value.selection.start == 1) {
                value.text[0].digitToInt()
            } else {
                value.text.toInt()
            }

        if (newValue <= max) {
            if (selection == TimePickerSelectionMode.Hour) {
                val targetHour =
                    if (newValue == 12 && state.isPm) {
                        12
                    } else if (newValue == 12 && !state.isPm && !state.is24hour) {
                        0
                    } else {
                        newValue + if (state.isPm && !state.is24hour) 12 else 0
                    }

                val isPotentialPrefix = if (state.is24hour) {
                    (newValue == 0 && (minHour ?: 0) <= 9) ||
                    (newValue == 1 && (minHour ?: 0) <= 19) ||
                    (newValue == 2 && (minHour ?: 0) <= 23)
                } else {
                    newValue == 1
                }

                if (isHourDisabled(targetHour, minHour) && !isPotentialPrefix) {
                    onDisabledTimeSelected?.invoke()
                    return
                }

                state.hour = targetHour
                if (newValue > 1 && !state.is24hour) {
                    state.selection = TimePickerSelectionMode.Minute
                }
            } else {
                if (isMinuteDisabled(state.hour, newValue, minHour, minMinute)) {
                    onDisabledTimeSelected?.invoke()
                    return
                }
                state.minute = newValue
            }

            onNewValue(
                if (value.text.length <= 2) {
                    value
                } else {
                    value.copy(text = value.text[0].toString())
                }
            )
        }
    } catch (_: NumberFormatException) {
    } catch (_: IllegalArgumentException) {
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerTextField(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    state: TimePickerState,
    selection: TimePickerSelectionMode,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TimePickerColors,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val textFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.timeSelectorContainerColor(true),
            unfocusedContainerColor = colors.timeSelectorContainerColor(true),
            focusedTextColor = colors.timeSelectorContentColor(true),
        )
    val selected = selection == state.selection
    Column(modifier = modifier) {
        if (!selected) {
            TimeSelector(
                modifier = Modifier.size(TimeFieldContainerWidth, TimeFieldContainerHeight),
                value =
                    if (selection == TimePickerSelectionMode.Hour) {
                        state.hourForDisplay
                    } else {
                        state.minute
                    },
                state = state,
                selection = selection,
                colors = colors,
            )
        }

        val contentDescription =
            if (selection == TimePickerSelectionMode.Minute) "Minute text field" else "Hour text field"

        Box(Modifier.visible(selected)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .size(TimeFieldContainerWidth, TimeFieldContainerHeight)
                        .semantics { this.contentDescription = contentDescription },
                interactionSource = interactionSource,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = LocalTextStyle.current,
                enabled = true,
                singleLine = true,
                cursorBrush =
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.10f to Color.Transparent,
                        0.10f to MaterialTheme.colorScheme.primary,
                        0.90f to MaterialTheme.colorScheme.primary,
                        0.90f to Color.Transparent,
                        1.00f to Color.Transparent,
                    ),
            ) {
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = it,
                    singleLine = true,
                    colors = textFieldColors,
                    enabled = true,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(0.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            shape = RoundedCornerShape(8.dp),
                            colors = textFieldColors,
                        )
                    },
                )
            }
        }

        Text(
            modifier = Modifier.offset(y = SupportLabelTop).clearAndSetSemantics {},
            text = if (selection == TimePickerSelectionMode.Hour) "Hour" else "Minute",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    LaunchedEffect(state.selection) {
        if (state.selection == selection) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun CircularLayout(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) radiusToSizeRatio: Float,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val radiusPx = constraints.maxHeight * radiusToSizeRatio
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables =
            measurables
                .fastFilter {
                    it.layoutId != LayoutId.Selector && it.layoutId != LayoutId.InnerCircle
                }
                .fastMap { measurable -> measurable.measure(itemConstraints) }
        val selectorMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.Selector }
        val innerMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.InnerCircle }
        val theta = if (placeables.isNotEmpty()) FullCircle / (placeables.count()) else 0f
        val selectorPlaceable = selectorMeasurable?.measure(itemConstraints)
        val innerCirclePlaceable = innerMeasurable?.measure(itemConstraints)

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            selectorPlaceable?.place(0, 0)

            placeables.fastForEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
                it.place(x = offsetX.roundToInt(), y = offsetY.roundToInt())
            }

            innerCirclePlaceable?.place(
                (constraints.maxWidth - innerCirclePlaceable.width) / 2,
                (constraints.maxHeight - innerCirclePlaceable.height) / 2,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
@ExperimentalMaterial3Api
internal fun numberContentDescription(
    selection: TimePickerSelectionMode,
    is24Hour: Boolean,
    number: Int,
): String {
    val unit = if (selection == TimePickerSelectionMode.Minute) "minutes" else "o'clock"
    return "$number $unit"
}

private fun dist(x1: Float, y1: Float, x2: Int, y2: Int): Float {
    val x = x2 - x1
    val y = y2 - y1
    return hypot(x.toDouble(), y.toDouble()).toFloat()
}

private fun atan(y: Float, x: Float): Float {
    val ret = atan2(y, x) - QuarterCircle.toFloat()
    return if (ret < 0) ret + FullCircle else ret
}

private fun Int.toLocalString(minDigits: Int = 1): String {
    return this.toString().padStart(minDigits, '0')
}

private fun CornerBasedShape.start(): CornerBasedShape =
    copy(topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

private fun CornerBasedShape.end(): CornerBasedShape =
    copy(topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp))

private fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

private fun CornerBasedShape.bottom(): CornerBasedShape =
    copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))

private enum class LayoutId {
    Selector,
    InnerCircle,
}

@Stable
private fun Modifier.visible(visible: Boolean) =
    this.then(
        VisibleModifier(
            visible
        )
    )

private class VisibleModifier(val visible: Boolean) : LayoutModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        if (!visible) {
            return layout(0, 0) {}
        }
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun hashCode(): Int = visible.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? VisibleModifier ?: return false
        return visible == otherModifier.visible
    }
}

internal class ClockFaceSizeModifier : LayoutModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val max = constraints.maxHeight.toDp()
        val size =
            when {
                max >= TimePickerMaxHeight -> ClockDialContainerSize
                max >= TimePickerMidHeight -> ClockDialMidContainerSize
                else -> ClockDialMinContainerSize
            }.roundToPx()

        val placeable = measurable.measure(Constraints.fixed(size, size))
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
