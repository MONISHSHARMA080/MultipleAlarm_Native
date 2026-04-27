package com.example.MultipleAlarmClock.Ui.alarmPicker

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController.AlarmValueForAlarmSeries
import com.coolApps.MultipleAlarmClock.Components_for_ui_compose.alarmPicker.getPreviewAlarms
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.example.MultipleAlarmClock.Data.dataStore.dataStore
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class AlarmPickerViewModel @Inject constructor(
	val analytics: Analytics,
	private val alarmManager: AlarmManager,
	private val alarmDao: AlarmDao,
	private val application: Application,
	@ApplicationContext  val context: Context
) : ViewModel() {

	private val _uiState = MutableStateFlow(AlarmPickerUiState())
	val uiState: StateFlow<AlarmPickerUiState> = _uiState.asStateFlow()

	private val _events = MutableSharedFlow<AlarmPickerEvent>(extraBufferCapacity = 1)
	val events: SharedFlow<AlarmPickerEvent> = _events.asSharedFlow()


	val allPermissionsGranted: StateFlow<Boolean> = application.dataStore.data
		.map { it.allPermissionsGranted }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = true
		)

	private var initialAlarm: AlarmData? = null
	private val errorHandler = ErrorHandler(notificationHandler = NotificationHandler(context),analytics)
	private val alarmsController = AlarmsController()

	fun initialize(alarm: AlarmData?) {
		initialAlarm = alarm
		_uiState.update {
			it.copy(alarmObject = alarm?.toAlarmObject() ?: createDefaultAlarmObject(alarm))
		}
		validateAlarm()
	}

	private fun createDefaultAlarmObject(alarm: AlarmData?): AlarmObject {
		val now = Calendar.getInstance()
		return AlarmObject(
			startTime = (now.clone() as Calendar).apply {
				add(Calendar.MINUTE, 1)
				if (get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
					set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
					set(Calendar.HOUR_OF_DAY, 23)
					set(Calendar.MINUTE, 59)
				}
				set(Calendar.SECOND, 0)
			},
			endTime = (now.clone() as Calendar).apply {
				add(Calendar.MINUTE, 45)
				if (get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
					set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
					set(Calendar.HOUR_OF_DAY, 23)
					set(Calendar.MINUTE, 59)
				}
				set(Calendar.SECOND, 0)
			},
			date = Calendar.getInstance().timeInMillis,
			message = alarm?.message ?: "",
			freqGottenAfterCallback = alarm?.frequencyInMin ?: 1
		)
	}

	fun checkPermissions(context: Context) {
		viewModelScope.launch {
			val liveCheck = PermissionUtils.allCriticalPermissionsGranted(context)

			// Update UI state so the button/colors change instantly
			_uiState.update { it.copy(areAllPermissionsGranted = liveCheck) }

			// Sync with DataStore silently
			context.dataStore.updateData { it.copy(allPermissionsGranted = liveCheck) }
		}
	}

	// Update your onSetAlarmClicked to be even simpler
	fun onSetAlarmClicked(currentAlarm: AlarmData?, alarmObject: AlarmObject) {
		// We don't need to check permissions here anymore because the button
		// is only clickable (or behaves differently) based on uiState.isPermissionGranted
		viewModelScope.launch {
			if (!_uiState.value.areAllPermissionsGranted) {
				val missing = PermissionUtils.getRequiredPermissionSteps(context)
				_events.emit(AlarmPickerEvent.ShowPermissionDialog(missing))
			} else {
				setAlarm(alarmObject, currentAlarm)
				_events.emit(AlarmPickerEvent.NavigateBack)
			}
		}
	}

	fun updateStartTime(newTime: Calendar) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(startTime = newTime)) }
		validateAlarm()
	}

	fun updateEndTime(newTime: Calendar) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(endTime = newTime)) }
		validateAlarm()
	}

	fun updateDate(calVersion: Calendar) {
		val currentAlarm = _uiState.value.alarmObject

		val newStartDate = (currentAlarm.startTime.clone() as Calendar).apply {
			set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
			set(Calendar.MONTH, calVersion.get(Calendar.MONTH))
			set(Calendar.DAY_OF_MONTH, calVersion.get(Calendar.DAY_OF_MONTH))
		}

		val newEndDate = (currentAlarm.endTime.clone() as Calendar).apply {
			set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
			set(Calendar.MONTH, calVersion.get(Calendar.MONTH))
			set(Calendar.DAY_OF_MONTH, calVersion.get(Calendar.DAY_OF_MONTH))
		}

		_uiState.update {
			it.copy(
				alarmObject = it.alarmObject.copy(
					date = calVersion.timeInMillis,
					startTime = newStartDate,
					endTime = newEndDate
				)
			)
		}
		validateAlarm()
	}

	fun updateFrequency(newFreq: Long) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(freqGottenAfterCallback = newFreq)) }
		validateAlarm()
	}

	fun updateMessage(newMessage: String) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(message = newMessage)) }
		validateAlarm()
	}

	fun incrementFrequency() {
		val current = _uiState.value.alarmObject.freqGottenAfterCallback
		val newFreq = if (current >= 1) current + 1 else 1
		updateFrequency(newFreq)
	}

	fun decrementFrequency() {
		val current = _uiState.value.alarmObject.freqGottenAfterCallback
		updateFrequency(current - 1)
	}

	private fun validateAlarm() {
		val result = _uiState.value.alarmObject.validate(initialAlarm)
		_uiState.update { it.copy(validationResult = result) }
	}

	fun getFrequencyPreviewText(): String {
		val state = _uiState.value
		val currentError = state.validationResult as? ValidationResult.Failure

		return if (state.validationResult is ValidationResult.Success) {
			"your alarm will ring on " + getPreviewAlarms(state.alarmObject, 4)
		} else {
			if (currentError?.field == AlarmErrorField.FREQUENCY) {
				currentError.message
			} else ""
		}
	}

	fun getValidationErrorMessage(): String {
		return when (val res = _uiState.value.validationResult) {
			is ValidationResult.Success -> ""
			is ValidationResult.Failure -> res.message
		}
	}

	fun captureEvent(name:String, properties: Map<String, Any>){
		viewModelScope.launch {
			analytics.captureEvent(name, properties)
		}
	}

	/**[setAlarm] - here [AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
	fun setAlarm(newAlarmObject: AlarmObject, oldAlarm: AlarmData? ){
		when ( oldAlarm) {
			null -> {
				//  oldAlarm was not there so setting a new alarm
				viewModelScope.launch {
					launch {
						analytics.captureEvent("user setting new alarm", mapOf(
							"alarmObject" to newAlarmObject.toString()
						))
					}
					logD("the alarm data confirmed is $newAlarmObject, and is  oldAlarm == newAlarmObject ->  ")
					val exception = alarmsController.startAlarmSeriesHandler(
						alarm = AlarmValueForAlarmSeries.AlarmObjectType(newAlarmObject),
						alarmManager = alarmManager,
						activityContext = context,
						alarmDao = alarmDao,
					)
					exception.fold(
						onSuccess = {
							launch {
								analytics.captureEvent("new alarm successfully set", mapOf("alarmObject" to newAlarmObject.toString()))
							}
						},
						onError = {messageToDisplayUser, exception ->
							logD("there is a error in making new alarm  that is $exception ")
							errorHandler.handleError(com.coolApps.MultipleAlarmClock.utils.Result.Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while making new alarm, Please try again" )
						}
					)
				}
			}
			else -> {
				//  oldAlarm was there so editing an existing alarm
				viewModelScope.launch {
					logD("deleting the alarm $ oldAlarm")
					alarmsController.updateAlarmStateInDb( oldAlarm, alarmDao).fold(onSuccess = {}, onError = { messageToDisplayUser, exception  ->
						// no such alarm exist in DB so can't update it
						logD("there is a error while editing the alarm and updating it's state in DB and  that is ${exception.message} ")
						errorHandler.handleError(com.coolApps.MultipleAlarmClock.utils.Result.Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
					}
					)
					val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
						alarm = AlarmValueForAlarmSeries.AlarmDataType(newAlarmObject.toAlarmData(oldAlarm.id) ),
						alarmManager, context, alarmDao
					)
					// now the error case is handled there
					alarmScheduledResult.fold(
						onSuccess = { },
						onError = { messageToDisplayUser, exception ->
							errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
							logD("there is a error/Exception in editing new alarm-->${exception.message}")
						}
					)

				}
			}
		}

	}
}
