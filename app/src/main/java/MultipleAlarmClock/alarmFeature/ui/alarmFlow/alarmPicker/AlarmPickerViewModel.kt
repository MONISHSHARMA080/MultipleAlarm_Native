package com.example.MultipleAlarmClock.Ui.alarmPicker

import MultipleAlarmClock.alarmFeature.data.local.AlarmData
import MultipleAlarmClock.alarmFeature.domain.model.AlarmErrorField
import MultipleAlarmClock.alarmFeature.domain.model.AlarmObject
import MultipleAlarmClock.alarmFeature.domain.model.ValidationResult
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController.AlarmValueForAlarmSeries
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.services.PlayAlarm
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.example.MultipleAlarmClock.Data.dataStore.Settings
import com.example.MultipleAlarmClock.Data.dataStore.copy
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import com.example.MultipleAlarmClock.Ui.alarmPicker.data.AlarmSound
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel(assistedFactory = AlarmPickerViewModel.Factory::class)
class AlarmPickerViewModel @AssistedInject constructor(
	val analytics: Analytics,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	@ApplicationContext val context: Context,
	@Assisted private val alarmData: AlarmData?
) : ViewModel() {

	@AssistedFactory
	interface Factory {
		fun create(alarmData: AlarmData?): AlarmPickerViewModel
	}

	private val _uiState = MutableStateFlow(AlarmPickerUiState(
		alarmObject =  createDefaultAlarmObject(alarmData),
		initialAlarm = alarmData,
		progress = if (alarmData == null) Progress.StartTime else Progress.FullEditor
	))

	val uiState: StateFlow<AlarmPickerUiState> = _uiState.asStateFlow()

	private  val nonCancellableScope = CoroutineScope(NonCancellable)

	private val _alarmSoundName = MutableStateFlow<List<AlarmSound>>(emptyList())
	val listOfAlarms = _alarmSoundName.asStateFlow()

	private val _selectedAlarmSound = MutableStateFlow<AlarmSound?>(null)
	val selectedAlarmSound = _selectedAlarmSound.asStateFlow()

	/** here null means it's empty*/
	private val _previewingSound = MutableStateFlow<AlarmSound?>(null)
	val previewingSound = _previewingSound.asStateFlow()
	private val _previewingRandom = MutableStateFlow(false)
	val previewingRandom = _previewingRandom.asStateFlow()
	private val errorHandler = ErrorHandler(notificationHandler = NotificationHandler(context),analytics)
	private val playAlarm = PlayAlarm(context, analytics)


	init {
		viewModelScope.launch(Dispatchers.IO) {
			_alarmSoundName.value = getAlarmSounds()
		}

		viewModelScope.launch {
			_uiState.collect { state -> captureUiStateAndSendAnalytics(state) }
		}
	}


	// Update your onSetAlarmClicked to be even simpler
	fun onSetAlarmClicked() {
		analytics.captureEvent("set alarm clicked", mapOf("Ui state" to _uiState.value.toString()))

		val current = _uiState.value
		val alarmToUse = current.alarmObject.ifTimeIntervalPassedThenReturnRollOver().alarmObject
		val validationResult = alarmToUse.validate()

		_uiState.update { it.copy(alarmObject = alarmToUse, validationResult = validationResult) }
		logD("validation result after setAlarmCLicked is  $validationResult ")

		if (validationResult is ValidationResult.Failure) return
		if (!current.areAllPermissionsGranted) {
			val missing = PermissionUtils.getRequiredPermissionSteps(context)
			_uiState.update { it.copy(showPermissionDialog = true, missingSteps = missing) }
			return
		}

		viewModelScope.launch {
					setNewOrUpdateAlarm(alarmToUse, current.initialAlarm)
					_uiState.update { it.copy(alarmOperationCompletedGoBack = true) }
		}
	}

	fun previewSound(sound: AlarmSound?) {
		val soundToPlay = sound ?: listOfAlarms.value.randomOrNull() ?: return

		val sameItemTapped =
			(sound == null && _previewingRandom.value) ||
					(sound != null && _previewingSound.value?.soundUri == sound.soundUri)

		if (sameItemTapped) {
			stopPreview()
			return
		}

		stopPreview()
		playAlarm.play(soundToPlay.soundUri)
		_previewingSound.value = soundToPlay
		_previewingRandom.value = sound == null
	}

	fun stopPreview() {
		playAlarm.stop()
		_previewingSound.value = null
		_previewingRandom.value = false
	}

	fun captureUiStateAndSendAnalytics(state: AlarmPickerUiState): Unit {
		val isNotificationsEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
		captureEvent(
			"alarmPickerUi_state_change",
			mapOf(
				"are all permission granted" to state.areAllPermissionsGranted,
				"validation error message" to state.validationResult.toString(),
				"ui_state" to uiState.value.toString(),
				"did user choose random alarmSound" to (state.alarmObject.alarmSoundUri == null),
				"notification permission granted" to isNotificationsEnabled
			)
		)
	}

	fun updateProgress(newProgress: Progress) {
		_uiState.update { it.copy(progress = newProgress) }
	}

	private fun updateAlarmObject(transform: (AlarmObject) -> AlarmObject) {
		_uiState.update { state ->
			val corrected = transform(state.alarmObject).ifTimeIntervalPassedThenReturnRollOver().alarmObject
			state.copy(
				alarmObject = corrected,
				validationResult = corrected.validate()
			)
		}
	}

	fun updateStartTime(newStartTime: Calendar) = updateAlarmObject { it.copy(startTime = newStartTime) }

	fun updateEndTime(newEndTime: Calendar) = updateAlarmObject { it.copy(endTime = newEndTime) }

	fun onAlarmSoundSelected(sound: AlarmSound?){
		_selectedAlarmSound.value = sound
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(alarmSoundUri = sound?.soundUri)) }
		previewSound(sound)
	}

	fun getAlarmSounds(): List<AlarmSound> {
		val ringtoneManager = RingtoneManager(context).apply {
			setType(RingtoneManager.TYPE_ALARM)
		}
		val cursor = ringtoneManager.cursor
		val sounds = mutableListOf<AlarmSound>()
		while (cursor.moveToNext()) {
			val position = cursor.position
			val title = ringtoneManager.getRingtone(position)
				?.getTitle(context)
				?: "Unknown"
			val uri = ringtoneManager.getRingtoneUri(position)
			sounds += AlarmSound(
				title = title,
				soundUri = uri,
			)
		}
		cursor.close()
		return sounds
	}

	/** created a default alarm object; either selects a time if [alarm] is null or else just puts the alarm in the alarmObject and will not increment the date that's not it's responsiblity*/
	private fun createDefaultAlarmObject(alarm: AlarmData?): AlarmObject {
		val selectedStartTime: Calendar
		val selectedEndTime: Calendar
		val selectedDate:Long
		logD("alarm is $alarm")
		when(alarm){
			null ->{
				val now = Calendar.getInstance()

				val startTime = (now.clone() as Calendar).apply  {
					add(Calendar.MINUTE, 1)
				}

				val endOfDay = (now.clone() as Calendar).apply {
					set(Calendar.HOUR_OF_DAY, 23)
					set(Calendar.MINUTE, 59)
					set(Calendar.SECOND, 0)
					set(Calendar.MILLISECOND, 0)
				}

				val durationMin = 45
				when{
					startTime.after(endOfDay) -> {
						selectedStartTime = (now.clone() as Calendar).apply {
							add(Calendar.DAY_OF_YEAR, 1)
							set(Calendar.HOUR_OF_DAY, 0)
							set(Calendar.MINUTE, 0)
						}
						selectedEndTime = (selectedStartTime.clone() as Calendar).apply {
							add(Calendar.MINUTE, durationMin)
						}
					}
					else ->{
						selectedStartTime = startTime
						val requestedEnd = (startTime.clone() as Calendar).apply {
							add(Calendar.MINUTE, durationMin)
						}
						// Case 1:
						// Full duration fits today.
						// Case 2:
						// It doesn't fit, so cap at 11:59 PM.
						selectedEndTime = if (requestedEnd.after(endOfDay)) endOfDay else requestedEnd
					}
				}
				selectedDate = selectedStartTime.timeInMillis
				logD(" selectedStartTime:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(selectedStartTime.timeInMillis)}, selectedEndTime:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(selectedEndTime.timeInMillis)}, endOfDay:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endOfDay.timeInMillis)} ")

			}else -> {
				selectedEndTime = Calendar.getInstance().apply { timeInMillis = alarm.endTime }
				selectedStartTime = Calendar.getInstance().apply { timeInMillis = alarm.startTime }
				selectedDate = alarm.startTime
			}
		}
		return AlarmObject(
			startTime = selectedStartTime,
			endTime = selectedEndTime,
			date = selectedDate,
			message = alarm?.message ?: "",
			freqGottenAfterCallback = alarm?.frequencyInMin ?: 1,
			alarmSoundUri = alarm?.sound?.toUri()
		)
	}

	fun checkPermissions(context: Context) {
		viewModelScope.launch {
			val liveCheck = PermissionUtils.allCriticalPermissionsGranted(context)
			_uiState.update { it.copy(areAllPermissionsGranted = liveCheck) }
			dataStore.updateData { currentVal ->  currentVal.copy {  allPermissionsGranted = liveCheck }}
		}
	}

	fun dismissPermissionDialog() {
		_uiState.update { it.copy(showPermissionDialog = false) }
		checkPermissions(context)
	}

	fun onDeleteClicked() {
		val alarmData: AlarmData  = uiState.value.initialAlarm ?: return
		analytics.captureEvent("delete alarm clicked", mapOf("alarmId" to alarmData.id))
		nonCancellableScope.launch {
			alarmsController.deleteAlarmHandler(alarmData, context, alarmManager).fold(
				onSuccess = {
					analytics.captureEvent("alarm successfully deleted", mapOf("alarmId" to alarmData.id))
					_uiState.update { it.copy(alarmOperationCompletedGoBack = true) }
				},
				onError = { messageToDisplayUser, exception ->
					logD("error while deleting alarm: ${exception.message}")
					errorHandler.handleError(Result.Failure(messageToDisplayUser, exception))
				}
			)
		}
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
	}

	fun updateFrequency(newFreq: Long) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(freqGottenAfterCallback = newFreq)) }
	}

	fun updateMessage(newMessage: String) {
		_uiState.update { it.copy(alarmObject = it.alarmObject.copy(message = newMessage)) }
	}

	fun captureEvent(name:String, properties: Map<String, Any>){
		viewModelScope.launch {
			analytics.captureEvent(name, properties)
		}
	}

	fun screen(screenName:String, properties: Map<String, Any>? = null): Unit {
		viewModelScope.launch {
			analytics.screen(screenName, properties)
		}
	}


	private fun validateForCurrentStep(
		progress: Progress,
		alarm: AlarmObject
	): ValidationResult {
		return when (progress) {
			Progress.StartTime -> {
				ValidationResult.Success
			}
			Progress.EndTime -> {
				if (alarm.endTime.timeInMillis <= alarm.startTime.timeInMillis) {
					ValidationResult.Failure(
						field = AlarmErrorField.Time,
						message = "End time must be after start time."
					)
				} else {
					ValidationResult.Success
				}
			}

			Progress.FullEditor -> {
				alarm.validate()
			}
		}
	}


	/**[setNewOrUpdateAlarm] - here [AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
	private fun setNewOrUpdateAlarm(newAlarmObject: AlarmObject, oldAlarm: AlarmData? ){
		when (oldAlarm) {
			null -> {
				//  oldAlarm was not there so setting a new alarm
				viewModelScope.launch {
					launch {
						analytics.captureEvent("user setting new alarm", mapOf("alarmObject" to newAlarmObject.toString() ) )
					}
					logD("the alarm data confirmed is $newAlarmObject, and is  oldAlarm == newAlarmObject ->  ")
					val exception = alarmsController.startAlarmSeriesHandler(
						alarm = AlarmValueForAlarmSeries.AlarmObjectType(newAlarmObject),
						alarmManager = alarmManager,
						activityContext = context,
					)
					exception.fold(
						onSuccess = {
							launch {
								dataStore.updateData { data ->data.copy { firstAlarmSet = true } }
							}
							launch {
								analytics.captureEvent("new alarm successfully set", mapOf("alarmObject" to newAlarmObject.toString()))
							}
						},
						onError = {messageToDisplayUser, exception ->
							logD("there is a error in making new alarm  that is $exception ")
							errorHandler.handleError(Result.Failure(messageToDisplayUser, exception))
						}
					)
				}
			}
			else -> {
				//  oldAlarm was there so editing an existing alarm
				viewModelScope.launch {
					logD("deleting the alarm $ oldAlarm")
					alarmsController.updateAlarmStateInDb( oldAlarm).fold(onSuccess = {}, onError = { messageToDisplayUser, exception  ->
						// no such alarm exist in DB so can't update it
						logD("there is a error while editing the alarm and updating it's state in DB and  that is ${exception.message} ")
						errorHandler.handleError(Result.Failure(messageToDisplayUser, exception))
					}
					)
					val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
						alarm = AlarmValueForAlarmSeries.AlarmDataType(newAlarmObject.toAlarmData(oldAlarm.id) ),
						alarmManager, context
					)
					// now the error case is handled there
					alarmScheduledResult.fold(
						onSuccess = {
							launch {
								// update it just to be safe even this is an edit
								dataStore.updateData { data ->data.copy { firstAlarmSet = true } }
							}
							launch {
								analytics.captureEvent("alarm(old) successfully edited",
									mapOf(
										"alarmObject" to newAlarmObject.toString(),
										"oldAlarm" to oldAlarm.toString(),
									)
								)
							}
						},
						onError = { messageToDisplayUser, exception ->
							errorHandler.handleError(Result.Failure(messageToDisplayUser, exception))
							logD("there is a error/Exception in editing new alarm-->${exception.message}")
						}
					)

				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		stopPreview()
		playAlarm.destroy()
	}
}
