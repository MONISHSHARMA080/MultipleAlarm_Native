package com.coolApps.MultipleAlarmClock.presentation.navigation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.data.paywall.OfferingState
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallEvent
import com.coolApps.MultipleAlarmClock.data.paywall.PaywallTriggerType
import com.coolApps.MultipleAlarmClock.data.preferences.OnboardingFlow
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.copy
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.TrialReminderScheduler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class NavigationViewModel @Inject constructor(
	private val dataStore: DataStore<Settings>,
	private val analytics: Analytics,
	@ApplicationContext private val context: Context,
) : ViewModel() {

	val isFirstLaunch: StateFlow<Boolean?> = dataStore.data
		.map { it.isFirstLaunch }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = null
		)

	private val _onboardingFlow = MutableStateFlow(OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED)

	val onboardingFlow: StateFlow<OnboardingFlow> = _onboardingFlow.asStateFlow()

	init {
		viewModelScope.launch {
			if (dataStore.data.first().isFirstLaunch) selectOnboardingFlow()
		}
	}

	suspend fun selectOnboardingFlow(): OnboardingFlow {
		val stored = dataStore.data.first().onboardingFlow
		if (stored != OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED) {
			_onboardingFlow.value = stored
			return stored
		}
		val flow = when (Random.nextInt(3)) {
			0 -> OnboardingFlow.ONBOARDING_FLOW_VARIANT_LONG
			1 -> OnboardingFlow.ONBOARDING_FLOW_VARIANT_SHORT
			else -> OnboardingFlow.ONBOARDING_FLOW_VARIANT_V3
		}
		_onboardingFlow.value = flow
		logD("onboarding flow: assigned $flow")
		analytics.captureEvent("onboarding_variant_assigned", mapOf("variant" to flow.name))
		dataStore.updateData { prefs ->
			if (prefs.onboardingFlow == OnboardingFlow.ONBOARDING_FLOW_UNSPECIFIED) {
				prefs.copy { onboardingFlow = flow }
			} else prefs
		}
		return flow
	}

	private val _offeringsByPlacement = MutableStateFlow<Map<String, OfferingState>>(emptyMap())

	val offeringsByPlacement: StateFlow<Map<String, OfferingState>> = _offeringsByPlacement.asStateFlow()

	fun requestPaywallOffering(trigger: PaywallTriggerType) {
		when (_offeringsByPlacement.value[trigger.placementId]) {
			is OfferingState.Loaded, OfferingState.Loading -> return
			else -> Unit // absent or Failed — (re)start the fetch
		}
		viewModelScope.launch {
			_offeringsByPlacement.update { it + (trigger.placementId to OfferingState.Loading) }
			try {
				val offerings = Purchases.sharedInstance.awaitOfferings()
				// Placement offering first; `current` as fallback (covers the
				// case before the placement has any "for all other cases" rule).
				val serving = offerings.getCurrentOfferingForPlacement(trigger.placementId)
					?: offerings.current
				logD("Paywall: offering(${trigger.placementId}) = ${serving?.identifier ?: "none"}")
				_offeringsByPlacement.update { it + (trigger.placementId to OfferingState.Loaded(serving)) }
			} catch (e: PurchasesException) {
				logD("Paywall: failed to fetch offerings for ${trigger.placementId}: $e")
				_offeringsByPlacement.update { it + (trigger.placementId to OfferingState.Failed) }
			}
		}
	}


	fun captureEvent(eventName: String, properties: Map<String, Any>) {
		viewModelScope.launch {
			analytics.captureEvent(eventName, properties)
		}
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

	fun screen(screenName: String, properties: Map<String, Any>? = null) {
		viewModelScope.launch {
			analytics.screen(screenName, properties)
		}
	}

}
