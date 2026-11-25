package com.coolApps.MultipleAlarmClock.presentation.onboarding.v4.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolApps.MultipleAlarmClock.presentation.onboarding.ButtonState
import com.coolApps.MultipleAlarmClock.presentation.onboarding.v4.OnboardingPersonalization
import com.coolApps.MultipleAlarmClock.presentation.picker.TimePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpTimeScreen(
	initialHour: Int,
	initialMinute: Int,
	personalization: OnboardingPersonalization,
	onTimeChanged: (hour: Int, minute: Int) -> Unit,
	onButtonStateChange: (ButtonState) -> Unit,
	modifier: Modifier = Modifier,
) {
	val view = LocalView.current
	val timePickerState = rememberTimePickerState(
		initialHour = initialHour,
		initialMinute = initialMinute,
		is24Hour = false,
	)

	LaunchedEffect(Unit) {
		onButtonStateChange(ButtonState.Enabled)
	}

	var isFirstRun by remember { mutableStateOf(true) }
	LaunchedEffect(timePickerState.hour, timePickerState.minute) {
		if (isFirstRun) {
			isFirstRun = false
		} else {
			view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
		}
		onTimeChanged(timePickerState.hour, timePickerState.minute)
	}

	BoxWithConstraints(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		val isWideLayout = maxWidth >= 600.dp
		val pickerLayoutType = if (maxWidth >= 520.dp && maxHeight < 600.dp) {
			TimePickerLayoutType.Horizontal
		} else {
			TimePickerLayoutType.Vertical
		}

		if (isWideLayout) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 24.dp)
					.padding(top = 12.dp, bottom = 128.dp),
				contentAlignment = Alignment.Center,
			) {
				Row(
					modifier = Modifier
						.widthIn(max = 920.dp)
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						text = stringResource(personalization.wakeUpTitleRes),
						style = typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
						color = colorScheme.onBackground,
						textAlign = TextAlign.Start,
						modifier = Modifier.weight(0.85f),
					)
					Box(
						modifier = Modifier.weight(1.35f),
						contentAlignment = Alignment.Center,
					) {
						InlineTimePickerCard(
							state = timePickerState,
							layoutType = pickerLayoutType,
						)
					}
				}
			}
		} else {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 24.dp)
					.padding(top = 12.dp, bottom = 132.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
			) {
				Text(
					text = stringResource(personalization.wakeUpTitleRes),
					style = typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
					color = colorScheme.onBackground,
					textAlign = TextAlign.Center,
				)
				Spacer(modifier = Modifier.height(24.dp))
				InlineTimePickerCard(
					state = timePickerState,
					layoutType = pickerLayoutType,
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineTimePickerCard(
	state: TimePickerState,
	layoutType: TimePickerLayoutType,
) {
	Surface(
		shape = RoundedCornerShape(28.dp),
		color = colorScheme.surfaceContainerLow,
		modifier = Modifier.widthIn(max = 520.dp),
	) {
		Box(
			modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
			contentAlignment = Alignment.Center,
		) {
			TimePicker(
				state = state,
				layoutType = layoutType,
			)
		}
	}
}
