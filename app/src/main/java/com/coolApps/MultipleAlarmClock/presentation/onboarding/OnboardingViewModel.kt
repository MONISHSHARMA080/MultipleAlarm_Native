package com.coolApps.MultipleAlarmClock.presentation.onboarding


import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.copy
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.domain.repository.RemoteUiRepository
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.coolApps.MultipleAlarmClock.presentation.util.Permissions.PermissionUtils
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import com.coolApps.MultipleAlarmClock.util.toAnalyticsString
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel class OnboardingViewModel @Inject constructor(
	val analytics: Analytics,
	alarmRepository: AlarmRepository,
	private val remoteUiRepository: RemoteUiRepository,
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

	fun fetchSduiDocument() {
		if (_displayState.value.isFetchingSdui || _displayState.value.remoteDocument != null) return
		_displayState.update { it.copy(isFetchingSdui = true) }

		viewModelScope.launch {
			try {
				withTimeout(1500.milliseconds) {
					// 1. & 2. Fetch flag and extract URL
					val url = analytics.getFeatureFlagUrlPayload("onboarding_sdui_flow")
						?: throw IllegalStateException("Invalid or missing payload")
					
					// 3. Fetch Remote Document
					val docResult = remoteUiRepository.fetchRemoteDocument(url).first()
					docResult.onSuccess { doc ->
						_displayState.update { it.copy(remoteDocument = doc, remoteDocumentError = null, isFetchingSdui = false) }
					}.onFailure { err ->
						throw err
					}
				}
			} catch (e: Exception) {
				if (e is CancellationException && e !is TimeoutCancellationException) {
					throw e
				}
				handleSduiFallback(e)
			}
		}
	}

	private suspend fun handleSduiFallback(e: Exception) {
		// Log fallback
		_displayState.update { it.copy(remoteDocumentError = e.message ?: "Failed to fetch SDUI", isFetchingSdui = false) }
		analytics.captureEvent(
			event = "onboarding_sdui_fallback",
			properties = mapOf("variant" to "fallback")
		)
		
	}

	private var selectedStruggles = emptySet<String>()
	private val navigationBackStack = mutableListOf<DisplaySate>()

	fun onStrugglesSelected(struggles: Set<String>) {
		selectedStruggles = struggles
	}

	fun onNextClicked()  {
		val currentStep = _displayState.value.displaySate.name
		analytics.captureEvent("onboarding_next_clicked", mapOf("step" to currentStep, "onBoardingUiState" to displayState.value.toString() ))
		
		if (_displayState.value.displaySate == DisplaySate.UserStruggles && selectedStruggles.isNotEmpty()) {
			analytics.captureEvent("onboarding_struggles_submitted", mapOf("struggles" to selectedStruggles.toList()))
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
				)
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
	): DisplaySate = when (action) {
		OnboardingNavigationAction.Next -> when (currentStep) {
			DisplaySate.Problem -> DisplaySate.UserStruggles
			DisplaySate.UserStruggles -> DisplaySate.Permission
			DisplaySate.Permission -> if (hasAlarm) DisplaySate.AlarmResult else DisplaySate.FirstAlarmIntro
			DisplaySate.FirstAlarmIntro -> if (hasAlarm) DisplaySate.AlarmResult else DisplaySate.CreateFirstAlarm
			DisplaySate.CreateFirstAlarm -> DisplaySate.AlarmResult
			DisplaySate.AlarmResult -> DisplaySate.OnboardingPaywall
			DisplaySate.OnboardingPaywall -> DisplaySate.OnboardingPaywall
		}
		OnboardingNavigationAction.SkipAlarmCreation -> when (currentStep) {
			DisplaySate.FirstAlarmIntro -> DisplaySate.OnboardingPaywall
			else -> currentStep
		}
		OnboardingNavigationAction.Back -> currentStep
	}

	private sealed interface OnboardingNavigationAction {
		data object Next : OnboardingNavigationAction
		data object Back : OnboardingNavigationAction
		data object SkipAlarmCreation : OnboardingNavigationAction
	}


	fun onPurchaseError(error: PurchasesError){
		viewModelScope.launch {
			analytics.captureEvent("purchase_error_ocurred",mapOf(
				"purchase_error" to error.toString(),
			))
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

	fun onPurchaseCancelled() {
		viewModelScope.launch {
			analytics.captureEvent("purchase_cancelled", mapOf())
		}
	}
}
