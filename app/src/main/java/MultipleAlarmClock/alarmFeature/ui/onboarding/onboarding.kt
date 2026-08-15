package MultipleAlarmClock.alarmFeature.ui.onboarding

import MultipleAlarmClock.alarmFeature.ui.onboarding.components.AlarmResultClaude
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.AlarmResultOpenAi
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.PermissionScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.ProblemScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker.AlarmPickerScreen
import com.example.MultipleAlarmClock.Ui.alarmPicker.AlarmPickerViewModel
import com.example.MultipleAlarmClock.Ui.alarmPicker.listAlarmRingtone.ListAlarmScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen() {
	val viewModel : OnboardingViewModel = hiltViewModel()
	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val missingSteps by viewModel.missingSteps.collectAsStateWithLifecycle()
	val allCriticalGranted by viewModel.allCriticalGranted.collectAsStateWithLifecycle()
	var showAlarmSoundList by remember { mutableStateOf(false) }

	when(uiState.displaySate){
//		DisplaySate.Greeting -> AlarmResultClaude(uiState.alarmData, onNextClick = {viewModel.onNextClicked()  } )
		DisplaySate.Greeting -> AlarmResultOpenAi (uiState.alarmData, onNextClick = {viewModel.onPreviousClicked()})
//		DisplaySate.Greeting -> GreetingScreen(onClickNext = {viewModel.onNextClicked()} )
		DisplaySate.Problem -> ProblemScreen { viewModel.onNextClicked() }
		DisplaySate.Permission -> {
			PermissionScreen(
				missingSteps = missingSteps,
				allCriticalGranted = allCriticalGranted,
				onNext = { viewModel.onNextClicked() },
				refreshPermissionUiState = {viewModel.refreshPermissions()},
			)
		}
		DisplaySate.CreateFirstAlarm -> {
			val alarmPickerViewModel : AlarmPickerViewModel = hiltViewModel<AlarmPickerViewModel, AlarmPickerViewModel.Factory> { factory -> factory.create(null) }
			val selected by alarmPickerViewModel.selectedAlarmSound.collectAsStateWithLifecycle()
			val previewing by alarmPickerViewModel.previewingSound.collectAsStateWithLifecycle()

			AnimatedContent(
				targetState = showAlarmSoundList,
				transitionSpec = {
					if (targetState) {
						// Going forward
						slideIntoContainer(
							towards = AnimatedContentTransitionScope.SlideDirection.Left,
							animationSpec = tween(150, easing = FastOutSlowInEasing)
						) togetherWith slideOutOfContainer(
									towards = AnimatedContentTransitionScope.SlideDirection.Left,
									animationSpec = tween(150, easing = FastOutSlowInEasing)
								)
					} else {
						// Going back
						slideIntoContainer(
							towards = AnimatedContentTransitionScope.SlideDirection.Right,
							animationSpec = tween(150, easing = FastOutSlowInEasing)
						) togetherWith slideOutOfContainer(
									towards = AnimatedContentTransitionScope.SlideDirection.Right,
									animationSpec = tween(150, easing = FastOutSlowInEasing)
								)
					}
				},
				label = "alarm screen navigation"
			) { shouldWeShowAlarmScreen ->

				if (shouldWeShowAlarmScreen) {
					ListAlarmScreen(
						alarmPickerViewModel,
						previewingUri = previewing?.soundUri,
						selectedUri = selected?.soundUri,
						onBack = {
							showAlarmSoundList = false
						},
						onSelected = { sound ->
							alarmPickerViewModel.onAlarmSoundSelected(sound)
						}
					)
				} else {
					AlarmPickerScreen(
						alarmSetProceed = {
							viewModel.onNextClicked()
						},
						forNewAlarm = true,
						viewModel = alarmPickerViewModel,
						onNavigateToSoundList = {
							showAlarmSoundList = true
						},
						settingAlarmCancelled = {
							viewModel.onPreviousClicked()
						}
					)
				}
			}

		}
		DisplaySate.AlarmResult -> AlarmResultClaude(uiState.alarmData, onNextClick = {viewModel.onPreviousClicked()})
	}
}