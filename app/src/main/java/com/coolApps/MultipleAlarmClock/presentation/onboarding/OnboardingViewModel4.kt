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
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@HiltViewModel
class OnboardingViewModel4 @Inject constructor(
	val analytics: Analytics,
	alarmRepository: AlarmRepository,
	private val settingsDataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	private val alarmManager: AlarmManager,
	@ApplicationContext val context: Context,
) : ViewModel() {

	private val _displayState = MutableStateFlow(OnboardingUiState4())

	val displayState: StateFlow<OnboardingUiState4> = combine(
		_displayState,
		alarmRepository.getAlarmsStream(),
	) { uiState, alarmList ->
		uiState.copy(alarmData = alarmList.maxByOrNull { it.id })
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = OnboardingUiState4(),
	)

	private val _onboardingOffering = MutableStateFlow<OfferingState>(OfferingState.Loading)
	val onboardingOffering: StateFlow<OfferingState> = _onboardingOffering.asStateFlow()

	private val navigationBackStack = mutableListOf<DisplayState4>()

	init {
		viewModelScope.launch {
			assignOnboardingFlow()
			refreshPermissions()
		}
		viewModelScope.launch(Dispatchers.IO) {
			fetchOnboardingEndOffering()
		}
	}

	private suspend fun assignOnboardingFlow() {
		val stored = settingsDataStore.data.first().onboardingFlow
		if (stored == OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED) return
		logD("onboarding flow: assigned $stored")
		_displayState.update { it.copy(onboardingFlow = stored) }
	}

	private suspend fun fetchOnboardingEndOffering() {
		try {
			val offerings = Purchases.sharedInstance.awaitOfferings()
			// Placement offering first; `current` as fallback (covers the
			// case before the placement has any "for all other cases" rule).
			val serving = offerings
				.getCurrentOfferingForPlacement(PaywallTriggerType.OnboardingEnd.placementId)
				?: offerings.current
			logD("Paywall: offering(${PaywallTriggerType.OnboardingEnd.placementId}) = ${serving?.identifier ?: "none"}")
			_onboardingOffering.value = OfferingState.Loaded(serving)
		} catch (e: PurchasesException) {
			logD("Paywall: failed to fetch offerings for ${PaywallTriggerType.OnboardingEnd.placementId}: $e")
			_onboardingOffering.value = OfferingState.Failed
		}
	}

	fun refreshPermissions() {
		val missing = PermissionUtils.getRequiredPermissionSteps(context)
		val allCriticalGranted = PermissionUtils.allCriticalPermissionsGranted(context)
		_displayState.update {
			it.copy(missingSteps = missing, allCriticalGranted = allCriticalGranted)
		}
	}

	fun onStrugglesSelected(struggles: Set<String>) {
		val primary = OnboardingStruggle.resolvePrimary(struggles)
		_displayState.update {
			it.copy(selectedStruggles = struggles, primaryStruggle = primary)
		}
	}

	fun onProfileSelected(profile: String?) {
		_displayState.update { it.copy(selectedProfile = profile) }
	}

	fun onWakeUpTimeChanged(hour: Int, minute: Int) {
		_displayState.update { it.copy(wakeUpHour = hour, wakeUpMinute = minute) }
	}

	fun buildPresetAlarm(defaultMessage: String): AlarmData {
		val state = displayState.value
		val minSpan = state.defaultFrequencyMin.toInt().coerceAtLeast(1)
		val maxEndMin = 23 * 60 + 59
		val rawStartMin = state.wakeUpHour * 60 + state.wakeUpMinute
		val startTotalMin = rawStartMin.coerceAtMost(maxEndMin - minSpan)
		val endTotalMin = (startTotalMin + state.defaultWindowMinutes).coerceAtMost(maxEndMin)

		val zoneId = ZoneId.systemDefault()
		val today = LocalDate.now(zoneId)
		val startMillis = today.atTime(LocalTime.of(startTotalMin / 60, startTotalMin % 60))
			.atZone(zoneId)
			.toInstant()
			.toEpochMilli()
		val endMillis = today.atTime(LocalTime.of(endTotalMin / 60, endTotalMin % 60))
			.atZone(zoneId)
			.toInstant()
			.toEpochMilli()

		val rolled = AlarmData(
			id = state.alarmData?.id ?: 0,
			startTime = startMillis,
			endTime = endMillis,
			message = defaultMessage,
			frequencyInMin = state.defaultFrequencyMin,
			sound = null,
			isReadyToUse = true,
		).rollOverIfTimeIntervalPassed()

		if (rolled.getNextAlarmTriggerTime() != null) return rolled

		val tomorrowStart = Instant.ofEpochMilli(rolled.startTime).atZone(zoneId).plusDays(1).toInstant().toEpochMilli()
		val tomorrowEnd = Instant.ofEpochMilli(rolled.endTime).atZone(zoneId).plusDays(1).toInstant().toEpochMilli()
		return rolled.copy(startTime = tomorrowStart, endTime = tomorrowEnd)
	}

	fun onNextClicked() {
		val currentState = _displayState.value
		val currentStep = currentState.displayState
		analytics.captureEvent(
			"onboarding_next_clicked",
			mapOf("step" to currentStep.name, "onBoardingUiState" to displayState.value.toString()),
		)

		when (currentStep) {
			DisplayState4.UserStruggles -> {
				if (currentState.selectedStruggles.isNotEmpty()) {
					val props = buildMap<String, Any> {
						put("struggles", currentState.selectedStruggles.toList())
						currentState.primaryStruggle?.let { put("primary_struggle", it.analyticsKey) }
					}
					analytics.captureEvent("onboarding_struggles_submitted", props)
				}
			}
			DisplayState4.UserProfile -> {
				currentState.selectedProfile?.let {
					analytics.captureEvent("onboarding_profile_submitted", mapOf("profile" to it))
				}
			}
			DisplayState4.WakeUpTime -> {
				val props = buildMap<String, Any> {
					put("hour", currentState.wakeUpHour)
					put("minute", currentState.wakeUpMinute)
					currentState.primaryStruggle?.let { put("primary_struggle", it.analyticsKey) }
				}
				analytics.captureEvent("onboarding_wakeup_time_submitted", props)
			}
			else -> Unit
		}

		dispatchNavigationAction(OnboardingNavigationAction.Next)
	}

	fun onPreviousClicked() {
		val currentStep = _displayState.value.displayState.name
		analytics.captureEvent("onboarding_previous_clicked", mapOf("step" to currentStep))
		dispatchNavigationAction(OnboardingNavigationAction.Back)
	}

	fun skipPrePaywall() {
		if (_displayState.value.displayState != DisplayState4.PrePaywall) return
		_displayState.update { it.copy(displayState = DisplayState4.OnboardingPaywall) }
	}

	private fun dispatchNavigationAction(action: OnboardingNavigationAction) {
		when (action) {
			OnboardingNavigationAction.Back -> navigateBack()
			OnboardingNavigationAction.Next -> {
				val currentStep = _displayState.value.displayState
				val nextStep = reduceNavigation(
					currentStep = currentStep,
					isHardPaywallEnabled = analytics.featureFlagsData.value?.isHardPaywallEnabled == true,
				)
				if (nextStep != currentStep) {
					navigationBackStack += currentStep
					_displayState.update {
						it.copy(
							displayState = nextStep,
							canNavigateBack = navigationBackStack.isNotEmpty(),
						)
					}
				}
			}
		}
	}

	private fun navigateBack() {
		val previousStep = navigationBackStack.removeLastOrNull() ?: return
		_displayState.update {
			it.copy(
				displayState = previousStep,
				canNavigateBack = navigationBackStack.isNotEmpty(),
			)
		}
	}

	private fun reduceNavigation(
		currentStep: DisplayState4,
		isHardPaywallEnabled: Boolean,
	): DisplayState4 {
		return when (currentStep) {
			DisplayState4.UserStruggles -> DisplayState4.UserProfile
			DisplayState4.UserProfile -> DisplayState4.WakeUpTime
			DisplayState4.WakeUpTime -> DisplayState4.Permission
			DisplayState4.Permission -> DisplayState4.CreateFirstAlarm
			DisplayState4.CreateFirstAlarm -> DisplayState4.AlarmPreview
			DisplayState4.AlarmPreview -> if (isHardPaywallEnabled) DisplayState4.PrePaywall else DisplayState4.OnboardingPaywall
			DisplayState4.PrePaywall -> DisplayState4.OnboardingPaywall
			DisplayState4.OnboardingPaywall -> DisplayState4.OnboardingPaywall
		}
	}

	private sealed interface OnboardingNavigationAction {
		data object Next : OnboardingNavigationAction
		data object Back : OnboardingNavigationAction
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
			alarmsController.cancelAlarmHandler(alarmData, context, alarmManager)
		}
	}

	fun finishedOnboarding(restoreAlarm: AlarmData? = null) {
		analytics.captureEvent("onboarding_finished", emptyMap())
		viewModelScope.launch {
			if (restoreAlarm != null) {
				// Re-schedule before flipping isFirstLaunch so navigation pop cannot cancel ViewModelScope mid-save.
				withContext(NonCancellable) {
					alarmsController.resetAlarmsHandler(
						alarmData = restoreAlarm,
						alarmManager = alarmManager,
						activityContext = context,
					)
				}
			}
			settingsDataStore.updateData { data ->
				if (data.installEpochTimeMs == 0L) {
					data.copy {
						isFirstLaunch = false
						installEpochTimeMs = System.currentTimeMillis()
					}
				} else {
					data.copy {
						isFirstLaunch = false
					}
				}
			}
		}
	}

	private fun logD(msg: String) {
		Log.d("AAAAA", "[OnboardingViewModel4] $msg")
	}
}
