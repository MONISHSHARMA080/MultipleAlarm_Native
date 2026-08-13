package MultipleAlarmClock.alarmFeature.ui.onboarding

import MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.OnboardingUiState
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel class OnboardingViewModel @Inject constructor(
	val analytics: Analytics,
	@ApplicationContext val context: Context
) : ViewModel() {

	private val _displayState = MutableStateFlow(OnboardingUiState())
	val displayState = _displayState.asStateFlow()

	private val _missingSteps = MutableStateFlow<List<PermissionStep>>(emptyList())
	val missingSteps = _missingSteps.asStateFlow()

	private val _allCriticalGranted = MutableStateFlow(false)
	val allCriticalGranted = _allCriticalGranted.asStateFlow()

	init {
		viewModelScope.launch {
			refreshPermissions()
		}
	}

	fun refreshPermissions() {
		val missing = PermissionUtils.getRequiredPermissionSteps(context)
		_missingSteps.update { missing }
		_allCriticalGranted.update { PermissionUtils.allCriticalPermissionsGranted(context) }
	}

	fun onNextClicked()  {
		// increment the state
		_displayState.update { value ->
			when(value.displaySate){
				DisplaySate.Greeting -> value.copy(displaySate = DisplaySate.Problem)
				// -- spam for testing
				DisplaySate.Problem -> value.copy(displaySate = DisplaySate.Permission)
				DisplaySate.CreateFirstAlarm ->value.copy(displaySate = DisplaySate.AlarmResult)
				DisplaySate.Permission -> value.copy(displaySate = DisplaySate.CreateFirstAlarm)
				// -- spam for testing

//				DisplaySate.Problem -> DisplaySate.CreateFirstAlarm
//				DisplaySate.CreateFirstAlarm -> DisplaySate.Permission
//				DisplaySate.Permission -> DisplaySate.AlarmResult
				DisplaySate.AlarmResult -> value.copy(displaySate = DisplaySate.AlarmResult)
			}
		}
	}
}

