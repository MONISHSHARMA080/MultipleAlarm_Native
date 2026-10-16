package com.coolApps.MultipleAlarmClock.presentation.picker

import android.view.HapticFeedbackConstants
import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import java.text.SimpleDateFormat


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimePickerWithoutDialog(
		state: TimePickerState,
		modifier: Modifier = Modifier,
		isCandidateInvalid: Boolean = false,
		uiState: AlarmPickerUiState,
		@IntRange(from = 0, to = 23) minHour:Int? = null,
		@IntRange(from = 0, to = 59) minMin:Int? = null,
		onDisabledTimeSelected:()->Unit
) {
	val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
	val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(vertical = 16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		val view = LocalView.current
		var isFirstRun by remember { mutableStateOf(true) }
		LaunchedEffect(state.hour, state.minute) {
			if (isFirstRun) {
				isFirstRun = false
			} else {
				view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
			}
		}

		Surface(
			shape = RoundedCornerShape(28.dp),
			color = colorScheme.surfaceContainerLow,
			modifier = Modifier.widthIn(max = 420.dp)
		) {
			Box(
				modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
				contentAlignment = Alignment.Center
			) {
				TimePicker(
					state = state,
					minHour = minHour,
					minMinute = minMin,
					onDisabledTimeSelected = onDisabledTimeSelected
				)
			}
		}

		AnimatedVisibility(
			visible = isCandidateInvalid,
			enter = expandVertically(spatialSpec) + fadeIn(effectsSpec),
			exit = shrinkVertically(spatialSpec) + fadeOut(effectsSpec)
		) {
			val locale = LocalLocale.current.platformLocale
			val startTimeString = remember(uiState.alarmData.startTime, locale) {
				SimpleDateFormat("h:mm a", locale).format(uiState.alarmData.startTime)
			}
			Surface(
				shape = RoundedCornerShape(16.dp),
				color = colorScheme.errorContainer,
				modifier = Modifier.padding(top = 16.dp)
			) {
				Text(
					text = stringResource(R.string.alarm_error_fix_alarm_time, startTimeString),
					style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
					color = colorScheme.onErrorContainer,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
					textAlign = TextAlign.Center
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinearProgressForNewAlarm(modifier: Modifier = Modifier, progress: Progress) {
	val (step, total) = when (progress) {
		Progress.StartTime -> 1 to 3
		Progress.EndTime -> 2 to 3
		Progress.FullEditor -> 3 to 3
	}

	val progressFraction by animateFloatAsState(
		targetValue = step / total.toFloat(),
		animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
		label = "progress_animation"
	)

	Column(
		modifier = modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(8.dp))
		LinearProgressIndicator(
			progress = { progressFraction },
			modifier = Modifier.fillMaxWidth().height(6.dp),
			color = colorScheme.primary,
			trackColor = colorScheme.surfaceContainerHighest,
			strokeCap = StrokeCap.Round
		)
	}
}
