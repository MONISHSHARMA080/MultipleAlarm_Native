package MultipleAlarmClock.alarmFeature.ui.onboarding

import MultipleAlarmClock.alarmFeature.ui.onboarding.components.GreetingScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.PermissionScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.components.ProblemScreen
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun OnboardingScreen() {
	val viewModel : OnboardingViewModel = hiltViewModel()
	val uiState by viewModel.displayState.collectAsStateWithLifecycle()
	val missingSteps by viewModel.missingSteps.collectAsStateWithLifecycle()
	val allCriticalGranted by viewModel.allCriticalGranted.collectAsStateWithLifecycle()

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val notificationPermState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
	
	// Track if we've requested notification permission at least once
	var notificationRequested by rememberSaveable {
		mutableStateOf(false)
	}

	val notificationPermanentlyDenied = notificationRequested && !notificationPermState.status.isGranted && !notificationPermState.status.shouldShowRationale

	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				viewModel.refreshPermissions()
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	when(uiState.displaySate){
		DisplaySate.Greeting -> GreetingScreen(onClickNext = {viewModel.onNextClicked()} )
		DisplaySate.Problem -> ProblemScreen { viewModel.onNextClicked() }
		DisplaySate.CreateFirstAlarm -> Text("--Open alarm Picker screen and allow then to create their first alarm--")
		DisplaySate.Permission -> {
			PermissionScreen(
				missingSteps = missingSteps,
				allCriticalGranted = allCriticalGranted,
				notificationPermanentlyDenied = notificationPermanentlyDenied,
				onNext = {
					viewModel.onNextClicked()
						 },
				onAction = { step ->
					when (step) {
						PermissionStep.PostNotification -> {
							if (notificationPermanentlyDenied) {
								context.startActivity(
									Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
										data = Uri.fromParts("package", context.packageName, null)
									}
								)
							} else {
								notificationPermState.launchPermissionRequest()
								notificationRequested = true
							}
						}
						PermissionStep.ExactAlarm -> {
							context.startActivity(
								Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
									data = Uri.fromParts("package", context.packageName, null)
								}
							)
						}
						PermissionStep.FullScreenIntent -> {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
								context.startActivity(
									Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
										data = Uri.fromParts("package", context.packageName, null)
									}
								)
							}
						}
						PermissionStep.XiaomiAutostart -> {
							PermissionUtils.launchXiaomiSettings(context)
						}
					}
				}
			)
		}
		DisplaySate.AlarmResult -> Text("--show them a screen that shows them how many notification is scheduled--")
	}
}
