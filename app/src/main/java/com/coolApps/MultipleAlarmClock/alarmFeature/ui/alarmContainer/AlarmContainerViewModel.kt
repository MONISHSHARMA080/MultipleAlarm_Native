package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer

import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@HiltViewModel
class AlarmContainerViewModel @Inject constructor(
	val analytics: Analytics,
	alarmRepository: AlarmRepository,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	@ApplicationContext  val context: Context
) : ViewModel(){

	private val errorHandler = ErrorHandler(notificationHandler = NotificationHandler(context),analytics)

	val showFeedbackUIState: StateFlow<Boolean> = dataStore.data
		.map { settings ->
//			settings.firstAlarmSet && !settings.feedbackShown
//			logD("shouldWeShowFeedbackCard:${settings.shouldWeShowFeedbackCard}")
			settings.shouldWeShowFeedbackCard
		}
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

	fun dismissFeedback() {
		viewModelScope.launch {
//			dataStore.updateData { it.copy { feedbackShown = true } }
			dataStore.updateData { it.copy { shouldWeShowFeedbackCard = false } }
			analytics.captureEvent("feedback board dismissed", mapOf())
		}
	}

	fun captureFeedback(feedback: String) {
		viewModelScope.launch {
			dataStore.updateData { it.copy { shouldWeShowFeedbackCard = false } }
			analytics.captureEvent("feedback given", mapOf("feedback" to feedback))
		}
	}

	val alarms: StateFlow<List<AlarmData>?> = alarmRepository.getAlarmsStream()
		.flowOn(Dispatchers.IO)
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000 ),
			initialValue = null
		)

	fun stopAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			launch {
				analytics.captureEvent("user stopped the alarm", mapOf("alarmData" to alarmData.toString()))
			}
			logD("user asked to stop the alarm $alarmData")
			alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager).fold(onSuccess = {}, onError = { error ->
				errorHandler.handleError(Result.Failure(error))
				logD("there is a error in cancelling alarm-->${error.internalErrorMessage}")
			})
		}
	}

	suspend fun captureEvent(event:String, properties:Map<String, Any>){
		analytics.captureEvent(event, properties)
	}

	fun resetAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			logD("about to reset the alarm-+")
			launch {
				analytics.captureEvent("user reset the alarm", mapOf(
					"new alarmData" to alarmData.toString()
				))
			}
			val exception = alarmsController.resetAlarms(
				alarmData = alarmData,
				alarmManager = alarmManager,
				activityContext = context,
			)
			exception.fold(
				onSuccess = {
					viewModelScope.launch {
						analytics.captureEvent("alarm successfully reset", mapOf(
							"alarmData" to alarmData.toString()
						))
					}
				},
				onError = { error ->
					errorHandler.handleError(Result.Failure(error))
					logD("error in the reset alarm -->${error.internalErrorMessage}")
				}
			)
		}

	}

	fun deleteAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			launch {
				analytics.captureEvent("user deleting the alarm", mapOf(
					"alarmData" to alarmData.toString()
				)
				)
			}
			logD("deleting the alarm $alarmData")
			alarmsController.deleteAlarmHandler(alarmData, context, alarmManager).fold(onSuccess = {}, onError = { error ->
				logD("there is a error in deleting the alarm that is $error")
				errorHandler.handleError(Result.Failure(error))
			})
		}

	}
}