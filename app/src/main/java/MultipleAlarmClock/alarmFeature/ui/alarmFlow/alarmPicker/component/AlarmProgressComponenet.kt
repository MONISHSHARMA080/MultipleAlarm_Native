package MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.R
import com.example.MultipleAlarmClock.Ui.alarmPicker.Progress


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerWithoutDialog(
	state: TimePickerState,
	modifier: Modifier = Modifier,
	isCandidateInvalid: Boolean = false,
	errorMessage: String? = null,
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
			Text(
				text = errorMessage ?: stringResource(R.string.alarm_error_time_range),
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
	val (step, total) =
		when (progress) {
			Progress.StartTime -> 1 to 3
			Progress.EndTime -> 2 to 3
			Progress.FullEditor -> 3 to 3
		}
	val progressFraction = step / total.toFloat()

	Column(
		modifier =
			modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 24.dp),
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
			modifier = Modifier.fillMaxWidth().height(5.dp).animateContentSize(),
			color = colorScheme.secondary,
			trackColor = colorScheme.surfaceContainerLow,
			strokeCap = StrokeCap.Round
		)
	}
}

