package com.example.MultipleAlarmClock.Ui.alarmPicker

import android.app.AlarmManager
import android.content.Context
import android.media.RingtoneManager
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController.AlarmValueForAlarmSeries
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.dataBase.AlarmDao
import com.coolApps.MultipleAlarmClock.dataBase.AlarmData
import com.coolApps.MultipleAlarmClock.dataBase.AlarmErrorField
import com.coolApps.MultipleAlarmClock.dataBase.AlarmObject
import com.coolApps.MultipleAlarmClock.dataBase.ValidationResult
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.notification.NotificationHandler
import com.coolApps.MultipleAlarmClock.services.PlayAlarm
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.example.MultipleAlarmClock.Data.dataStore.Settings
import com.example.MultipleAlarmClock.Data.dataStore.copy
import com.example.MultipleAlarmClock.Ui.Permissions.PermissionUtils
import com.example.MultipleAlarmClock.Ui.alarmPicker.data.AlarmSound
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@HiltViewModel
class AlarmPickerViewModel @Inject constructor(
	val analytics: Analytics,
	private val alarmManager: AlarmManager,
	private val alarmDao: AlarmDao,
	private val dataStore: DataStore<Settings>,
	@ApplicationContext  val context: Context
) : ViewModel() {

	private val _uiState = MutableStateFlow(AlarmPickerUiState())
	val uiState: StateFlow<AlarmPickerUiState> = _uiState.map { state->
		state.copy(validationResult = state.alarmObject.validate(state.initialAlarm))
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmPickerUiState())


	private val _alarmSoundName = MutableStateFlow<List<AlarmSound>>(emptyList())
	val listOfAlarms = _alarmSoundName.asStateFlow()

	private val _selectedAlarmSound = MutableStateFlow<AlarmSound?>(null)
	val selectedAlarmSound = _selectedAlarmSound.asStateFlow()

	/** here null means it's empty*/
	private val _previewingSound = MutableStateFlow<AlarmSound?>(null)
	val previewingSound = _previewingSound.asStateFlow()

	private val _previewingRandom = MutableStateFlow(false)
	val previewingRandom = _previewingRandom.asStateFlow()


	private val _events = MutableSharedFlow<AlarmPickerEvent>(extraBufferCapacity = 1)
	val events: SharedFlow<AlarmPickerEvent> = _events.asSharedFlow()

	private val errorHandler = ErrorHandler(notificationHandler = NotificationHandler(context),analytics)
	private val alarmsController = AlarmsController()

	private val playAlarm = PlayAlarm(context, analytics)


	init {
		viewModelScope.launch(Dispatchers.IO) {
			_alarmSoundName.value = getAlarmSounds()
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


	fun setInitialAlarmObject(alarmData: AlarmData?) {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					alarmObject = alarmData?.toAlarmObject() ?: createDefaultAlarmObject(alarmData),
					initialAlarm = alarmData
				)
			}
			launch{
				_selectedAlarmSound.value = getAlarmSoundFromAlarmData(alarmData)
			}
		}
	}

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

	private fun getAlarmSoundFromAlarmData(alarm: AlarmData?): AlarmSound? {
		return  if (alarm != null && alarm.sound != null){
			try {
				AlarmSound(
					title = RingtoneManager.getRingtone(context, alarm.sound.toUri())?.getTitle(context)  ?: return null,
					soundUri = alarm.sound.toUri()
				)
			} catch (_: Exception) {
				null
			}
		}else null
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
			freqGottenAfterCallback = alarm?.frequencyInMin ?: 1,
			alarmSoundUri = alarm?.sound?.toUri() ,
		)
	}

	fun checkPermissions(context: Context) {
		viewModelScope.launch {
			val liveCheck = PermissionUtils.allCriticalPermissionsGranted(context)
			_uiState.update { it.copy(areAllPermissionsGranted = liveCheck) }
			dataStore.updateData { currentVal ->  currentVal.copy {  allPermissionsGranted = true }}
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
		val result = _uiState.value.alarmObject.validate(_uiState.value.initialAlarm)
		_uiState.update { it.copy(validationResult = result) }
	}

	fun getFrequencyPreviewText(): String {
		val state = _uiState.value
		val currentError = state.validationResult as? ValidationResult.Failure

		return if (state.validationResult is ValidationResult.Success) {
			"your alarm will ring on ${getPreviewAlarms(state.alarmObject, 4)}"
		} else {
			if (currentError?.field == AlarmErrorField.FREQUENCY) {
				currentError.message
			} else ""
		}
	}

	fun captureEvent(name:String, properties: Map<String, Any>){
		viewModelScope.launch {
			analytics.captureEvent(name, properties)
		}
	}

	private fun getPreviewAlarms(alarm: AlarmObject, numberOfAlarmPreviewToReturn:Int = 3): String{
		val alarmObj = alarm.deepCopy()
		val stringBuilder= StringBuilder()
		val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
		var index = 0

		while (!alarmObj.startTime.after(alarmObj.endTime) && index < numberOfAlarmPreviewToReturn) {
			stringBuilder.append(timeFormat.format(alarmObj.startTime.time))
			alarmObj.startTime.timeInMillis += alarmObj.getFreqInMillisecond()
			if (alarmObj.freqGottenAfterCallback <= 0) break
			index ++
			if (index < numberOfAlarmPreviewToReturn && !alarmObj.startTime.after(alarmObj.endTime)) {
				stringBuilder.append(", ")
			}
		}

		return if(alarmObj.startTime.after(alarmObj.endTime)){
			stringBuilder.toString().trim()
		}else{
			stringBuilder.append(".....${timeFormat.format(alarmObj.endTime.time)}").toString().trim()
		}
	}


	/**[setAlarm] - here [AlarmData] is the alarm passed in the function if it is same to the alarmObject one then do not set the alarm, as user might have miss clicked it*/
	fun setAlarm(newAlarmObject: AlarmObject, oldAlarm: AlarmData? ){
		when (oldAlarm) {
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
								dataStore.updateData { data ->data.copy { firstAlarmSet = true } }
							}
							launch {
								analytics.captureEvent("new alarm successfully set", mapOf("alarmObject" to newAlarmObject.toString()))
							}
						},
						onError = {messageToDisplayUser, exception ->
							logD("there is a error in making new alarm  that is $exception ")
							errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while making new alarm, Please try again" )
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
						errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
					}
					)
					val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
						alarm = AlarmValueForAlarmSeries.AlarmDataType(newAlarmObject.toAlarmData(oldAlarm.id) ),
						alarmManager, context, alarmDao
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
							errorHandler.handleError(Result.Failure(messageToDisplayUser, exception), "Sorry an error occurred while editing alarm, Please try again" )
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
