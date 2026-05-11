package com.example.MultipleAlarmClock.Ui.alarmContainer

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.example.MultipleAlarmClock.Data.dataStore.Settings
import com.example.MultipleAlarmClock.Data.dataStore.copy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@HiltViewModel
class AlarmContainerViewModel @Inject constructor(
	val analytics: Analytics,
	private val alarmDao: AlarmDao,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	@ApplicationContext  val context: Context
) : ViewModel(){

	private val alarmsController = AlarmsController()
	private val errorHandler = ErrorHandler(notificationHandler = NotificationHandler(context),analytics)
	val showFeedbackPopup: StateFlow<Boolean> = dataStore.data
		.map { it.firstAlarmSet }
		.distinctUntilChanged()
		.scan(Pair(null as Boolean?, false)) { accumulator, isSet ->
			val prev = accumulator.first
			// Trigger only if we explicitly transitioned from false to true
			val trigger = (prev == false && isSet)
			Pair(isSet, trigger)
		}
		.map { it.second } // Extract the trigger boolean
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = false
		)
	private val _isFeedbackDismissed = MutableStateFlow(false)

	// 2. Combine the transition trigger with the dismissal state
	val showFeedbackUIState: StateFlow<Boolean> = combine(
		showFeedbackPopup, // The flow from Step 1
		_isFeedbackDismissed
	) { triggered, dismissed ->
		triggered && !dismissed
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

	fun dismissFeedback() {
		viewModelScope.launch {
			analytics.captureEvent("feedback board dismissed", mapOf())
			dataStore.updateData {
				it.copy {  firstAlarmSet = true}
			}
		}
		_isFeedbackDismissed.value = true
	}
	fun captureFeedback(feedback: String) {
		viewModelScope.launch {
			analytics.captureEvent("feedback given", mapOf(
					"feedback" to feedback
			))
			dataStore.updateData {
				it.copy {  firstAlarmSet = true}
			}
		}
		_isFeedbackDismissed.value = true
	}


	val alarms: StateFlow<List<AlarmData>?> = alarmDao.getAllAlarmsFlow()
		.flowOn(Dispatchers.IO)
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000 ),
			initialValue = null
		)

	fun stopAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			launch {
				analytics.captureEvent("user stopped the alarm", mapOf(
					"alarmData" to alarmData.toString()
				))
			}
			logD("user asked to stop the alarm $alarmData")
			alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager, alarmDao).fold(onSuccess = {}, onError = {messageToDisplayUser,exception ->
				errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while cancelling alarm, Please try again" )
				logD("there is a error/Exception in making new alarm-->${exception.message}")
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
				alarmDao =alarmDao,
			)
			exception.fold(
				onSuccess = {
					viewModelScope.launch {
						analytics.captureEvent("alarm successfully reset", mapOf(
							"alarmData" to alarmData.toString()
						))
					}
				},
				onError = { messageToDisplayUser, exception->
					errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while resting the  alarm, Please try again" )
					logD("error/exception in the reset alarm -->${exception.message}")
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
			alarmsController.deleteAlarmHandler(alarmData, context, alarmDao,  alarmManager).fold(onSuccess = {}, onError = {messageToDisplayUser, exception ->
				logD("there is a error in deleting the alarm  that is $exception ")
				errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while deleting the alarm, Please try again" )
			})
		}

	}
}