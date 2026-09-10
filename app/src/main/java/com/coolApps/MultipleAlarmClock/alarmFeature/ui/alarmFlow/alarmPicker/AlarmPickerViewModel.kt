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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = emptyList()
		)

	private val _previewingSound = MutableStateFlow<AlarmSound?>(null)
	val previewingSound = _previewingSound.asStateFlow()

	private val _previewingRandom = MutableStateFlow(false)
	val previewingRandom = _previewingRandom.asStateFlow()

	// Derived purely from listOfAlarms + the currently selected sound uri in uiState.
	// No manual write path needed anymore -- onAlarmSoundSelected just updates uiState.
	val selectedAlarmSound: StateFlow<AlarmSound?> = combine(
		listOfAlarms,
		_uiState.map { it.alarmData.sound }.distinctUntilChanged()
	) { sounds, soundUri ->
		sounds.find { it.soundUri.toString() == soundUri }
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = null
	)

	private val nonCancellableScope = CoroutineScope(NonCancellable)

	val isPremium: StateFlow<Boolean> = entitlementManager.isPremium

	private val playAlarm = PlayAlarm(context, analytics)

	init {
		observePremiumAccess()
	}

	fun navigationToPaywallComplete(){
		_uiState.update { it.copy(showPaywall = false) }
	}

	// Update your onSetAlarmClicked to be even simpler
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
		val sameItemTapped = (sound == null && _previewingRandom.value) ||
				(sound != null && _previewingSound.value?.soundUri == sound.soundUri)

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

	fun updateStartTime(newStartTime: Calendar) {
		updateAlarmData {
			it.copy(startTime = newStartTime.apply {
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}.timeInMillis)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	fun updateEndTime(newEndTime: Calendar) {
		updateAlarmData {
			it.copy(endTime = newEndTime.apply {
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}.timeInMillis)
		}
		captureUiStateAndSendAnalytics(_uiState.value)
	}

	fun onAlarmSoundSelected(sound: AlarmSound?) {
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
			endTime = newEndDate.timeInMillis,
			// if the user tries to set the alarm date then they're telling to turn of the repeating alarm
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
				// Repeat turned off -> keep the exact time-of-day/duration the user
				// dialed in, just move the DATE back to today.
				val today = LocalDate.now()
				val newStart = Calendar.getInstance().apply {
					timeInMillis = current.startTime
					set(Calendar.YEAR, today.year)
					set(Calendar.MONTH, today.monthValue - 1)
					set(Calendar.DAY_OF_MONTH, today.dayOfMonth)
					set(Calendar.SECOND, 0)
					set(Calendar.MILLISECOND, 0)
				}
				current.copy(
					startTime = newStart.timeInMillis,
					endTime = newStart.timeInMillis + durationMillis,
					repeatDays = null
				)
			} else {
				// Repeat is on (or day set changed) -> keep time-of-day/duration,
				// re-anchor the DATE from today forward so it's order-independent.
				val nextDate = newRepeatDays.nextRepeatDate(LocalDate.now()) ?: LocalDate.now()
				val newStart = Calendar.getInstance().apply {
					timeInMillis = current.startTime
					set(Calendar.YEAR, nextDate.year)
					set(Calendar.MONTH, nextDate.monthValue - 1)
					set(Calendar.DAY_OF_MONTH, nextDate.dayOfMonth)
					set(Calendar.SECOND, 0)
					set(Calendar.MILLISECOND, 0)
				}
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

		//TODO: remove the pending repeat date form here as user can just click it again and ok for the ux, and wouldn't require state sync if they did convert


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

}
