package com.example.MultipleAlarmClock.Ui.Navigation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel()
class NavigationViewModel @Inject constructor(
	private val application: Application,
	val analytics: Analytics,
) : ViewModel() {

//	val isFirstLaunch: StateFlow<Boolean?> = application.dataStore.data
//		.map { it.isFirstLaunch }
//		.stateIn(
//			scope = viewModelScope,
//			started = SharingStarted.WhileSubscribed(5_000),
//			initialValue = null
//		)

	fun captureEvent(eventName:String, properties: Map<String, Any>): Unit {
		viewModelScope.launch {
			analytics.captureEvent(eventName, properties)
		}
	}
	fun screen(screenName:String, properties: Map<String, Any>? = null): Unit {
		viewModelScope.launch {
			analytics.screen(screenName, properties)
		}
	}

//	suspend fun onOnboardingComplete() {
//		viewModelScope.launch {
//			application.dataStore.updateData { it.copy(isFirstLaunch = false) }
//		}
//	}
}
