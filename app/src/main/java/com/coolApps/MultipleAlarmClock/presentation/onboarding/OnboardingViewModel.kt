package com.coolApps.MultipleAlarmClock.presentation.onboarding

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.copy
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionUtils
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
		viewModelScope.launch(Dispatchers.IO) {
			fetchOnboardingEndOffering()
		}
	}


	private val _onboardingOffering = MutableStateFlow<OfferingState>(OfferingState.Loading)

	val onboardingOffering: StateFlow<OfferingState> = _onboardingOffering.asStateFlow()

	private suspend fun fetchOnboardingEndOffering() {
		try {
			val offerings = Purchases.sharedInstance.awaitOfferings()
			// Placement offering first; `current` as fallback (covers the
			// case before the placement has any "for all other cases" rule).
			val serving = offerings
				.getCurrentOfferingForPlacement(PaywallTriggerType.OnboardingEnd.placementId)
				?: offerings.current
			logD("Paywall: offering(${PaywallTriggerType.OnboardingEnd.placementId}) = ${serving?.identifier ?: "none"}, serving:$serving, offerings:$offerings")
			_onboardingOffering.value = OfferingState.Loaded(serving)
		} catch (e: PurchasesException) {
			logD("Paywall: failed to fetch offerings for ${PaywallTriggerType.OnboardingEnd.placementId}: $e")
			_onboardingOffering.value = OfferingState.Failed
		}
	}

	fun refreshPermissions() {
		val missing = PermissionUtils.getRequiredPermissionSteps(context)
		val allCriticalGranted = PermissionUtils.allCriticalPermissionsGranted(context)
		_displayState.update { it.copy(missingSteps = missing, allCriticalGranted = allCriticalGranted ) }
	}

	private var selectedStruggles = emptySet<String>()
	private var selectedProfile: String? = null
	private val navigationBackStack = mutableListOf<DisplaySate>()

	fun onStrugglesSelected(struggles: Set<String>) {
		selectedStruggles = struggles
	}

	fun onProfileSelected(profile: String?) {
		selectedProfile = profile
	}

	fun onNextClicked()  {
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_next_clicked", mapOf("step" to currentStep, "onBoardingUiState" to displayState.value.toString() ))

		if (_displayState.value.displaySate == DisplaySate.UserStruggles && selectedStruggles.isNotEmpty()) {
			analytics.captureEvent("onboarding_struggles_submitted", mapOf("struggles" to selectedStruggles.toList()))
		}
		if (_displayState.value.displaySate == DisplaySate.UserProfile) {
			selectedProfile?.let {analytics.captureEvent("onboarding_profile_submitted", mapOf("profile" to it))   }
		}
		dispatchNavigationAction(OnboardingNavigationAction.Next)
	}

	fun onSkipAlarmCreation() {
		dispatchNavigationAction(OnboardingNavigationAction.SkipAlarmCreation)
	}

	fun onPreviousClicked()  {
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_previous_clicked", mapOf("step" to currentStep))
		dispatchNavigationAction(OnboardingNavigationAction.Back)
	}

	private fun dispatchNavigationAction(action: OnboardingNavigationAction) {
		when (action) {
			OnboardingNavigationAction.Back -> navigateBack()
			OnboardingNavigationAction.Next,
			OnboardingNavigationAction.SkipAlarmCreation -> {
				val currentStep = _displayState.value.displaySate
				val nextStep = reduceNavigation(
					currentStep = currentStep,
					action = action,
					hasAlarm = displayState.value.alarmData != null,
					isHardPaywallEnabled = analytics.featureFlagsData.value?.isHardPaywallEnabled == true,
				)
				logD("hard paywall enabled ${analytics.featureFlagsData.value?.isHardPaywallEnabled}  hasAlarm = ${displayState.value.alarmData != null} nextStep: $nextStep")
				if (nextStep != currentStep) {
					navigationBackStack += currentStep
					_displayState.update { it.copy(displaySate = nextStep) }
				}
			}
		}
	}

	private fun navigateBack() {
		val previousStep = navigationBackStack.removeLastOrNull() ?: return
		_displayState.update { it.copy(displaySate = previousStep) }
	}

	private fun reduceNavigation(
		currentStep: DisplaySate,
		action: OnboardingNavigationAction,
		hasAlarm: Boolean,
		isHardPaywallEnabled: Boolean,
	): DisplaySate {
		return when (action) {
			OnboardingNavigationAction.Next -> when (currentStep) {
				DisplaySate.Problem -> DisplaySate.UserStruggles
				DisplaySate.UserStruggles -> DisplaySate.UserProfile
				DisplaySate.UserProfile -> DisplaySate.Permission
				DisplaySate.Permission -> if (hasAlarm) DisplaySate.AlarmResult else DisplaySate.FirstAlarmIntro
				DisplaySate.FirstAlarmIntro -> if (hasAlarm) DisplaySate.AlarmResult else DisplaySate.CreateFirstAlarm
				DisplaySate.CreateFirstAlarm -> DisplaySate.AlarmResult
				DisplaySate.AlarmResult -> if (isHardPaywallEnabled) DisplaySate.PrePaywall else DisplaySate.OnboardingPaywall
				DisplaySate.PrePaywall -> DisplaySate.OnboardingPaywall
				DisplaySate.OnboardingPaywall -> DisplaySate.OnboardingPaywall
			}
			OnboardingNavigationAction.SkipAlarmCreation -> when (currentStep) {
				DisplaySate.FirstAlarmIntro ->
					if (isHardPaywallEnabled) DisplaySate.PrePaywall else DisplaySate.OnboardingPaywall
				else -> currentStep
			}
			OnboardingNavigationAction.Back -> currentStep
		}
	}

	private sealed interface OnboardingNavigationAction {
		data object Next : OnboardingNavigationAction
		data object Back : OnboardingNavigationAction
		data object SkipAlarmCreation : OnboardingNavigationAction
	}


	fun onPaywallEvent(event: PaywallEvent) {
		viewModelScope.launch {
			analytics.captureEvent(event.eventName, event.eventParams)
			when (event) {
				is PaywallEvent.PurchaseCompleted -> {
					TrialReminderScheduler.scheduleIfOnTrial(context, event.customerInfo)
				}
				is PaywallEvent.RestoreCompleted -> {
					TrialReminderScheduler.scheduleIfOnTrial(context, event.customerInfo)
				}
				else -> Unit
			}
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
	private fun logD(msg: String){
		Log.d("AAAAA", "[OnboardingViewModel] $msg")
	}
}
