package com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.Permissions.PermissionUtils
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.DisplaySate
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.onboarding.data.OnboardingUiState
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

	init {
		viewModelScope.launch {
			refreshPermissions()
		}
	}

	fun refreshPermissions() {
		val missing = PermissionUtils.getRequiredPermissionSteps(context)
		val allCriticalGranted = PermissionUtils.allCriticalPermissionsGranted(context)
		_displayState.update { it.copy(missingSteps = missing, allCriticalGranted = allCriticalGranted ) }
	}

	fun onNextClicked()  {
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_next_clicked", mapOf("step" to currentStep, "onBoardingUiState" to displayState.value.toString() ))
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
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_previous_clicked", mapOf("step" to currentStep))
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
		 analytics.captureEvent("onboarding_finished", emptyMap())
		 viewModelScope.launch {
			 settingsDataStore.updateData { data ->
				 if (data.installEpochTimeMs == 0L){
					 data.copy {
						 isFirstLaunch = false
						 installEpochTimeMs = System.currentTimeMillis()
					 }
				 }else{
					 data.copy {
						 isFirstLaunch = false
					 }
				 }
			 }
		 }
	}
}