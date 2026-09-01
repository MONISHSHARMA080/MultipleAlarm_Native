package com.coolApps.MultipleAlarmClock.alarmFeature.ui.alarmContainer

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.AlarmLogic.AlarmsController
import com.coolApps.MultipleAlarmClock.Data.dataStore.Settings
import com.coolApps.MultipleAlarmClock.Data.dataStore.copy
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.alarmFeature.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.AlarmRepository
import com.coolApps.MultipleAlarmClock.alarmFeature.domain.InAppReviewEligibilityChecker
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import com.coolApps.MultipleAlarmClock.logD
import com.coolApps.MultipleAlarmClock.utils.Result.Result
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.model.ReviewErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@HiltViewModel
class AlarmContainerViewModel @Inject constructor(
	val analytics: Analytics,
	alarmRepository: AlarmRepository,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	private val errorHandler: ErrorHandler,
	@ApplicationContext  val context: Context
) : ViewModel(){

	private val eligibilityChecker: InAppReviewEligibilityChecker = InAppReviewEligibilityChecker()

	val alarmControllerUi: StateFlow<AlarmContainerUiState> = combine(dataStore.data, alarmRepository.getAlarmsStream(), analytics.featureFlagsData){ settingsData , alarmList, featureFlag->
		when{
			featureFlag == null -> {
				AlarmContainerUiState(
					alarmList = alarmList,
					reviewInelligiblityReason = null,
					showReviewUi = false
				)
			}
			alarmList.isEmpty() -> {
				AlarmContainerUiState(
					alarmList = alarmList,
					reviewInelligiblityReason = null,
					showReviewUi = false
				)
			}
			else ->{
				val highestIdAlarm = alarmList.maxBy { it.id }
				val res = eligibilityChecker.evaluate(
					installEpochTimeMs = settingsData.installEpochTimeMs,
					alarmCount = highestIdAlarm.id,
					lastReviewAttemptEpochTimeMs = settingsData.lastReviewAttemptedAt,
					config = featureFlag
				)
				when(res){
					is InAppReviewEligibilityChecker.Result.Eligible ->{
						AlarmContainerUiState(
							alarmList = alarmList,
							reviewInelligiblityReason = null,
							showReviewUi = true
						)
					}
					is InAppReviewEligibilityChecker.Result.NotEligible -> {
						AlarmContainerUiState(
							alarmList = alarmList,
							reviewInelligiblityReason = res.reason,
							showReviewUi = false
						)
					}
				}
			}
		}
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = AlarmContainerUiState()
	)



	val showFeedbackUIState: StateFlow<Boolean> = dataStore.data
		.map { settings ->
			settings.shouldWeShowFeedbackCard
		}
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

	fun setInAppReviewConsumed(task: Task<ReviewInfo>) {
		viewModelScope.launch {
			// if the isSuccessful == false or true that doesn't mean that the user saw the popup and failure could be
			// due to any reason, so better to back of and not get rate limited as the api can change any time
			val isSuccessful = task.isSuccessful
			val reviewException = task.exception as? ReviewException
			val message = reviewException?.message ?: ""
			val statusCode = reviewException?.statusCode ?: ""
			val stackTrace = reviewException?.stackTrace ?: ""
			val cause= reviewException?.cause ?: ""
			val status = reviewException?.status ?: ""
			val errorCode = reviewException?.errorCode ?: ""
			val isTransientFailure = !isSuccessful && errorCode == ReviewErrorCode.INTERNAL_ERROR

			logD("review flow completed, success=$isSuccessful, error_code=$errorCode, left_state_unchanged/isTransientFailure =$isTransientFailure")

			analytics.captureEvent(
				"in_app_review_shown",
				mapOf(
					"success" to isSuccessful,
					"error_code" to errorCode,
					"error_message" to message,
					"left_state_unchanged" to isTransientFailure,
					"status_code" to statusCode,
					"stack_trace" to stackTrace,
					"cause" to cause,
					"status" to status,
					"error_code" to errorCode
				)
			)
			// isSuccessful == true doesn't mean the user saw the dialog (quota can
			// silently block it with no error), and isSuccessful == false doesn't
			// mean we should give up permanently - only INTERNAL_ERROR is transient.
			// INVALID_REQUEST/PLAY_STORE_NOT_FOUND will fail identically next time too,
			// so there's no point holding eligibility open for those.
			if (! isTransientFailure){
				dataStore.updateData {
					it.copy {lastReviewAttemptedAt = System.currentTimeMillis() }
				}
			}
		}
	}

	fun dismissFeedback() {
		viewModelScope.launch {
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

	fun captureEvent(event:String, properties:Map<String, Any>){
		analytics.captureEvent(event, properties)
	}

	fun resetAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			logD("about to reset the alarm-+")
			val exception = alarmsController.resetAlarmsHandler(
				alarmData = alarmData,
				alarmManager = alarmManager,
				activityContext = context,
			)
			exception.fold(
				onSuccess = {
					viewModelScope.launch {
						analytics.captureEvent("alarm_reset", mapOf(
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