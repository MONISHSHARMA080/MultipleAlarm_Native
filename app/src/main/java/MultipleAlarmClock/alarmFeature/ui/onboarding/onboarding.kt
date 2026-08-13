package MultipleAlarmClock.alarmFeature.ui.onboarding

import MultipleAlarmClock.alarmFeature.ui.onboarding.components.GreetingScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.PermissionScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.ProblemScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen() {
	val viewModel : OnboardingViewModel = hiltViewModel()
	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val missingSteps by viewModel.missingSteps.collectAsStateWithLifecycle()
	val allCriticalGranted by viewModel.allCriticalGranted.collectAsStateWithLifecycle()

	when(uiState.displaySate){
		DisplaySate.Greeting -> GreetingScreen(onClickNext = {viewModel.onNextClicked()} )
		DisplaySate.Problem -> ProblemScreen { viewModel.onNextClicked() }
		DisplaySate.CreateFirstAlarm -> Text("--Open alarm Picker screen and allow then to create their first alarm--")
		DisplaySate.Permission -> {
			PermissionScreen(
				missingSteps = missingSteps,
				allCriticalGranted = allCriticalGranted,
				onNext = { viewModel.onNextClicked() },
				refreshPermissionUiState = {viewModel.refreshPermissions()},
			)
		}
		DisplaySate.AlarmResult -> Text("--show them a screen that shows them how many notification is scheduled--")
	}
}
