package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmFlow.alarmPicker

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.alarmFeature.data.billing.EntitlementManager
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmDataValidationResult
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.RepeatDays
import com.coolApps.MultipleAlarmClock.alarmFeature.repository.AlarmSoundRepository
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

@HiltViewModel(assistedFactory = AlarmPickerViewModel.Factory::class)
class AlarmPickerViewModel @AssistedInject constructor(
		val analytics: Analytics,
		private val alarmManager: AlarmManager,
		private val dataStore: DataStore<Settings>,
		private val alarmsController: AlarmsController,
		private val entitlementManager: EntitlementManager,
		private val errorHandler: ErrorHandler,
		private val alarmSoundRepository: AlarmSoundRepository,
		@ApplicationContext val context: Context,
		@Assisted private val alarmData: AlarmData?
) : ViewModel() {

	@AssistedFactory
	interface Factory {
		fun create(alarmData: AlarmData?): AlarmPickerViewModel
	}

	private val _uiState = MutableStateFlow(
		AlarmPickerUiState(
			alarmData = createDefaultAlarm(alarmData),
			initialAlarm = alarmData,
			progress = if (alarmData == null) Progress.StartTime else Progress.FullEditor
		)
	)
	val uiState: StateFlow<AlarmPickerUiState> = _uiState.asStateFlow()

	val listOfAlarms: StateFlow<List<AlarmSound>> = alarmSoundRepository
		.getAlarmSoundsStream()
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = emptyList()
		)

	private val _previewingSound = MutableStateFlow<AlarmSound?>(null)
	val previewingSound = _previewingSound.asStateFlow()

	private val _previewingRandom = MutableStateFlow(false)
	val previewingRandom = _previewingRandom.asStateFlow()

	private val _selectedAlarmSound = MutableStateFlow<AlarmSound?>(null)
	val selectedAlarmSound = _selectedAlarmSound.asStateFlow()

	private val nonCancellableScope = CoroutineScope(NonCancellable)

	val isPremium: StateFlow<Boolean> = entitlementManager.isPremium

	private val playAlarm = PlayAlarm(context, analytics)

	init {
		// if we have an initial alarm then get it's start alarm sound name
		_uiState.value.alarmData.sound?.let { initialSoundUri ->
			viewModelScope.launch(Dispatchers.IO) {
				_selectedAlarmSound.value = alarmSoundRepository.resolveSound(initialSoundUri)
			}
		}
		observePremiumAccess()
	}

	fun navigationToPaywallComplete(){
		_uiState.update { it.copy(showPaywall = false) }
	}

	fun onSetAlarmClicked() {
		analytics.captureEvent("set alarm clicked", mapOf("Ui state" to _uiState.value.toString()))

		val current = _uiState.value
		val alarmToUse = current.alarmData.rollOverIfTimeIntervalPassed()
		val validationResult = alarmToUse.validate()

		_uiState.update { it.copy(alarmData = alarmToUse, validationResult = validationResult) }
		logD("validation result after setAlarmCLicked is $validationResult ")

		if (validationResult !is AlarmDataValidationResult.Success) {
			captureUiStateAndSendAnalytics(_uiState.value)
			return
		}

		if (!current.areAllPermissionsGranted) {
			val missing = PermissionUtils.getRequiredPermissionSteps(context)
			_uiState.update { it.copy(showPermissionDialog = true, missingSteps = missing) }
			captureUiStateAndSendAnalytics(_uiState.value)
			return
		}

		viewModelScope.launch {
			setNewOrUpdateAlarm(alarmToUse, current.initialAlarm)
			_uiState.update { it.copy(alarmOperationCompletedGoBack = true) }
			captureUiStateAndSendAnalytics(_uiState.value)
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
		_previewingRandom.value = (sound == null)
	}

	fun stopPreview() {
		playAlarm.stop()
		_previewingSound.value = null
		_previewingRandom.value = false
	}

	fun captureUiStateAndSendAnalytics(state: AlarmPickerUiState) {
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
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	private fun updateAlarmData(transform: (AlarmData) -> AlarmData) {
		_uiState.update { state ->
			val transformed = transform(state.alarmData)
			val corrected = transformed.rollOverIfTimeIntervalPassed()

			val newStartTime = Calendar.getInstance().apply {
				timeInMillis = corrected.startTime
			}.truncatedToMinute().timeInMillis
			val newEndTime = Calendar.getInstance().apply {
				timeInMillis = corrected.endTime
			}.truncatedToMinute().timeInMillis

			val finalAlarm = corrected.copy(startTime = newStartTime, endTime = newEndTime)
			state.copy(
				alarmData = finalAlarm,
				validationResult = finalAlarm.validate()
			)
		}
	}

	fun updateStartTime(newStartTime: Calendar) {
		updateAlarmData {
			it.copy(startTime = newStartTime.apply {
			}.truncatedToMinute().timeInMillis)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	fun updateEndTime(newEndTime: Calendar) {
		updateAlarmData {
			it.copy(endTime = newEndTime.apply {
			}.truncatedToMinute().timeInMillis)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	fun onAlarmSoundSelected(sound: AlarmSound?) {
		_selectedAlarmSound.value = sound
		_uiState.update {
			it.copy(
				alarmData = it.alarmData.copy(sound = sound?.soundUri?.toString())
			)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
		previewSound(sound)
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
				}.truncatedToMinute()

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
			captureUiStateAndSendAnalytics(_uiState.value)
			dataStore.updateData { currentVal ->  currentVal.copy {  allPermissionsGranted = liveCheck }}
		}
	}

	fun dismissPermissionDialog() {
		_uiState.update { it.copy(showPermissionDialog = false) }
		captureUiStateAndSendAnalytics(_uiState.value)
		checkPermissions(context)
	}

	fun onDeleteClicked() {
		val alarmData: AlarmData  = uiState.value.initialAlarm ?: return
		nonCancellableScope.launch {
			alarmsController.deleteAlarmHandler(alarmData, context, alarmManager).fold(
				onSuccess = {
					analytics.captureEvent("alarm successfully deleted", mapOf("alarmId" to alarmData.id))
					_uiState.update { it.copy(alarmOperationCompletedGoBack = true) }
					captureUiStateAndSendAnalytics(_uiState.value)
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
		}.withDate(calVersion)


		val newEndDate = Calendar.getInstance().apply {
			timeInMillis = currentAlarm.endTime
		}.withDate(calVersion)

		val updated = currentAlarm.copy(
			startTime = newStartDate.timeInMillis,
			endTime = newEndDate.timeInMillis,
			repeatDays = null
		).rollOverIfTimeIntervalPassed()

		_uiState.update {
			it.copy(
				alarmData = updated,
				validationResult = updated.validate()
			)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
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

	private fun setNewOrUpdateAlarm(newAlarmData: AlarmData, oldAlarm: AlarmData? ){
		viewModelScope.launch {
			logD("deleting the alarm $oldAlarm")
			val alarmScheduledResult = alarmsController.startAlarmSeriesHandler(
				alarm = newAlarmData.copy(id = oldAlarm?.id ?: 0),
				alarmManager, context
			)
			alarmScheduledResult.fold(
				onSuccess = {
					launch {
						dataStore.updateData { data ->data.copy { firstAlarmSet = true } }
					}
					launch {
						val eventText = if (oldAlarm != null) "alarm_edited"  else "new_alarm_set"
						analytics.captureEvent(eventText,
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

	fun onRepeatDayClicked(day: DayOfWeek) {
		viewModelScope.launch {
			entitlementManager.refresh()

			if (isPremium.value) {
				toggleRepeatDay(day)
			} else {
				_uiState.update {
					it.copy(
						showPaywall = true,
						pendingRepeatDay = day
					)
				}
			}
		}
	}

	fun toggleRepeatDay(day: DayOfWeek) {
		_uiState.update { state ->
			val current = state.alarmData
			val currentSet = current.repeatDays?.toSet() ?: emptySet()
			val newSet = if (day in currentSet) currentSet - day else currentSet + day
			val newRepeatDays = RepeatDays.of(newSet)

			val durationMillis = current.endTime - current.startTime

			val rebasedAlarm = if (newRepeatDays == null) {
				val today = LocalDate.now()
				val newStart = Calendar.getInstance().apply {
					timeInMillis = current.startTime
					set(Calendar.YEAR, today.year)
					set(Calendar.MONTH, today.monthValue - 1)
					set(Calendar.DAY_OF_MONTH, today.dayOfMonth)
				}.truncatedToMinute()
				current.copy(
					startTime = newStart.timeInMillis,
					endTime = newStart.timeInMillis + durationMillis,
					repeatDays = null
				)
			} else {
				val nextDate = newRepeatDays.nextRepeatDate(LocalDate.now()) ?: LocalDate.now()
				val newStart = Calendar.getInstance().apply {
					timeInMillis = current.startTime
					set(Calendar.YEAR, nextDate.year)
					set(Calendar.MONTH, nextDate.monthValue - 1)
					set(Calendar.DAY_OF_MONTH, nextDate.dayOfMonth)
				}.truncatedToMinute()
				current.copy(
					startTime = newStart.timeInMillis,
					endTime = newStart.timeInMillis + durationMillis,
					repeatDays = newRepeatDays
				)
			}

			val corrected = rebasedAlarm.rollOverIfTimeIntervalPassed()
			state.copy(alarmData = corrected, validationResult = corrected.validate())
		}
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	private fun observePremiumAccess() {
		viewModelScope.launch {
			entitlementManager.isPremium.collect { isPremium ->
				if (isPremium) {
					val pendingDay = _uiState.value.pendingRepeatDay

					if (pendingDay != null) {
						toggleRepeatDay(pendingDay)

						_uiState.update {
							it.copy(
								showPaywall = false,
								pendingRepeatDay = null
							)
						}
					}
				}
			}
		}
	}

	override fun onCleared() {
		super.onCleared()
		stopPreview()
		playAlarm.destroy()
	}
	private fun Calendar.truncatedToMinute(): Calendar = apply {
		set(Calendar.SECOND, 0)
		set(Calendar.MILLISECOND, 0)
	}

	private fun Calendar.withDate(other: Calendar): Calendar = apply {
		set(Calendar.YEAR, other.get(Calendar.YEAR))
		set(Calendar.MONTH, other.get(Calendar.MONTH))
		set(Calendar.DAY_OF_MONTH, other.get(Calendar.DAY_OF_MONTH))
		truncatedToMinute()
	}
}