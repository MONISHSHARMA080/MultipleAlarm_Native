package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.RepeatDays
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.Permissions.PermissionUtils
import com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker.data.AlarmSound
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.services.PlayAlarm
import com.coolApps.MultipleAlarmClock.utils.Result.Result
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
import java.time.DayOfWeek
import java.util.Calendar

@HiltViewModel(assistedFactory = AlarmPickerViewModel.Factory::class)
class AlarmPickerViewModel @AssistedInject constructor(
	val analytics: Analytics,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	private val errorHandler: ErrorHandler,
	@ApplicationContext val context: Context,
	@Assisted private val alarmData: AlarmData?
) : ViewModel() {

	@AssistedFactory
	interface Factory {
		fun create(alarmData: AlarmData?): AlarmPickerViewModel
	}

	private val _uiState = MutableStateFlow(AlarmPickerUiState(
		alarmData = createDefaultAlarm(alarmData),
		initialAlarm = alarmData,
		progress = if (alarmData == null) Progress.StartTime else Progress.FullEditor
	))

	val uiState: StateFlow<AlarmPickerUiState> = _uiState.asStateFlow()

	private val nonCancellableScope = CoroutineScope(NonCancellable)

	private val _alarmSoundName = MutableStateFlow<List<AlarmSound>>(emptyList())
	val listOfAlarms = _alarmSoundName.asStateFlow()

	private val _selectedAlarmSound = MutableStateFlow<AlarmSound?>(null)
	val selectedAlarmSound = _selectedAlarmSound.asStateFlow()

	/** here null means it's empty*/
	private val _previewingSound = MutableStateFlow<AlarmSound?>(null)
	val previewingSound = _previewingSound.asStateFlow()
	private val _previewingRandom = MutableStateFlow(false)
	val previewingRandom = _previewingRandom.asStateFlow()
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
		val alarmToUse = current.alarmData.rollOverIfTimeIntervalPassed()
		val validationResult = alarmToUse.validate()

		_uiState.update { it.copy(alarmData = alarmToUse, validationResult = validationResult) }
		logD("validation result after setAlarmCLicked is $validationResult ")

		if (validationResult !is AlarmDataValidationResult.Success) return
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

		val sameItemTapped = (sound == null && _previewingRandom.value) || (sound != null && _previewingSound.value?.soundUri == sound.soundUri)

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
				"did user choose random alarmSound" to (state.alarmData.sound == null),
				"notification permission granted" to isNotificationsEnabled
			)
		)
	}

	fun updateProgress(newProgress: Progress) {
		_uiState.update { it.copy(progress = newProgress) }
	}

	private fun updateAlarmData(transform: (AlarmData) -> AlarmData) {
		_uiState.update { state ->
			val transformed = transform(state.alarmData)
			val corrected = transformed.rollOverIfTimeIntervalPassed()

			val newStartTime = Calendar.getInstance().apply {
				timeInMillis = corrected.startTime
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}.timeInMillis
			val newEndTime = Calendar.getInstance().apply {
				timeInMillis = corrected.endTime
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}.timeInMillis

			val finalAlarm = corrected.copy(startTime = newStartTime, endTime = newEndTime)
			state.copy(
				alarmData = finalAlarm,
				validationResult = finalAlarm.validate()
			)
		}
	}

	fun updateStartTime(newStartTime: Calendar) = updateAlarmData {
		it.copy(startTime = newStartTime.apply {
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis)
	}

	fun updateStartTime(newStartTime: Long) = updateAlarmData {
		it.copy(startTime = newStartTime)
	}

	fun updateEndTime(newEndTime: Calendar) = updateAlarmData {
		it.copy(endTime = newEndTime.apply {
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis)
	}

	fun updateEndTime(newEndTime: Long) = updateAlarmData {
		it.copy(endTime = newEndTime)
	}

	fun onAlarmSoundSelected(sound: AlarmSound?){
		_selectedAlarmSound.value = sound
		_uiState.update { it.copy(alarmData = it.alarmData.copy(sound = sound?.soundUri?.toString())) }
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

	/** creates a default alarm data; either selects a time if [alarm] is null or else returns [alarm]*/
	private fun createDefaultAlarm(alarm: AlarmData?): AlarmData {
		val selectedStartTime: Long
		val selectedEndTime: Long
		logD("alarm is $alarm")
		when(alarm){
			null ->{
				val now = Calendar.getInstance()

				val startTime = (now.clone() as Calendar).apply  {
					add(Calendar.MINUTE, 1)
					set(Calendar.SECOND, 0)
					set(Calendar.MILLISECOND, 0)
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
						val start = (now.clone() as Calendar).apply {
							add(Calendar.DAY_OF_YEAR, 1)
							set(Calendar.HOUR_OF_DAY, 0)
							set(Calendar.MINUTE, 0)
							set(Calendar.SECOND, 0)
							set(Calendar.MILLISECOND, 0)
						}
						val end = (start.clone() as Calendar).apply {
							add(Calendar.MINUTE, durationMin)
							set(Calendar.SECOND, 0)
							set(Calendar.MILLISECOND, 0)
						}
						selectedStartTime = start.timeInMillis
						selectedEndTime = end.timeInMillis
					}
					else ->{
						selectedStartTime = startTime.timeInMillis
						val requestedEnd = (startTime.clone() as Calendar).apply {
							add(Calendar.MINUTE, durationMin)
							set(Calendar.SECOND, 0)
							set(Calendar.MILLISECOND, 0)
						}
						// Case 1:
						// Full duration fits today.
						// Case 2:
						// It doesn't fit, so cap at 11:59 PM.
						selectedEndTime = if (requestedEnd.after(endOfDay)) endOfDay.timeInMillis else requestedEnd.timeInMillis
					}
				}
				logD(" selectedStartTime:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(selectedStartTime)}, selectedEndTime:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(selectedEndTime)}, endOfDay:${alarmsController.getTimeInHumanReadableFormatProtectFrom0Included(endOfDay.timeInMillis)} ")

				return AlarmData(
					startTime = selectedStartTime,
					endTime = selectedEndTime,
					message = "",
					frequencyInMin = 5,
					sound = null,
					isReadyToUse = true
				)
			}
			else -> {
				return alarm
			}
		}
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
		nonCancellableScope.launch {
			alarmsController.deleteAlarmHandler(alarmData, context, alarmManager).fold(
				onSuccess = {
					analytics.captureEvent("alarm successfully deleted", mapOf("alarmId" to alarmData.id))
					_uiState.update { it.copy(alarmOperationCompletedGoBack = true) }
				},
				onError = { error ->
					logD("error while deleting alarm: ${error.internalErrorMessage}")
					errorHandler.handleError(Result.Failure(error))
				}
			)
		}
	}

	fun updateDate(calVersion: Calendar) {
		val currentAlarm = _uiState.value.alarmData

		val newStartDate = Calendar.getInstance().apply {
			timeInMillis = currentAlarm.startTime
			set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
			set(Calendar.MONTH, calVersion.get(Calendar.MONTH))
			set(Calendar.DAY_OF_MONTH, calVersion.get(Calendar.DAY_OF_MONTH))
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}

		val newEndDate = Calendar.getInstance().apply {
			timeInMillis = currentAlarm.endTime
			set(Calendar.YEAR, calVersion.get(Calendar.YEAR))
			set(Calendar.MONTH, calVersion.get(Calendar.MONTH))
			set(Calendar.DAY_OF_MONTH, calVersion.get(Calendar.DAY_OF_MONTH))
			set(Calendar.SECOND, 0)
			set(Calendar.MILLISECOND, 0)
		}

		val updated = currentAlarm.copy(
			startTime = newStartDate.timeInMillis,
			endTime = newEndDate.timeInMillis
		).rollOverIfTimeIntervalPassed()

		_uiState.update {
			it.copy(
				alarmData = updated,
				validationResult = updated.validate()
			)
		}
	}

	fun updateFrequency(newFreq: Long) {
		_uiState.update { state ->
			val updated = state.alarmData.copy(frequencyInMin = newFreq).rollOverIfTimeIntervalPassed()
			state.copy(
				alarmData = updated,
				validationResult = updated.validate()
			)
		}
	}

	fun updateMessage(newMessage: String) {
		_uiState.update { it.copy(alarmData = it.alarmData.copy(message = newMessage)) }
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

	/**[setNewOrUpdateAlarm] - sets a new alarm or updates an existing one*/
	private fun setNewOrUpdateAlarm(newAlarmData: AlarmData, oldAlarm: AlarmData? ){
		when (oldAlarm) {
			null -> {
				//  oldAlarm was not there so setting a new alarm
				viewModelScope.launch {
					logD("the alarm data confirmed is $newAlarmData, and is  oldAlarm == newAlarmData ->  ")
					val exception = alarmsController.startAlarmSeriesHandler(
						alarm = newAlarmData.copy(isReadyToUse = true, id = 0),
						alarmManager = alarmManager,
						activityContext = context,
					)
					exception.fold(
						onSuccess = {
							launch {
								dataStore.updateData { data ->data.copy { firstAlarmSet = true } }
							}
							launch {
								analytics.captureEvent("new alarm successfully set", mapOf("alarmData" to newAlarmData.toString()))
							}
						},
						onError = { error ->
							logD("there is a error in making new alarm  that is $error")
							errorHandler.handleError(Result.Failure(error))
						}
					)
				}
			}
			else -> {
				//  oldAlarm was there so editing an existing alarm
				viewModelScope.launch {
					logD("deleting the alarm $oldAlarm")
					val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
						alarm = newAlarmData.copy(id = oldAlarm.id),
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
										"alarmData" to newAlarmData.toString(),
										"oldAlarm" to oldAlarm.toString(),
									)
								)
							}
						},
						onError = { error ->
							errorHandler.handleError(Result.Failure(error))
							logD("there is a error in editing new alarm-->${error.internalErrorMessage}")
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

	fun toggleRepeatDay(day: DayOfWeek) {
		_uiState.update { state ->
			val currentSet = state.alarmData.repeatDays?.toSet() ?: emptySet()
			val newSet = if (day in currentSet) currentSet - day else currentSet + day
			val updated = state.alarmData.copy(repeatDays = RepeatDays.of(newSet))
			state.copy(alarmData = updated, validationResult = updated.validate())
		}

	}
}
