package MultipleAlarmClock.alarmFeature.ui.onboarding

import MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import MultipleAlarmClock.alarmFeature.ui.onboarding.data.OnboardingUiState
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.example.MultipleAlarmClock.Data.dataStore.Settings
import com.example.MultipleAlarmClock.Data.dataStore.copy
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionStep
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel class OnboardingViewModel @Inject constructor(
	val analytics: Analytics,
	alarmRepository: AlarmRepository,
	private val settingsDataStore: DataStore<Settings>,
	@ApplicationContext val context: Context
) : ViewModel() {

	private val _displayState = MutableStateFlow(OnboardingUiState())

	val displayState = combine(_displayState, alarmRepository.getAlarmsStream()){ uiState, alarmList ->
		uiState.copy(alarmData = alarmList.firstOrNull(),)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = OnboardingUiState()
	)

	private val _missingSteps = MutableStateFlow<List<PermissionStep>>(emptyList())
	val missingSteps = _missingSteps.asStateFlow()

	private val _allCriticalGranted = MutableStateFlow(false)
	val allCriticalGranted = _allCriticalGranted.asStateFlow()

	val nonCancellableScope = CoroutineScope(NonCancellable)

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
				DisplaySate.Problem -> value.copy(displaySate = DisplaySate.Permission)
				DisplaySate.Permission -> value.copy(displaySate = DisplaySate.CreateFirstAlarm)
				DisplaySate.CreateFirstAlarm ->value.copy(displaySate = DisplaySate.AlarmResult)
				DisplaySate.AlarmResult -> value.copy(displaySate = DisplaySate.AlarmResult);
			}
		}
	}
	fun onPreviousClicked()  {
		// increment the state
		_displayState.update { value ->
			when(value.displaySate){
				DisplaySate.Greeting -> value.copy(displaySate = DisplaySate.Greeting)
				DisplaySate.Problem -> value.copy(displaySate = DisplaySate.Greeting)
				DisplaySate.Permission -> value.copy(displaySate = DisplaySate.Problem)
				DisplaySate.CreateFirstAlarm ->value.copy(displaySate = DisplaySate.Permission)
				DisplaySate.AlarmResult -> value.copy(displaySate = DisplaySate.CreateFirstAlarm)
			}
		}
	}

	  fun finishedOnboarding(){
		 viewModelScope.launch {
			 settingsDataStore.updateData { data -> data.copy { isFirstLaunch = false }}
		 }
	}
}

