package MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerUiState
import com.example.MultipleAlarmClock.Ui.alarmPicker.Progress
import java.text.SimpleDateFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerWithoutDialog(
	state: TimePickerState,
	modifier: Modifier = Modifier,
	isCandidateInvalid: Boolean = false,
	uiState: AlarmPickerUiState,
) {

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(vertical = 16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		TimePicker(state = state)

		AnimatedVisibility(visible = isCandidateInvalid) {
			val locale = LocalLocale.current.platformLocale
			val startTimeString = remember(uiState.alarmObject.startTime, locale) {
				SimpleDateFormat("h:mm a", locale).format(uiState.alarmObject.startTime.time)
			}
			Text(
				text =  stringResource(R.string.alarm_error_time_range, startTimeString ),
				style = typography.bodyMedium,
				color = colorScheme.error,
				modifier = Modifier.padding(top = 16.dp),
				textAlign = TextAlign.Center
			)
		}
	}
}



@Composable
fun LinearProgressForNewAlarm(modifier: Modifier = Modifier, progress: Progress) {
	val (step, total) = when (progress) {
		Progress.StartTime -> 1 to 3
		Progress.EndTime -> 2 to 3
		Progress.FullEditor -> 3 to 3
	}

	// Add the Material 3 animation spec here to make it glide smoothly
	val progressFraction by animateFloatAsState(
		targetValue = step / total.toFloat(),
		animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
		label = "progress_animation"
	)

	Column(
		modifier = modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "Step $step of $total",
				style = typography.labelSmall,
				fontWeight = FontWeight.Normal,
				color = colorScheme.onSurface
			)
		}
		Spacer(modifier = Modifier.height(8.dp))
		LinearProgressIndicator(
			progress = { progressFraction },
			modifier = Modifier.fillMaxWidth().height(4.dp),
			color = colorScheme.secondary,
			trackColor = colorScheme.surfaceContainerLow,
			strokeCap = StrokeCap.Round
		)
	}
}
