package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.copy
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionUtils
import com.coolApps.MultipleAlarmClock.presentation.onboarding.DisplaySate
import com.coolApps.MultipleAlarmClock.presentation.onboarding.OnboardingUiState
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import com.coolApps.MultipleAlarmClock.util.toAnalyticsString
import com.revenuecat.purchases.CustomerInfo
import android.app.AlarmManager
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.revenuecat.purchases.models.StoreTransaction
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
	private val alarmsController: AlarmsController,
	private val alarmManager: AlarmManager,
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

	private var selectedStruggles = emptySet<String>()

	fun onStrugglesSelected(struggles: Set<String>) {
		selectedStruggles = struggles
	}

	fun onNextClicked()  {
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_next_clicked", mapOf("step" to currentStep, "onBoardingUiState" to displayState.value.toString() ))
		
		if (_displayState.value.displaySate == DisplaySate.UserStruggles && selectedStruggles.isNotEmpty()) {
			analytics.captureEvent("onboarding_struggles_submitted", mapOf("struggles" to selectedStruggles.toList()))
		}
		
		// increment the state
		_displayState.update { value ->
			when(value.displaySate){
				DisplaySate.Greeting -> value.copy(displaySate = DisplaySate.Problem)
				DisplaySate.Problem -> value.copy(displaySate = DisplaySate.UserStruggles)
				DisplaySate.UserStruggles -> value.copy(displaySate = DisplaySate.Permission)
				DisplaySate.Permission -> value.copy(displaySate = DisplaySate.FirstAlarmIntro)
				DisplaySate.FirstAlarmIntro -> value.copy(displaySate = DisplaySate.CreateFirstAlarm)
				DisplaySate.CreateFirstAlarm ->value.copy(displaySate = DisplaySate.AlarmResult)
				DisplaySate.AlarmResult -> value.copy(displaySate = DisplaySate.OnboardingPaywall)
				DisplaySate.OnboardingPaywall -> value.copy(displaySate = DisplaySate.OnboardingPaywall)
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
				DisplaySate.UserStruggles -> value.copy(displaySate = DisplaySate.Problem)
				DisplaySate.Permission -> value.copy(displaySate = DisplaySate.UserStruggles)
				DisplaySate.FirstAlarmIntro -> value.copy(displaySate = DisplaySate.Permission)
				DisplaySate.CreateFirstAlarm ->value.copy(displaySate = DisplaySate.FirstAlarmIntro)
				DisplaySate.AlarmResult -> value.copy(displaySate = DisplaySate.CreateFirstAlarm)
				DisplaySate.OnboardingPaywall -> value.copy(displaySate = DisplaySate.AlarmResult)
			}
		}
	}

	fun onPurchaseCompletedEvent(customerInfo: CustomerInfo, storeTransaction: StoreTransaction){
		viewModelScope.launch {
			analytics.captureEvent("purchase_completed",mapOf(
				"customerInfo" to customerInfo.toString(),
				"storeTransaction" to storeTransaction.toAnalyticsString(),
			))
			TrialReminderScheduler.scheduleIfOnTrial(context, customerInfo)
		}
	}
	fun onRestoreCompletedEvent(customerInfo:CustomerInfo){
		viewModelScope.launch {
			analytics.captureEvent("restore_completed",mapOf(
				"customerInfo" to customerInfo.toString()
			))
		}
	}



	fun stopAlarm(alarmData: AlarmData) {
		viewModelScope.launch {
			alarmsController.cancelAlarmHandler(alarmData, context, alarmManager).fold(
				onSuccess = {},
				onError = { error ->
					// Handle error if needed
				}
			)
		}
	}

	fun resetAlarm(alarmData: AlarmData) {
		viewModelScope.launch {
			alarmsController.resetAlarmsHandler(
				alarmData = alarmData,
				alarmManager = alarmManager,
				activityContext = context,
			).fold(
				onSuccess = {},
				onError = { error ->
					// Handle error if needed
				}
			)
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